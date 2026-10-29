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
import { i18nResolver } from "./i18nResolver";
import applicationResources from "../../../resources/ApplicationResources.properties?raw";
import { loadById } from "@xwiki/platform-xwiki-utils";
import { beforeEach, describe, expect, it, vi } from "vitest";

vi.mock("@xwiki/platform-xwiki-utils", () => ({ loadById: vi.fn() }));

describe("i18nResolver", () => {
  // The keys the editor asks the server for, captured from the RequireJS module the resolver defines.
  let requestedKeys;

  beforeEach(async () => {
    globalThis.define = (moduleName, config) => {
      requestedKeys = config.keys;
    };
    loadById.mockResolvedValue([{ locale: "en", prefix: "blocknote." }, {}]);
    await i18nResolver({ global: { setLocaleMessage: vi.fn() } });
  });

  it("asks the server for the upload failure message", () => {
    // Without the key the editor would show "blocknote.upload.failed" when an upload fails.
    expect(requestedKeys).toContain("upload.failed");
  });

  it("gets every key it asks for from the resource bundle of this module", () => {
    expect(
      requestedKeys.filter(
        (key) => !applicationResources.includes(`\nblocknote.${key}=`),
      ),
    ).toEqual([]);
  });

  it("names the file in the upload failure message", () => {
    expect(applicationResources).toMatch(/^blocknote\.upload\.failed=.*\{0\}/m);
  });
});
