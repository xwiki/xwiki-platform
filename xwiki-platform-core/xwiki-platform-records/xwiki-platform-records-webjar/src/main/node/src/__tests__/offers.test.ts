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
import { typesUrl } from "../fieldUse";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import type { JsonFetcher, PropertyDescriptor } from "../fieldPicker";
import type { PropertyType } from "../fieldUse";
import type { Suggester } from "../suggester";

/**
 * The defaults of the types the tests use, as the types resource reports them.
 */
const TYPES: PropertyType[] = [
  { id: "String", sortable: true, filterable: true, filter: { id: "text" } },
  { id: "Number", sortable: true, filterable: true, filter: { id: "number" } },
  { id: "Password", sortable: false, filterable: false, filter: {} },
];

/**
 * What the properties resource reports for the data type.
 */
const DESCRIPTORS: PropertyDescriptor[] = [
  { id: "doc.title", name: "Title", type: "String" },
  {
    id: "status",
    name: "Status",
    type: "String",
    filter: { id: "text", searchURL: "/xwiki/values?fp={encodedQuery}" },
  },
  { id: "count", name: "Count", type: "Number" },
  { id: "code", name: "Code", type: "Password" },
];

/**
 * The module under test, imported again for each test, since the property types are cached per page.
 */
let offers: typeof import("../offers");

/**
 * The picker field, inside a dialog whose data type is picked.
 */
let element: Element;

/**
 * Answers each resource the offers ask, in the shape it really uses.
 */
let fetchJson: ReturnType<typeof vi.fn<JsonFetcher>>;

beforeEach(async () => {
  vi.stubGlobal("XWiki", { contextPath: "/xwiki" });
  vi.resetModules();
  offers = await import("../offers");
  document.body.innerHTML = `
    <form>
      <input name="class" value="Book.Class" />
      <input class="picker" />
    </form>`;
  element = document.querySelector(".picker")!;
  fetchJson = vi.fn<JsonFetcher>(async (url) => {
    if (url === typesUrl("/xwiki")) {
      return { types: TYPES };
    }
    if (url.includes("/values")) {
      return { propertyValues: [{ value: "Active" }, { value: "On hold" }] };
    }
    return { links: [], properties: DESCRIPTORS };
  });
});

afterEach(() => {
  vi.unstubAllGlobals();
});

/**
 * @param options - options, as an offer returns them
 * @returns their values
 */
function values(options: { value: string }[]): string[] {
  return options.map((option) => option.value);
}

describe("columnsOffer", () => {
  it("offers every field matching the query", async () => {
    expect(
      values(await offers.columnsOffer.load(element, "co", fetchJson, [])),
    ).toEqual(["count", "code"]);
  });
});

describe("sortOffer", () => {
  it("offers both directions of the fields that can be sorted on, minus those already sorted on", async () => {
    expect(
      values(
        await offers.sortOffer.load(element, "", fetchJson, ["doc.title:asc"]),
      ),
    ).toEqual(["status:asc", "status:desc", "count:asc", "count:desc"]);
  });

  it("shows a stored criterion back with why the table ignores it", async () => {
    const [option] = await offers.sortOffer.resolve(
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

    offers.sortOffer.settings!.onItemAdd!.call(suggester, "count:asc");
    offers.sortOffer.settings!.onItemRemove!.call(suggester, "count:asc");

    expect(suggester.clearOptions).toHaveBeenCalledTimes(2);
    expect(suggester.load).toHaveBeenCalledTimes(2);
  });
});

describe("filtersOffer", () => {
  it("offers the fields that can be filtered on until a value separator is typed", async () => {
    expect(
      values(await offers.filtersOffer.load(element, "", fetchJson, [])),
    ).toEqual(["doc.title=", "status=", "count="]);
  });

  it("offers the values the field suggests once its value separator is typed", async () => {
    expect(
      values(await offers.filtersOffer.load(element, "status=", fetchJson, [])),
    ).toEqual(["status=Active", "status=On%20hold"]);
  });

  it("offers no value for a field that suggests none, so the author can type one", async () => {
    await expect(
      offers.filtersOffer.load(element, "count=", fetchJson, []),
    ).resolves.toEqual([]);
  });

  it("shows a stored constraint back with why the table ignores it", async () => {
    const [option] = await offers.filtersOffer.resolve(
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

    offers.filtersOffer.settings!.onItemAdd!.call(suggester, "status=");

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

    offers.filtersOffer.settings!.onItemAdd!.call(suggester, "status=Active");

    expect(suggester.removeItem).not.toHaveBeenCalled();
    expect(suggester.setTextboxValue).toHaveBeenCalledWith("");
    expect(loadSelected).toHaveBeenCalledWith(
      "status=Active",
      expect.any(Function),
    );
  });
});
