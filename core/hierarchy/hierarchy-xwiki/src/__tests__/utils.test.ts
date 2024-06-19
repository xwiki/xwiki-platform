/*
 * See the LICENSE file distributed with this work for additional
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

import type { WikiConfig } from "@xwiki/cristal-api";
import { describe, expect, it } from "vitest";

import { getRestSpacesApiUrl } from "../utils";

describe("getRestSpacesApiUrl", () => {
  const wikiConfig: WikiConfig = {
    baseURL: "<baseURL>",
  } as WikiConfig;
  it("regular identifier", () => {
    expect(
      getRestSpacesApiUrl(wikiConfig, "Space1.Space2.WebHome"),
    ).toStrictEqual(
      "<baseURL>/rest/wikis/xwiki/spaces/Space1/spaces/Space2/pages/WebHome",
    );
  });
  it("identifier with special characters", () => {
    expect(
      getRestSpacesApiUrl(wikiConfig, "Space1\\\\\\.Space\\\\2.Web/Home"),
    ).toStrictEqual(
      "<baseURL>/rest/wikis/xwiki/spaces/Space1%5C%2ESpace%5C2/pages/Web%2FHome",
    );
  });
});
