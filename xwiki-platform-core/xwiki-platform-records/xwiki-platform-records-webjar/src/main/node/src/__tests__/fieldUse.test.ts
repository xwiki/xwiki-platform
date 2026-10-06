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
  asTypes,
  canFilter,
  canSort,
  filterProblem,
  fits,
  sortProblem,
  typesUrl,
} from "../fieldUse";
import { describe, expect, it } from "vitest";
import type { PropertyDescriptor } from "../fieldPicker";
import type { PropertyType } from "../fieldUse";

/**
 * The defaults of the types the tests use, as the types resource reports them.
 */
const TYPES: PropertyType[] = [
  { id: "String", sortable: true, filterable: true, filter: { id: "text" } },
  { id: "Number", sortable: true, filterable: true, filter: { id: "number" } },
  {
    id: "Boolean",
    sortable: true,
    filterable: true,
    filter: { id: "boolean", trueValue: 1, falseValue: 0 },
  },
  { id: "Password", sortable: false, filterable: false, filter: {} },
];

/**
 * What the properties resource reports: every flag it was not given reads false, whatever the type allows.
 */
const DESCRIPTORS: PropertyDescriptor[] = [
  { id: "doc.title", name: "Title", type: "String" },
  {
    id: "doc.hidden",
    name: "Hidden",
    type: "Boolean",
    filter: { id: "boolean", trueValue: true, falseValue: false },
  },
  { id: "_actions", name: "Actions" },
  { id: "name", name: "Name", type: "String" },
  { id: "count", name: "Count", type: "Number" },
  { id: "active", name: "Active", type: "Boolean" },
  { id: "code", name: "Code", type: "Password" },
  { id: "rating", name: "Rating", type: "StarRating" },
].map((descriptor) => ({ ...descriptor, sortable: false, filterable: false }));

/**
 * @param id - a field identifier
 * @returns its descriptor
 */
function field(id: string): PropertyDescriptor {
  return DESCRIPTORS.find((descriptor) => descriptor.id === id)!;
}

describe("typesUrl", () => {
  it("points at the types of the liveTable source", () => {
    expect(typesUrl("/xwiki")).toBe(
      "/xwiki/rest/liveData/sources/liveTable/types?media=json",
    );
  });
});

describe("asTypes", () => {
  it("reads the types out of the envelope, and drops what has no identifier", () => {
    expect(
      asTypes({ links: [], types: [{ id: "String" }, { name: "x" }] }),
    ).toEqual([{ id: "String" }]);
  });

  it("yields no type for anything else", () => {
    expect(asTypes(null)).toEqual([]);
    expect(asTypes({ types: "String" })).toEqual([]);
  });
});

describe("canSort and canFilter", () => {
  it("follow the type rather than the flags the properties resource reports", () => {
    expect(canSort(field("count"), TYPES)).toBe(true);
    expect(canFilter(field("count"), TYPES)).toBe(true);
    expect(canSort(field("code"), TYPES)).toBe(false);
    expect(canFilter(field("code"), TYPES)).toBe(false);
  });

  it("allow nothing for a type the source has no defaults for", () => {
    expect(canSort(field("rating"), TYPES)).toBe(false);
    expect(canFilter(field("_actions"), TYPES)).toBe(false);
  });
});

describe("fits", () => {
  it("takes only numbers for a number field", () => {
    expect(fits(field("count"), TYPES, " 12.5 ")).toBe(true);
    expect(fits(field("count"), TYPES, "-3e2")).toBe(true);
    expect(fits(field("count"), TYPES, "cheap")).toBe(false);
  });

  it("takes the values of the field's own boolean filter, which differ between fields and page metadata", () => {
    expect(fits(field("active"), TYPES, "1")).toBe(true);
    expect(fits(field("active"), TYPES, "yes")).toBe(false);
    expect(fits(field("doc.hidden"), TYPES, "true")).toBe(true);
    expect(fits(field("doc.hidden"), TYPES, "1")).toBe(false);
  });

  it("takes anything for a text field, and an empty value for any field", () => {
    expect(fits(field("name"), TYPES, "100%")).toBe(true);
    expect(fits(field("count"), TYPES, "")).toBe(true);
  });
});

describe("sortProblem", () => {
  it("says nothing about a field that can be sorted on", () => {
    expect(sortProblem(DESCRIPTORS, TYPES, "count", "Book")).toBeNull();
  });

  it("names the field that cannot be sorted on", () => {
    expect(sortProblem(DESCRIPTORS, TYPES, "code", "Book")).toEqual({
      key: "picker.problem.notSortable",
      args: ["Code"],
    });
  });

  it("names the field the data type no longer has, and the data type", () => {
    expect(sortProblem(DESCRIPTORS, TYPES, "budget", "Book")).toEqual({
      key: "picker.problem.fieldMissing",
      args: ["budget", "Book"],
    });
  });

  it("says nothing of a pseudo-column the data type does not report, which the macro passes through", () => {
    expect(sortProblem(DESCRIPTORS, TYPES, "_avatar", "Book")).toBeNull();
  });

  it("says nothing when the data type is gone or out of reach, which the dialog reports on its own", () => {
    const metadataOnly = DESCRIPTORS.filter((descriptor) =>
      descriptor.id.startsWith("doc."),
    );
    expect(sortProblem(metadataOnly, TYPES, "budget", "Book")).toBeNull();
  });
});

describe("filterProblem", () => {
  it("says nothing about a value the field can take", () => {
    expect(
      filterProblem(DESCRIPTORS, TYPES, "count=%2012%20", "Book"),
    ).toBeNull();
  });

  it("names the field that cannot be filtered on", () => {
    expect(filterProblem(DESCRIPTORS, TYPES, "code=s1", "Book")).toEqual({
      key: "picker.problem.notFilterable",
      args: ["Code"],
    });
  });

  it("names the value the field cannot take, decoded as the renderer decodes it", () => {
    expect(
      filterProblem(DESCRIPTORS, TYPES, "count=very+cheap", "Book"),
    ).toEqual({
      key: "picker.problem.valueDoesNotFit",
      args: ["Count", "very cheap"],
    });
  });

  it("names the field the data type no longer has", () => {
    expect(filterProblem(DESCRIPTORS, TYPES, "budget=1", "Book")).toEqual({
      key: "picker.problem.fieldMissing",
      args: ["budget", "Book"],
    });
  });

  it("names a constraint the renderer cannot decode", () => {
    expect(filterProblem(DESCRIPTORS, TYPES, "name=100%", "Book")).toEqual({
      key: "picker.problem.unreadable",
      args: ["name=100%"],
    });
  });

  it("says nothing about a constraint that names no field, which the macro drops silently", () => {
    expect(filterProblem(DESCRIPTORS, TYPES, "=x", "Book")).toBeNull();
  });
});
