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
import {
  asValues,
  encodeConstraint,
  isIncomplete,
  resolveFilterOption,
  splitConstraint,
  toFieldOptions,
  toValueOptions,
  valuesUrl,
} from "../filterPicker";
import { describe, expect, it } from "vitest";
import type { PropertyDescriptor } from "../fieldPicker";

const STATUS: PropertyDescriptor = {
  id: "status",
  name: "Status",
  type: "StaticList",
  filter: { searchURL: "/xwiki/rest/.../status/values?fp={encodedQuery}" },
};

const DESCRIPTORS: PropertyDescriptor[] = [
  STATUS,
  { id: "doc.title", name: "Title", type: "String" },
  { id: "_actions", name: "Actions" },
];

describe("splitConstraint", () => {
  it("splits a constraint on its first separator", () => {
    expect(splitConstraint("status=Active")).toEqual({
      field: "status",
      value: "Active",
    });
  });

  it("returns null while no value separator has been typed", () => {
    expect(splitConstraint("stat")).toBeNull();
  });

  it("decodes the value", () => {
    expect(splitConstraint("client=Acme%20%26%20Co")).toEqual({
      field: "client",
      value: "Acme & Co",
    });
  });

  it("keeps a value that is not valid percent-encoding", () => {
    // decodeURIComponent throws on a bare %, and a keystroke handler must not.
    expect(splitConstraint("discount=100%")).toEqual({
      field: "discount",
      value: "100%",
    });
  });

  it("reads an empty value as empty rather than as no constraint", () => {
    expect(splitConstraint("status=")).toEqual({ field: "status", value: "" });
  });
});

describe("encodeConstraint", () => {
  it("encodes what the parameter's own separators would otherwise swallow", () => {
    // The renderer splits on & and on the first =, then URL-decodes, so an unencoded value holding either would
    // be read as a second constraint.
    expect(encodeConstraint("client", "Acme & Co")).toBe(
      "client=Acme%20%26%20Co",
    );
    expect(encodeConstraint("formula", "a=b")).toBe("formula=a%3Db");
  });
});

describe("toFieldOptions", () => {
  it("offers the fields as constraints waiting for a value", () => {
    expect(toFieldOptions(DESCRIPTORS, "status")).toEqual([
      {
        value: "status=",
        label: "Status =",
        hint: "StaticList",
        optgroup: FIELDS_GROUP,
      },
    ]);
  });

  it("groups the entry metadata apart", () => {
    expect(toFieldOptions(DESCRIPTORS, "title")[0].optgroup).toBe(
      METADATA_GROUP,
    );
  });

  it("leaves out the Live Data pseudo-columns", () => {
    expect(
      toFieldOptions(DESCRIPTORS).some((option) =>
        option.value.startsWith("_actions"),
      ),
    ).toBe(false);
  });
});

describe("isIncomplete", () => {
  it("tells a field waiting for its value from a usable constraint", () => {
    expect(isIncomplete("status=")).toBe(true);
    expect(isIncomplete("status=Active")).toBe(false);
  });
});

describe("valuesUrl", () => {
  it("puts the typed text where the placeholder is, encoded", () => {
    expect(
      valuesUrl(
        "/xwiki/rest/values?fp={encodedQuery}",
        "on hold",
        "http://wiki/xwiki/bin/view/Space/Page",
      ),
    ).toBe("http://wiki/xwiki/rest/values?fp=on%20hold");
  });

  it("resolves a search URL that is only a query string", () => {
    // The user and group suggester is reported as '?xpage=uorgsuggest&...', relative to the current page.
    expect(
      valuesUrl(
        "?xpage=uorgsuggest&input={encodedQuery}",
        "ad",
        "http://wiki/xwiki/bin/view/Space/Page",
      ),
    ).toBe("http://wiki/xwiki/bin/view/Space/Page?xpage=uorgsuggest&input=ad");
  });
});

describe("asValues", () => {
  it("reads the class property values resource", () => {
    expect(
      asValues({ propertyValues: [{ value: "Text" }, { value: "Wysiwyg" }] }),
    ).toEqual(["Text", "Wysiwyg"]);
  });

  it("reads a bare array, as the user suggester answers", () => {
    expect(asValues([{ id: "XWiki.Admin" }, "XWiki.Guest"])).toEqual([
      "XWiki.Admin",
      "XWiki.Guest",
    ]);
  });

  it("yields nothing for a shape it does not know", () => {
    expect(asValues({ unexpected: true })).toEqual([]);
    expect(asValues(null)).toEqual([]);
  });
});

describe("toValueOptions", () => {
  it("builds one encoded constraint per value", () => {
    expect(toValueOptions(["Active", "On hold"], STATUS)).toEqual([
      {
        value: "status=Active",
        label: "Status = Active",
        hint: "StaticList",
        optgroup: FIELDS_GROUP,
      },
      {
        value: "status=On%20hold",
        label: "Status = On hold",
        hint: "StaticList",
        optgroup: FIELDS_GROUP,
      },
    ]);
  });

  it("narrows the values to what was typed after the separator", () => {
    expect(
      toValueOptions(["Active", "On hold"], STATUS, "hold").map((o) => o.value),
    ).toEqual(["status=On%20hold"]);
  });
});

describe("resolveFilterOption", () => {
  it("shows a stored constraint with its field name and decoded value", () => {
    expect(resolveFilterOption(DESCRIPTORS, "status=On%20hold")).toEqual({
      value: "status=On%20hold",
      label: "Status = On hold",
      hint: "StaticList",
      optgroup: FIELDS_GROUP,
    });
  });

  it("keeps a constraint naming a field the data type no longer has", () => {
    expect(resolveFilterOption(DESCRIPTORS, "gone=1")).toEqual(
      expect.objectContaining({ value: "gone=1", label: "gone=1" }),
    );
  });

  it("keeps something that is not a constraint at all", () => {
    expect(resolveFilterOption(DESCRIPTORS, "nonsense")).toEqual(
      expect.objectContaining({ value: "nonsense", label: "nonsense" }),
    );
  });
});
