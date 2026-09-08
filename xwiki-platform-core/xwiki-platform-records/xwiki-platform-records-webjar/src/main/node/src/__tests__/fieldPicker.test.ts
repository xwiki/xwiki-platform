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
  FIELDS_GROUP,
  METADATA_GROUP,
  asDescriptors,
  findDataType,
  loadOptions,
  propertiesUrl,
  toOptions,
} from "../fieldPicker";
import { beforeEach, describe, expect, it, vi } from "vitest";
import type { JsonFetcher, PropertyDescriptor } from "../fieldPicker";
import type { Mock } from "vitest";

/**
 * Builds a macro dialog holding a data type input and a field picker, as the macro editor renders it.
 *
 * @param dataType - the value of the data type input
 * @returns the field picker element
 */
function givenDialog(dataType: string): Element {
  document.body.innerHTML = `
    <form>
      <input name="class" value="${dataType}" />
      <select name="properties" class="suggest-records-fields" multiple></select>
    </form>`;
  return document.querySelector(".suggest-records-fields")!;
}

const DESCRIPTORS: PropertyDescriptor[] = [
  { id: "doc.title", name: "Title", type: "String" },
  { id: "status", name: "Status", type: "StaticList" },
  { id: "budget", name: "Budget", type: "Number" },
];

describe("findDataType", () => {
  it("reads the value of the sibling class parameter", () => {
    expect(findDataType(givenDialog("Clients.Code.ProjectClass"))).toBe(
      "Clients.Code.ProjectClass",
    );
  });

  it("returns null when no data type is picked yet", () => {
    expect(findDataType(givenDialog(""))).toBeNull();
  });

  it("returns null when the class input holds only whitespace", () => {
    expect(findDataType(givenDialog("   "))).toBeNull();
  });

  it("does not read the class input of another dialog", () => {
    document.body.innerHTML = `
      <form id="other"><input name="class" value="Other.Class" /></form>
      <form id="mine">
        <input name="class" value="Mine.Class" />
        <select class="suggest-records-fields"></select>
      </form>`;
    const picker = document.querySelector("#mine .suggest-records-fields")!;
    expect(findDataType(picker)).toBe("Mine.Class");
  });

  it("falls back to the document when the picker is not inside a form", () => {
    document.body.innerHTML = `
      <input name="class" value="Loose.Class" />
      <select class="suggest-records-fields"></select>`;
    const picker = document.querySelector(".suggest-records-fields")!;
    expect(findDataType(picker)).toBe("Loose.Class");
  });
});

describe("propertiesUrl", () => {
  it("asks the Live Data properties resource for the given data type", () => {
    expect(propertiesUrl("/xwiki", "Clients.Code.ProjectClass")).toBe(
      "/xwiki/rest/liveData/sources/liveTable/properties" +
        "?sourceParams.className=Clients.Code.ProjectClass&media=json",
    );
  });

  it("encodes a reference holding characters that are special in a query string", () => {
    const url = propertiesUrl("/xwiki", "Space.A&B=C");
    expect(url).toContain("sourceParams.className=Space.A%26B%3DC");
    expect(url).toContain("media=json");
  });
});

