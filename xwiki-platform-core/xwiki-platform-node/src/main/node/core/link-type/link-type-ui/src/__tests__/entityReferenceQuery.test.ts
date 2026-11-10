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
import { entityReferenceQuery } from "../entityReferenceQuery";
import { describe, expect, it, vi } from "vitest";
import type { EntityReference } from "@xwiki/platform-model-api";

const ref = { name: "Other" } as unknown as EntityReference;
const sourceDocument = { name: "Source" } as unknown as NonNullable<
  Parameters<typeof entityReferenceQuery>[3]
>["sourceDocument"];

describe("entityReferenceQuery", () => {
  it("serializes the reference relative to the source document of the link", () => {
    const serialize = vi.fn(() => "Other");

    expect(
      entityReferenceQuery(ref, { serialize }, "doc:", { sourceDocument }),
    ).toBe("Other");
    expect(serialize).toHaveBeenCalledWith(ref, sourceDocument);
  });

  it("serializes the reference fully when the source document is unknown", () => {
    const serialize = vi.fn(() => "xwiki:Space.Other");

    expect(entityReferenceQuery(ref, { serialize }, "doc:")).toBe(
      "xwiki:Space.Other",
    );
    expect(serialize).toHaveBeenCalledWith(ref, undefined);
  });

  it("removes the entity type prefix added by the serializer", () => {
    expect(
      entityReferenceQuery(
        ref,
        { serialize: () => "attach:cat.png" },
        "attach:",
      ),
    ).toBe("cat.png");
  });
});
