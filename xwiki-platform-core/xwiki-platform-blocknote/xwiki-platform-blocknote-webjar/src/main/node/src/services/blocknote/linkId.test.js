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
  LINK_ID_PARAM,
  extractLinkId,
  injectLinkId,
  stripLinkId,
} from "./linkId";
import { describe, expect, it } from "vitest";

describe("linkId", () => {
  const id = "123e4567-e89b-12d3-a456-426614174000";

  const hrefs = {
    "absolute url": "http://example.com/path",
    "root-relative url": "/xwiki/bin/view/Space/Page",
    "url with existing query": "/xwiki/bin/view/Space/Page?a=1&b=2",
    "url with fragment": "/xwiki/bin/view/Space/Page#section",
    "url with query and fragment": "/xwiki/bin/view/Space/Page?a=1#section",
    mailto: "mailto:john@example.com",
  };

  describe("injectLinkId then extractLinkId returns the id", () => {
    Object.entries(hrefs).forEach(([label, href]) => {
      it(label, () => {
        expect(extractLinkId(injectLinkId(href, id))).toBe(id);
      });
    });
  });

  describe("injectLinkId then stripLinkId restores the href", () => {
    Object.entries(hrefs).forEach(([label, href]) => {
      it(label, () => {
        expect(stripLinkId(injectLinkId(href, id))).toBe(href);
      });
    });
  });

  it("extractLinkId returns undefined when there is no synthetic id", () => {
    Object.values(hrefs).forEach((href) => {
      expect(extractLinkId(href)).toBeUndefined();
    });
  });

  it("stripLinkId leaves a href without synthetic id unchanged", () => {
    expect(stripLinkId("/xwiki/bin/view/Space/Page?a=1")).toBe(
      "/xwiki/bin/view/Space/Page?a=1",
    );
  });

  it("injectLinkId replaces an existing synthetic id instead of duplicating it", () => {
    const once = injectLinkId("/path", "first");
    const twice = injectLinkId(once, "second");
    expect(extractLinkId(twice)).toBe("second");
    expect(twice.match(new RegExp(LINK_ID_PARAM, "g")) ?? []).toHaveLength(1);
  });
});
