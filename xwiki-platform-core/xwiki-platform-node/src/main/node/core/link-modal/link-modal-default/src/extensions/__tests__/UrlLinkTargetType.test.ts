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
import UrlConfig from "../../vue/UrlConfig.vue";
import { UrlLinkTargetType } from "../UrlLinkTargetType";
import { describe, expect, it } from "vitest";

describe("UrlLinkTargetType", () => {
  const extension = new UrlLinkTargetType();

  it("has the expected identity, and the lowest possible priority (highest order)", () => {
    expect(extension.type).toBe("url");
    expect(extension.order).toBe(Number.MAX_SAFE_INTEGER);
    expect(extension.getLabel("en")).toBe("URL");
    expect(extension.component()).toBe(UrlConfig);
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
});
