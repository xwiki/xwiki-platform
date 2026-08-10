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
import { UrlLinkTargetType } from "../UrlLinkTargetType";
import { describe, expect, it } from "vitest";

describe("UrlLinkTargetType", () => {
  const extension = new UrlLinkTargetType();

  it("has the expected identity, and the lowest possible priority (highest order)", () => {
    expect(extension.type).toBe("url");
    expect(extension.order).toBe(Number.MAX_SAFE_INTEGER);
    expect(extension.getLabel("en")).toBe("URL");
  });

  it("matches any URL, unconditionally", () => {
    expect(
      extension.tryParseUrl("anything at all, not even a valid URL"),
    ).toEqual({
      url: "anything at all, not even a valid URL",
    });
  });

  it("serializes back to the exact same URL", () => {
    expect(extension.serializeUrl({ url: "https://example.org" })).toBe(
      "https://example.org",
    );
  });

  it("converts references with no dedicated configuration into a bare URL", () => {
    ["url", "path", "unc", "unknown"].forEach((type) => {
      expect(
        extension.tryParseReference?.({
          type,
          typed: false,
          reference: "some/target",
          parameters: {},
        }),
      ).toEqual({ url: "some/target" });
    });
  });

  it("does not match resource types with their own dedicated link target, unlike tryParseUrl", () => {
    ["doc", "attach", "mailto", "interwiki", "space", "user"].forEach(
      (type) => {
        expect(
          extension.tryParseReference?.({
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
      extension.configToReference?.({ url: "https://example.org" }),
    ).toEqual({
      type: "url",
      typed: false,
      reference: "https://example.org",
      parameters: {},
    });
  });

  it("returns undefined when converting to a reference with an empty URL", () => {
    expect(extension.configToReference?.({ url: "  " })).toBeUndefined();
  });
});
