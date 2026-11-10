/**
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
import { dsComponentStubs } from "./dsStubs";
import SearchBox from "../SearchBox.vue";
import { flushPromises, mount } from "@vue/test-utils";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { createI18n } from "vue-i18n";
import type { SearchBoxItem, SearchBoxSource } from "../SearchBox.vue";

type Value = { name: string };

function item(name: string): SearchBoxItem<Value, string> {
  return {
    value: { name },
    renderingData: name,
    key: name,
    equivalentQuery: name,
  };
}

function mountSearchBox(props: {
  suggestions?: SearchBoxItem<Value, string>[];
  parseQuery?: (query: string) => Value | null;
  getSuggestions?: SearchBoxSource<Value, string>;
  placeholder?: string;
  initialValue?: string;
  autofocus?: boolean;
  required?: boolean;
}) {
  return mount(SearchBox<Value, string>, {
    props: {
      label: "Reference",
      initialValue: props.initialValue ?? null,
      getSuggestions:
        props.getSuggestions ?? (async () => props.suggestions ?? []),
      parseQuery: props.parseQuery,
      placeholder: props.placeholder,
      autofocus: props.autofocus,
      required: props.required,
    },
    slots: {
      renderSuggestion: `<template #renderSuggestion="{ renderingData }">{{ renderingData }}</template>`,
    },
    global: {
      plugins: [createI18n({ legacy: false, locale: "en", messages: {} })],
      components: dsComponentStubs,
    },
  });
}

describe("SearchBox", () => {
  // jsdom doesn't implement scrolling.
  beforeEach(() => {
    Element.prototype.scrollIntoView ??= vi.fn();
  });

  it("hints that typing searches, since the suggestions are only displayed once something is typed", () => {
    const wrapper = mountSearchBox({});

    expect(wrapper.get("input").attributes("placeholder")).toBe(
      "Type to search...",
    );
  });

  it("displays the given placeholder instead of the default one", () => {
    const wrapper = mountSearchBox({ placeholder: "Page name" });

    expect(wrapper.get("input").attributes("placeholder")).toBe("Page name");
  });

  it("marks the search field with autofocus only when asked to", () => {
    expect(
      mountSearchBox({ autofocus: true }).get("input").attributes("autofocus"),
    ).toBeDefined();
    expect(
      mountSearchBox({}).get("input").attributes("autofocus"),
    ).toBeUndefined();
  });

  it("marks the search field as required only when asked to", () => {
    expect(
      mountSearchBox({ required: true }).get("input").attributes("required"),
    ).toBeDefined();
    expect(
      mountSearchBox({}).get("input").attributes("required"),
    ).toBeUndefined();
  });

  describe("Enter key", () => {
    it("selects the value parsed from the typed text when Enter is pressed without picking a suggestion", async () => {
      const wrapper = mountSearchBox({
        parseQuery: (query) =>
          query.startsWith("ref:") ? { name: query } : null,
      });
      const input = wrapper.get("input");

      await input.setValue("ref:Some.Page");
      await input.trigger("keydown", { key: "Enter" });

      expect(wrapper.emitted("select")).toEqual([[{ name: "ref:Some.Page" }]]);
    });

    it("selects nothing when the typed text doesn't designate a value", async () => {
      const wrapper = mountSearchBox({ parseQuery: () => null });
      const input = wrapper.get("input");

      await input.setValue("not a reference");
      await input.trigger("keydown", { key: "Enter" });

      expect(wrapper.emitted("select")).toBeUndefined();
    });

    it("selects nothing when Enter is pressed without picking a suggestion and without parseQuery", async () => {
      const wrapper = mountSearchBox({});
      const input = wrapper.get("input");

      await input.setValue("ref:Some.Page");
      await input.trigger("keydown", { key: "Enter" });

      expect(wrapper.emitted("select")).toBeUndefined();
    });

    it("selects the focused suggestion rather than the typed text", async () => {
      const parseQuery = vi.fn((query: string) => ({ name: query }));
      const wrapper = mountSearchBox({
        suggestions: [item("Suggested.Page")],
        parseQuery,
      });
      const input = wrapper.get("input");

      await input.setValue("Sugg");
      await vi.waitFor(() =>
        expect(wrapper.findAll("li.suggestion")).toHaveLength(1),
      );
      await input.trigger("keydown", { key: "ArrowDown" });
      await input.trigger("keydown", { key: "Enter" });

      expect(wrapper.emitted("select")).toEqual([[{ name: "Suggested.Page" }]]);
      expect(parseQuery).not.toHaveBeenCalled();
    });
  });

  describe("leaving the field", () => {
    it("selects the value parsed from the typed text, e.g. a page that doesn't exist yet", async () => {
      const wrapper = mountSearchBox({
        suggestions: [item("Existing.Page")],
        parseQuery: (query) => ({ name: query }),
      });
      const input = wrapper.get("input");

      await input.setValue("New.Page");
      await vi.waitFor(() =>
        expect(wrapper.findAll("li.suggestion")).toHaveLength(1),
      );
      await input.trigger("blur");

      expect(wrapper.emitted("select")).toEqual([[{ name: "New.Page" }]]);
    });

    it("selects nothing when the text is unchanged", async () => {
      const parseQuery = vi.fn((query: string) => ({ name: query }));
      const wrapper = mountSearchBox({
        initialValue: "Initial.Page",
        parseQuery,
      });

      await wrapper.get("input").trigger("blur");

      expect(wrapper.emitted("select")).toBeUndefined();
      expect(parseQuery).not.toHaveBeenCalled();
    });

    it("doesn't select again the text already selected with Enter", async () => {
      const wrapper = mountSearchBox({
        parseQuery: (query) => ({ name: query }),
      });
      const input = wrapper.get("input");

      await input.setValue("New.Page");
      await input.trigger("keydown", { key: "Enter" });
      await input.trigger("blur");

      expect(wrapper.emitted("select")).toEqual([[{ name: "New.Page" }]]);
    });

    it("doesn't select again the picked suggestion", async () => {
      const wrapper = mountSearchBox({
        suggestions: [item("Suggested.Page")],
        parseQuery: (query) => ({ name: `parsed:${query}` }),
      });
      const input = wrapper.get("input");

      await input.setValue("Sugg");
      await vi.waitFor(() =>
        expect(wrapper.findAll("li.suggestion")).toHaveLength(1),
      );
      await wrapper.get("li.suggestion").trigger("click");
      await input.trigger("blur");

      expect(wrapper.emitted("select")).toEqual([[{ name: "Suggested.Page" }]]);
    });
  });

  describe("emptying the text", () => {
    it("clears the selection when the field is left, whitespace counting as empty", async () => {
      const parseQuery = vi.fn((query: string) => ({ name: query }));
      const wrapper = mountSearchBox({
        initialValue: "Initial.Page",
        parseQuery,
      });
      const input = wrapper.get("input");

      await input.setValue("  ");
      await input.trigger("blur");

      expect(wrapper.emitted("select")).toEqual([[null]]);
      expect(parseQuery).not.toHaveBeenCalled();
    });

    it("clears nothing when the field is left with the text already empty", async () => {
      const wrapper = mountSearchBox({});

      await wrapper.get("input").trigger("blur");

      expect(wrapper.emitted("select")).toBeUndefined();
    });

    it("clears the selection once, when Enter is pressed", async () => {
      const wrapper = mountSearchBox({
        initialValue: "Initial.Page",
        suggestions: [item("Initial.Page")],
        parseQuery: (query) => ({ name: query }),
      });
      const input = wrapper.get("input");

      await input.setValue("");
      await input.trigger("keydown", { key: "Enter" });
      await input.trigger("blur");

      expect(wrapper.emitted("select")).toEqual([[null]]);
    });
  });

  describe("Escape key", () => {
    // Presses Escape in the search field and returns whether the event reached the listeners of the ancestors (e.g. the
    // dialog holding the search box) and whether its default action (e.g. closing a native dialog) was prevented.
    function pressEscape(wrapper: ReturnType<typeof mountSearchBox>) {
      const ancestorListener = vi.fn();
      wrapper.element.addEventListener("keydown", ancestorListener);
      const event = new KeyboardEvent("keydown", {
        key: "Escape",
        bubbles: true,
        cancelable: true,
      });
      wrapper.get("input").element.dispatchEvent(event);
      wrapper.element.removeEventListener("keydown", ancestorListener);
      return {
        propagated: ancestorListener.mock.calls.length > 0,
        defaultPrevented: event.defaultPrevented,
      };
    }

    it("closes the suggestions without closing the dialog holding the search box", async () => {
      const wrapper = mountSearchBox({ suggestions: [item("Some.Page")] });
      await wrapper.get("input").setValue("Some");
      await vi.waitFor(() =>
        expect(wrapper.findAll("li.suggestion")).toHaveLength(1),
      );

      expect(pressEscape(wrapper)).toEqual({
        propagated: false,
        defaultPrevented: true,
      });
      await vi.waitFor(() =>
        expect(wrapper.find(".suggestions-container").exists()).toBe(false),
      );
    });

    it("ignores the results of a search dismissed while loading", async () => {
      let resolveSearch: (
        results: SearchBoxItem<Value, string>[],
      ) => void = () => {};
      const wrapper = mountSearchBox({
        getSuggestions: () =>
          new Promise((resolve) => (resolveSearch = resolve)),
      });
      await wrapper.get("input").setValue("Some");
      await vi.waitFor(() =>
        expect(wrapper.find(".suggestions-container").exists()).toBe(true),
      );

      expect(pressEscape(wrapper).propagated).toBe(false);
      resolveSearch([item("Some.Page")]);
      await flushPromises();

      expect(wrapper.find(".suggestions-container").exists()).toBe(false);
    });

    it("is left to the dialog holding the search box when there are no suggestions displayed", () => {
      const wrapper = mountSearchBox({ suggestions: [item("Some.Page")] });

      expect(pressEscape(wrapper)).toEqual({
        propagated: true,
        defaultPrevented: false,
      });
    });
  });

  describe("suggestion list", () => {
    const scrollIntoView = vi.fn();

    beforeEach(() => {
      Element.prototype.scrollIntoView = scrollIntoView;
    });

    afterEach(() => {
      scrollIntoView.mockReset();
      delete (Element.prototype as Partial<Element>).scrollIntoView;
    });

    async function showSuggestions(count: number) {
      const wrapper = mountSearchBox({
        suggestions: Array.from({ length: count }, (_, i) => item(`Page${i}`)),
      });
      await wrapper.get("input").setValue("Page");
      await vi.waitFor(() =>
        expect(wrapper.findAll("li.suggestion")).toHaveLength(count),
      );
      return wrapper;
    }

    it("scrolls the suggestion focused with the arrow keys into view", async () => {
      const wrapper = await showSuggestions(7);
      const input = wrapper.get("input");

      for (let i = 0; i < 6; i++) {
        await input.trigger("keydown", { key: "ArrowDown" });
      }
      await vi.waitFor(() => expect(scrollIntoView).toHaveBeenCalledTimes(6));

      const focused = wrapper.get("li.suggestion.focused");
      expect(focused.text()).toBe("Page6");
      expect(scrollIntoView.mock.contexts.at(-1)).toBe(focused.element);
      expect(scrollIntoView).toHaveBeenLastCalledWith({ block: "nearest" });
    });
  });
});
