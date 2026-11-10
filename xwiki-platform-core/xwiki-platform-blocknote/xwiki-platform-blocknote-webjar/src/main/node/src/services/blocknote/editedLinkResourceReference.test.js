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
import { editedLinkResourceReference } from "./editedLinkResourceReference";
import { describe, expect, it } from "vitest";

function pageReference(reference, parameters = {}, typed = true) {
  return { type: "doc", typed, reference, parameters };
}

describe("editedLinkResourceReference", () => {
  it("saves the reference produced by the link editor when the link points to another resource", () => {
    const edited = pageReference("Space.Other", { anchor: "HSection" });

    expect(
      editedLinkResourceReference(
        pageReference("Space.Page", { anchor: "HOld", rel: "nofollow" }),
        edited,
      ),
    ).toBe(edited);
  });

  it("saves the reference produced by the link editor for a new link", () => {
    const edited = pageReference("Space.Page");

    expect(editedLinkResourceReference(undefined, edited)).toBe(edited);
  });

  it("saves the changed query string and anchor of a link to the same resource", () => {
    expect(
      editedLinkResourceReference(
        pageReference("Space.Page", { queryString: "a=1", anchor: "HOld" }),
        pageReference("Space.Page", { queryString: "b=2", anchor: "HNew" }),
      ),
    ).toEqual(
      pageReference("Space.Page", { queryString: "b=2", anchor: "HNew" }),
    );
  });

  it("saves the cleared query string and anchor of a link to the same resource", () => {
    expect(
      editedLinkResourceReference(
        pageReference("Space.Page", { queryString: "a=1", anchor: "HOld" }),
        pageReference("Space.Page"),
      ),
    ).toEqual(pageReference("Space.Page"));
  });

  it("preserves what the link editor doesn't edit, for a link to the same resource", () => {
    expect(
      editedLinkResourceReference(
        pageReference("Space.Page", { unknown: "kept" }, false),
        pageReference("Space.Page", { anchor: "HSection" }, true),
      ),
    ).toEqual(
      pageReference(
        "Space.Page",
        { unknown: "kept", anchor: "HSection" },
        false,
      ),
    );
  });
});
