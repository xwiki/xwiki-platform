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
import "reflect-metadata";
import { XWikiStorage } from "../xwikiStorage";
import { DefaultLogger, type WikiConfig } from "@xwiki/cristal-api";
import { describe, expect, it } from "vitest";

import type {
  AuthenticationManager,
  AuthenticationManagerProvider,
} from "@xwiki/cristal-authentication-api";

describe("getPageFromViewURL", () => {
  const wikiConfig: WikiConfig = {
    baseURL: "<baseURL>",
  } as WikiConfig;

  class MockAuthenticationManagerProvider
    implements AuthenticationManagerProvider
  {
    get(): AuthenticationManager | undefined {
      return undefined;
    }
  }

  const xwikiStorage: XWikiStorage = new XWikiStorage(
    new DefaultLogger(),
    new MockAuthenticationManagerProvider(),
  );
  xwikiStorage.setWikiConfig(wikiConfig);
  it("regular identifier", () => {
    expect(
      xwikiStorage.getPageFromViewURL(
        "<baseURL>/bin/view/Space1/Space2/WebHome",
      ),
    ).toStrictEqual("Space1.Space2.WebHome");
  });
  it("identifier with special characters", () => {
    expect(
      xwikiStorage.getPageFromViewURL(
        "<baseURL>/bin/view/Space1%5C.Space%5C2/Web%2FHome",
      ),
    ).toStrictEqual("Space1\\\\\\.Space\\\\2.Web/Home");
  });
  it("missing terminal page", () => {
    expect(
      xwikiStorage.getPageFromViewURL("<baseURL>/bin/view/Space1/Space2/"),
    ).toStrictEqual("Space1.Space2.WebHome");
  });
});
