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
let filters: typeof import("../filterSuggestions");

let element: Element;

let fetchJson: Mock<JsonFetcher>;

beforeEach(async () => {
  vi.resetModules();
  filters = await import("../filterSuggestions");
  ({ element, fetchJson } = givenPickerDialog());
});

afterEach(() => {
  vi.unstubAllGlobals();
});

describe("filterSuggestions", () => {
  it("suggests the fields that can be filtered on until a value separator is typed", async () => {
    expect(
      values(await filters.filterSuggestions.load(element, "", fetchJson, [])),
    ).toEqual(["doc.title=", "status=", "count="]);
  });

  it("suggests the values the field suggests once its value separator is typed", async () => {
    expect(
      values(
        await filters.filterSuggestions.load(element, "status=", fetchJson, []),
      ),
    ).toEqual(["status=Active", "status=On%20hold"]);
  });

  it("suggests no value for a field that suggests none, so the author can type one", async () => {
    await expect(
      filters.filterSuggestions.load(element, "count=", fetchJson, []),
    ).resolves.toEqual([]);
  });

  it("shows a stored constraint back with why the table ignores it", async () => {
    const [option] = await filters.filterSuggestions.resolve(
      element,
      "count=cheap",
      fetchJson,
    );
    expect(option.problem).toBe("picker.problem.valueDoesNotFit");
  });

  it("puts a picked field back in the text box, so the author carries on with its value", () => {
    const suggester = {
      removeItem: vi.fn(),
      setTextboxValue: vi.fn(),
      refreshOptions: vi.fn(),
    } as unknown as Suggester;

    filters.filterSuggestions.settings!.onItemAdd!.call(
      suggester,
      "status=",
      document.createElement("div"),
    );

    expect(suggester.removeItem).toHaveBeenCalledWith("status=", true);
    expect(suggester.setTextboxValue).toHaveBeenCalledWith("status=");
    expect(suggester.refreshOptions).toHaveBeenCalledWith(true);
  });

  it("empties the text box after a complete constraint, and checks it", () => {
    const loadSelected = vi.fn();
    const suggester = {
      removeItem: vi.fn(),
      setTextboxValue: vi.fn(),
      refreshOptions: vi.fn(),
      settings: { loadSelected },
    } as unknown as Suggester;

    filters.filterSuggestions.settings!.onItemAdd!.call(
      suggester,
      "status=Active",
      document.createElement("div"),
    );

    expect(suggester.removeItem).not.toHaveBeenCalled();
    expect(suggester.setTextboxValue).toHaveBeenCalledWith("");
    expect(loadSelected).toHaveBeenCalledWith(
      "status=Active",
      expect.any(Function),
    );
  });
});

describe("loadValues", () => {
  it("fills the typed text into the field's suggestion URL", async () => {
    fetchJson.mockResolvedValue({ propertyValues: [{ value: "Active" }] });

    await expect(
      filters.loadValues(
        "/xwiki/values?fp={encodedQuery}",
        "on hold",
        fetchJson,
      ),
    ).resolves.toEqual(["Active"]);
    expect(fetchJson.mock.calls[0][0]).toContain("/xwiki/values?fp=on%20hold");
  });

  it("suggests nothing when the values cannot be loaded, since a value can still be typed", async () => {
    fetchJson.mockRejectedValue(new Error("HTTP 500"));

    await expect(
      filters.loadValues("/xwiki/values?fp={encodedQuery}", "a", fetchJson),
    ).resolves.toEqual([]);
  });
});
