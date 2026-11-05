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
import { describe, expect, it, vi } from "vitest";
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
}) {
  return mount(SearchBox<Value, string>, {
    props: {
      label: "Reference",
      initialValue: null,
      getSuggestions: async () => props.suggestions ?? [],
      parseQuery: props.parseQuery,
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
