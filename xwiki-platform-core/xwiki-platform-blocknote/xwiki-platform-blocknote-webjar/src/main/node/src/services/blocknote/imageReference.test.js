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
import { toRelativeImageReference } from "./imageReference";
import { EntityType, Model } from "@xwiki/platform-xwiki-model-api";
import { describe, expect, it } from "vitest";

describe("toRelativeImageReference", () => {
  function relativeTo(documentReference, reference, typed = false) {
    return toRelativeImageReference(
      { type: "attach", typed, reference, parameters: {} },
      Model.resolve(documentReference, EntityType.DOCUMENT),
    );
  }

  it("keeps only the name of an attachment of the edited document", () => {
    expect(
      relativeTo("xwiki:Space.Page", "xwiki:Space.Page@cat.png", true),
    ).toEqual({
      type: "attach",
      typed: false,
      reference: "cat.png",
      parameters: {},
    });
  });

  it("keeps a reference that is already relative to the edited document", () => {
    expect(relativeTo("xwiki:Space.Page", "cat.png").reference).toBe("cat.png");
  });

  it("makes the reference of an attachment of another page relative to the edited document", () => {
    expect(
      relativeTo("xwiki:Space.Page", "xwiki:Space.Other@cat.png").reference,
    ).toBe("Other@cat.png");
    expect(
      relativeTo("xwiki:A.B.WebHome", "xwiki:A.C.WebHome@x.png").reference,
    ).toBe("A.C.WebHome@x.png");
  });

  it("keeps the parent spaces of an attachment of a child page", () => {
    // Relative to the edited document, "C.WebHome@x.png" would point to the top level C page.
    expect(
      relativeTo("xwiki:A.B.WebHome", "xwiki:A.B.C.WebHome@x.png").reference,
    ).toBe("A.B.C.WebHome@x.png");
  });

  it("keeps the wiki of an attachment from another wiki", () => {
    expect(
      relativeTo("xwiki:Space.Page", "dev:Space.Page@cat.png").reference,
    ).toBe("dev:Space.Page@cat.png");
  });

  it("keeps the escaped characters of the attachment name", () => {
    expect(
      relativeTo("xwiki:Space.Page", "xwiki:Space.Page@a\\@b:c.png").reference,
    ).toBe("a\\@b:c.png");
  });

  it("keeps the parameters of the resource reference", () => {
    const reference = toRelativeImageReference(
      {
        type: "attach",
        typed: true,
        reference: "xwiki:Space.Page@cat.png",
        parameters: { queryString: "width=50" },
      },
      Model.resolve("xwiki:Space.Page", EntityType.DOCUMENT),
    );
    expect(reference.parameters).toEqual({ queryString: "width=50" });
  });

  it("leaves the references that don't point to an attachment unchanged", () => {
    const reference = {
      type: "url",
      typed: false,
      reference: "https://example.org/cat.png",
      parameters: {},
    };
    expect(
      toRelativeImageReference(
        reference,
        Model.resolve("xwiki:Space.Page", EntityType.DOCUMENT),
      ),
    ).toBe(reference);
  });
});