describe("toOptions", () => {
  it("separates the entry metadata from the data type's own fields", () => {
    const options = toOptions(DESCRIPTORS);
    expect(options).toEqual([
      {
        value: "doc.title",
        label: "Title",
        hint: "String",
        optgroup: METADATA_GROUP,
      },
      {
        value: "status",
        label: "Status",
        hint: "StaticList",
        optgroup: FIELDS_GROUP,
      },
      {
        value: "budget",
        label: "Budget",
        hint: "Number",
        optgroup: FIELDS_GROUP,
      },
    ]);
  });

  it("falls back to the identifier when a property has no translated name", () => {
    const options = toOptions([{ id: "internalField" }]);
    expect(options[0]).toMatchObject({
      value: "internalField",
      label: "internalField",
      optgroup: FIELDS_GROUP,
    });
    expect(options[0].hint).toBeUndefined();
  });

  it("matches the query against the label", () => {
    expect(toOptions(DESCRIPTORS, "stat").map((o) => o.value)).toEqual([
      "status",
    ]);
  });

  it("matches the query against the identifier, so doc.title is reachable by typing doc", () => {
    expect(toOptions(DESCRIPTORS, "doc").map((o) => o.value)).toEqual([
      "doc.title",
    ]);
  });

  it("ignores the case and the surrounding spaces of the query", () => {
    expect(toOptions(DESCRIPTORS, "  BUDGET ").map((o) => o.value)).toEqual([
      "budget",
    ]);
  });

  it("offers everything when the query is empty", () => {
    expect(toOptions(DESCRIPTORS, "")).toHaveLength(3);
  });

  it("hides the Live Data pseudo-columns", () => {
    // The resource reports these next to the real fields; they are affordances, not data.
    const descriptors = [
      { id: "_actions" },
      { id: "_avatar" },
      { id: "_images" },
      { id: "_attachments" },
      { id: "first_name", name: "First Name" },
    ] as PropertyDescriptor[];
    expect(toOptions(descriptors).map((o) => o.value)).toEqual(["first_name"]);
  });

  it("falls back to the identifier when the resource sends a null name", () => {
    // doc.* descriptors really do come back with name: null.
    const descriptors = [
      { id: "doc.title", name: null, type: "String" },
    ] as unknown as PropertyDescriptor[];
    expect(toOptions(descriptors)[0]).toMatchObject({
      value: "doc.title",
      label: "doc.title",
      optgroup: METADATA_GROUP,
    });
  });

  it("skips descriptors without a usable identifier", () => {
    const descriptors = [{ id: "" }, { id: "kept" }] as PropertyDescriptor[];
    expect(toOptions(descriptors).map((o) => o.value)).toEqual(["kept"]);
  });
});

describe("asDescriptors", () => {
  it("reads the descriptors out of the resource's envelope", () => {
    // The shape the properties resource actually answers with.
    const payload = {
      links: [{ href: "..." }, { href: "..." }],
      properties: [{ id: "doc.title" }, { id: "first_name" }],
    };
    expect(asDescriptors(payload).map((d) => d.id)).toEqual([
      "doc.title",
      "first_name",
    ]);
  });

  it("accepts a bare array too", () => {
    expect(asDescriptors([{ id: "a" }, { id: "b" }])).toHaveLength(2);
  });

  it("drops entries that are not property descriptors", () => {
    expect(
      asDescriptors([{ id: "a" }, null, 42, "b", { name: "no id" }]),
    ).toEqual([{ id: "a" }]);
  });

  it("yields nothing when the payload holds no properties", () => {
    expect(asDescriptors({ links: [] })).toEqual([]);
    expect(asDescriptors({ properties: "not an array" })).toEqual([]);
    expect(asDescriptors(null)).toEqual([]);
    expect(asDescriptors(undefined)).toEqual([]);
  });
});

describe("loadOptions", () => {
  let fetchJson: Mock<JsonFetcher>;

  beforeEach(() => {
    fetchJson = vi.fn<JsonFetcher>();
    // Answer in the shape the resource really uses.
    fetchJson.mockResolvedValue({ links: [], properties: DESCRIPTORS });
  });

  it("loads the columns of the selected data type", async () => {
    const picker = givenDialog("Clients.Code.ProjectClass");
    const options = await loadOptions(picker, "", "/xwiki", fetchJson);

    expect(fetchJson).toHaveBeenCalledWith(
      propertiesUrl("/xwiki", "Clients.Code.ProjectClass"),
    );
    expect(options.map((option) => option.value)).toEqual([
      "doc.title",
      "status",
      "budget",
    ]);
  });

  it("offers nothing, without requesting anything, while no data type is picked", async () => {
    const options = await loadOptions(givenDialog(""), "", "/xwiki", fetchJson);

    expect(options).toEqual([]);
    expect(fetchJson).not.toHaveBeenCalled();
  });

  it("applies the query to what it loaded", async () => {
    const picker = givenDialog("Clients.Code.ProjectClass");
    const options = await loadOptions(picker, "budget", "/xwiki", fetchJson);
    expect(options.map((option) => option.value)).toEqual(["budget"]);
  });

  it("propagates a failure of the properties resource to its caller", async () => {
    const picker = givenDialog("Clients.Code.ProjectClass");
    fetchJson.mockRejectedValue(new Error("HTTP 500"));

    await expect(loadOptions(picker, "", "/xwiki", fetchJson)).rejects.toThrow(
      "HTTP 500",
    );
  });
});
