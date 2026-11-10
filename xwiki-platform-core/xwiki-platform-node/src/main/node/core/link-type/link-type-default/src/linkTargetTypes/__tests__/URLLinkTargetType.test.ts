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
import { URLLinkTargetType } from "../URLLinkTargetType";
import { describe, expect, it } from "vitest";

describe("URLLinkTargetType", () => {
  const targetType = new URLLinkTargetType();

  it("has the expected identity, and the lowest possible priority (highest order)", async () => {
    expect(targetType.type).toBe("url");
    expect(targetType.order).toBe(Number.MAX_SAFE_INTEGER);
    await expect(targetType.getLabel("en")).resolves.toBe("URL");
  });

  it("matches any URL, unconditionally", () => {
    expect(targetType.fromURL("anything at all, not even a valid URL")).toEqual(
      {
        location: "anything at all, not even a valid URL",
      },
    );
  });

  it("serializes back to the exact same URL", () => {
    expect(targetType.toURL({ location: "https://example.org" })).toBe(
      "https://example.org",
    );
  });

  it("converts a URL reference into its URL", () => {
    expect(
      targetType.fromReference?.({
        type: "url",
        typed: false,
        reference: "https://example.org",
        parameters: {},
      }),
    ).toEqual({ location: "https://example.org" });
  });

  it("matches only URL references, unlike fromURL", () => {
    ["doc", "attach", "mailto", "path", "unc", "unknown", "interwiki"].forEach(
      (type) => {
        expect(
          targetType.fromReference?.({
            type,
            typed: true,
            reference: "Space.Page",
            parameters: {},
          }),
        ).toBeNull();
      },
    );
  });

  it("converts a URL config back into an untyped reference", () => {
    expect(
      targetType.toReference?.({ location: "https://example.org" }),
    ).toEqual({
      type: "url",
      typed: false,
      reference: "https://example.org",
      parameters: {},
    });
  });

  it("returns undefined when converting to a reference with an empty URL", () => {
    expect(targetType.toReference?.({ location: "  " })).toBeUndefined();
  });

  it("labels the link with its URL, like XWiki Rendering does for the URL links without a label", () => {
    expect(
      targetType.getDefaultLabel({
        location: " https://www.example.org/docs/guide.pdf ",
      }),
    ).toBe("https://www.example.org/docs/guide.pdf");
  });
});
