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
import { mount } from "@vue/test-utils";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { createI18n } from "vue-i18n";
import type { SearchBoxItem } from "../SearchBox.vue";

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
  placeholder?: string;
}) {
  return mount(SearchBox<Value, string>, {
    props: {
      label: "Reference",
      initialValue: null,
      getSuggestions: async () => props.suggestions ?? [],
      parseQuery: props.parseQuery,
      placeholder: props.placeholder,
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
