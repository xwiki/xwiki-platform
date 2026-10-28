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

import { loadXWikiToolbarTranslations } from "../translations";
import { mockRequireJS } from "@xwiki/platform-test-requirejs";
import { loadById } from "@xwiki/platform-xwiki-utils";
import { afterEach, beforeEach, describe, expect, it } from "vitest";
import type { RequireJSMock } from "@xwiki/platform-test-requirejs";

describe("loadXWikiToolbarTranslations", () => {
  let requireJS: RequireJSMock;

  beforeEach(() => {
    requireJS = mockRequireJS({
      "xwiki-l10n!xwiki-realtime-toolbar-translation-keys": {
        "realtime.done": "Done",
        "core.document.modificationWithVersion": "Version {0} by {1} on {2}",
        // Not requested by the realtime toolbar.
        "other.key": "Other",
        get: () => "",
      },
    });
    document.documentElement.lang = "fr";
  });

  afterEach(() => {
    requireJS.restore();
    document.documentElement.lang = "";
  });

  it("loads the translations of the realtime toolbar", async () => {
    const i18n = await loadXWikiToolbarTranslations();

    const query = await loadById<{ keys: string[] }>(
      "xwiki-realtime-toolbar-translation-keys",
    );
    expect(query.keys).toContain("realtime.done");
    expect(query.keys).toContain("core.document.modificationWithVersion");

    const { locale, t, messages } = i18n.global as unknown as {
      locale: { value: string };
      t: (key: string, list?: unknown[]) => string;
      messages: { value: Record<string, Record<string, string>> };
    };
    expect(locale.value).toBe("fr");
    expect(Object.keys(messages.value.fr).sort()).toEqual([
      "core.document.modificationWithVersion",
      "realtime.done",
    ]);
    expect(t("realtime.done")).toBe("Done");
    expect(
      t("core.document.modificationWithVersion", ["1.1", "Alice", "today"]),
    ).toBe("Version 1.1 by Alice on today");
    // Missing translations are displayed as translation keys.
    expect(t("realtime.leave")).toBe("realtime.leave");
  });
});
