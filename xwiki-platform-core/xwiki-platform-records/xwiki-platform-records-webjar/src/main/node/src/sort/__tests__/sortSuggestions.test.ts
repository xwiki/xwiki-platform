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
import {
  givenPickerDialog,
  values,
} from "../../common/__tests__/suggestions.mock";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import type { JsonFetcher } from "../../common/fields";
import type { Suggester } from "../../common/suggester";
import type { Mock } from "vitest";

/**
 * The module under test, imported again for each test, since the property types are cached per page.
 */
let sort: typeof import("../sortSuggestions");

let element: Element;

let fetchJson: Mock<JsonFetcher>;

beforeEach(async () => {
  vi.resetModules();
  sort = await import("../sortSuggestions");
  ({ element, fetchJson } = givenPickerDialog());
});

afterEach(() => {
  vi.unstubAllGlobals();
});

describe("directionLabels", () => {
  it("translates the three labels a sort criterion is offered under", async () => {
    // The translations module was imported again along with the module under test, so it is the one to set.
    const translations = await import("../../common/translations");
    translations.setMessages({ get: (key) => `[${key}]` });
    expect(sort.directionLabels()).toEqual({
      ascending: "[picker.sort.ascending]",
      descending: "[picker.sort.descending]",
      defaultOrder: "[picker.sort.default]",
    });
  });
});

describe("sortSuggestions", () => {
  it("suggests both directions of the fields that can be sorted on, minus those already sorted on", async () => {
    expect(
      values(
        await sort.sortSuggestions.load(element, "", fetchJson, [
          "doc.title:asc",
        ]),
      ),
    ).toEqual(["status:asc", "status:desc", "count:asc", "count:desc"]);
  });

  it("shows a stored criterion back with why the table ignores it", async () => {
    const [option] = await sort.sortSuggestions.resolve(
      element,
      "code:desc",
      fetchJson,
    );
    expect(option.value).toBe("code:desc");
    expect(option.problem).toBe("picker.problem.notSortable");
  });

  it("loads the options again when a criterion is added or removed", () => {
    const suggester = {
      clearOptions: vi.fn(),
      load: vi.fn(),
      inputValue: () => "",
    } as unknown as Suggester;

    const item = document.createElement("div");
    sort.sortSuggestions.settings!.onItemAdd!.call(
      suggester,
      "count:asc",
      item,
    );
    sort.sortSuggestions.settings!.onItemRemove!.call(
      suggester,
      "count:asc",
      item,
    );

    expect(suggester.clearOptions).toHaveBeenCalledTimes(2);
    expect(suggester.load).toHaveBeenCalledTimes(2);
  });
});
