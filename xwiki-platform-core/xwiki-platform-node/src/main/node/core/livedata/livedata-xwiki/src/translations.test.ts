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

import { initTranslationsBuilder } from "./translations";
import { describe, expect, it, vi } from "vitest";
import { createI18n } from "vue-i18n";
import type { Resolver } from "@xwiki/platform-localization-api";
import type { I18n } from "vue-i18n";

function buildResolver(translations: Record<string, string> = {}): Resolver {
  return {
    resolve: vi.fn().mockResolvedValue({ translations }),
  };
}

function buildI18n(): I18n {
  return createI18n({ legacy: false, locale: "en", messages: {} }) as I18n;
}

describe("initTranslationsBuilder", () => {
  it("resolves the query in the locale of the translations", async () => {
    const resolver = buildResolver({ "a.key": "Bonjour" });
    const i18n = buildI18n();

    const translations = await initTranslationsBuilder(resolver)("fr", i18n)([
      "a.key",
    ]);

    expect(resolver.resolve).toHaveBeenCalledWith({
      keys: ["a.key"],
      locale: "fr",
    });
    expect(translations).toEqual({ "a.key": "Bonjour" });
    expect(i18n.global.getLocaleMessage("fr")).toEqual({
      "a.key": "Bonjour",
    });
  });

  it("keeps the prefix of an object query", async () => {
    const resolver = buildResolver();
    const i18n = buildI18n();

    await initTranslationsBuilder(resolver)("fr", i18n)({
      prefix: "ns.",
      keys: ["hello"],
    });

    expect(resolver.resolve).toHaveBeenCalledWith({
      prefix: "ns.",
      keys: ["hello"],
      locale: "fr",
    });
  });

  it("does not override the locale pinned by the query", async () => {
    const resolver = buildResolver();
    const i18n = buildI18n();

    await initTranslationsBuilder(resolver)("fr", i18n)({
      keys: ["hello"],
      locale: "de",
    });

    expect(resolver.resolve).toHaveBeenCalledWith({
      keys: ["hello"],
      locale: "de",
    });
  });
});
