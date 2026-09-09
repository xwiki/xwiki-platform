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

import { FIELDS_GROUP, METADATA_GROUP } from "../fieldPicker";
import { resolveSortOption, toSortOptions } from "../sortPicker";
import { describe, expect, it } from "vitest";
import type { PropertyDescriptor } from "../fieldPicker";

const DESCRIPTORS: PropertyDescriptor[] = [
  { id: "doc.title", name: "Title", type: "String" },
  { id: "budget", name: "Budget", type: "Number" },
  { id: "_actions", name: "Actions" },
];

describe("toSortOptions", () => {
  it("offers each field ascending and descending", () => {
    expect(toSortOptions(DESCRIPTORS, "budget")).toEqual([
      {
        value: "budget:asc",
        label: "Budget (ascending)",
        hint: "Number",
        optgroup: FIELDS_GROUP,
      },
      {
        value: "budget:desc",
        label: "Budget (descending)",
        hint: "Number",
        optgroup: FIELDS_GROUP,
      },
    ]);
  });

  it("groups the entry metadata apart from the fields", () => {
    expect(
      toSortOptions(DESCRIPTORS, "title").map((option) => option.optgroup),
    ).toEqual([METADATA_GROUP, METADATA_GROUP]);
  });

  it("leaves out the Live Data pseudo-columns", () => {
    expect(
      toSortOptions(DESCRIPTORS).some((option) =>
        option.value.startsWith("_actions"),
      ),
    ).toBe(false);
  });

  it("matches on the stored value as well as on the label", () => {
    expect(toSortOptions(DESCRIPTORS, "budget:desc")).toEqual([
      expect.objectContaining({ value: "budget:desc" }),
    ]);
  });

  it("offers nothing when the data type has no field", () => {
    expect(toSortOptions([])).toEqual([]);
  });

  it("stops offering a field that is already sorted on", () => {
    // Both directions go, not just the one picked: sorting a field twice says nothing the first criterion did not
    // already say.
    expect(
      toSortOptions(DESCRIPTORS, "", ["budget:asc"]).map((o) => o.value),
    ).toEqual(["doc.title:asc", "doc.title:desc"]);
  });

  it("recognises a used field whose criterion names no direction", () => {
    expect(
      toSortOptions(DESCRIPTORS, "", ["budget"]).map((o) => o.value),
    ).toEqual(["doc.title:asc", "doc.title:desc"]);
  });
});

describe("resolveSortOption", () => {
  it("shows a stored criterion with its field name and direction", () => {
    expect(resolveSortOption(DESCRIPTORS, "budget:desc")).toEqual({
      value: "budget:desc",
      label: "Budget (descending)",
      hint: "Number",
      optgroup: FIELDS_GROUP,
    });
  });

  it("keeps a criterion that names no direction, and says so", () => {
    expect(resolveSortOption(DESCRIPTORS, "budget")).toEqual(
      expect.objectContaining({
        value: "budget",
        label: "Budget (default order)",
      }),
    );
  });

  it("keeps a criterion naming a field the data type no longer has", () => {
    // The suggester refuses free text, so an option whose value is not exactly the stored one would drop what the
    // author wrote the next time the dialog opens.
    expect(resolveSortOption(DESCRIPTORS, "gone:asc")).toEqual(
      expect.objectContaining({ value: "gone:asc", label: "gone:asc" }),
    );
  });

  it("keeps a criterion whose direction is not one Live Data defines", () => {
    expect(resolveSortOption(DESCRIPTORS, "budget:sideways")).toEqual(
      expect.objectContaining({
        value: "budget:sideways",
        label: "Budget (default order)",
      }),
    );
  });
});
