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
import { createI18n } from "vue-i18n";

vi.mock("@xwiki/platform-xwiki-utils", () => ({ loadById: vi.fn() }));

// The translations the server would serve, read from the actual resource bundle of this module.
const bundle = Object.fromEntries(
  applicationResources
    .split("\n")
    .filter((line) => /^[^#\s]/.test(line))
    .map((line) => {
      const separator = line.indexOf("=");
      return [
        line.slice(0, separator).trim(),
        line.slice(separator + 1).trim(),
      ];
    }),
);

// The keys the editor asks the server for, captured from the RequireJS module i18nResolver defines.
let requestedConfig;

beforeEach(() => {
  requestedConfig = undefined;
  // Both the resolver and Logic read the locale from there, so the messages land under the locale i18n uses.
  document.documentElement.lang = "en";
  globalThis.define = (moduleName, config) => {
    requestedConfig = config;
  };
  // Serve only the requested keys, like the localization module does, and only those that the bundle actually
  // defines, so that a key missing from either side ends up missing from the messages.
  loadById.mockImplementation(async () => [
    requestedConfig,
    Object.fromEntries(
      requestedConfig.keys
        .map((key) => [key, bundle[`${requestedConfig.prefix}${key}`]])
        .filter(([, message]) => message !== undefined),
    ),
  ]);
});

async function resolveMessages() {
  const i18n = createI18n({
    legacy: false,
    locale: document.documentElement.lang,
  });
  await i18nResolver(i18n);
  return i18n.global;
}

describe("i18nResolver", () => {
  it("requests only keys the resource bundle defines", async () => {
    await resolveMessages();
    expect(
      requestedConfig.keys.filter(
        (key) => bundle[`${requestedConfig.prefix}${key}`] === undefined,
      ),
    ).toEqual([]);
  });

  it("translates the upload failure message with the file name", async () => {
    const { t } = await resolveMessages();
    expect(t("blocknote.upload.failed", ["notes.txt"])).toBe(
      "Failed to upload [notes.txt].",
    );
  });
});
