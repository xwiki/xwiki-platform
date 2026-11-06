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

import { loadById } from "@xwiki/platform-xwiki-utils";
import { createI18n } from "vue-i18n";
import type { I18n } from "vue-i18n";

type Define = (id: string, definition: unknown) => void;

/**
 * The translations resolved by the `xwiki-l10n!` RequireJS plugin, indexed by translation key, along with a function
 * to format them.
 */
type ResolvedTranslations = Record<string, unknown>;

/**
 * The translation keys used by the realtime toolbar. Note that the translation messages must use only the vue-i18n
 * list interpolation syntax (e.g. `{0}`) because they are compiled by vue-i18n.
 */
const TRANSLATION_KEYS = [
  "cancel",
  "core.comment.tooltip",
  "core.document.modificationWithVersion",
  "core.menu.toggleDropdown",
  "core.validation.required",
  "realtime.changes",
  "realtime.changes.empty",
  "realtime.changes.error",
  "realtime.changes.isnew",
  "realtime.close",
  "realtime.connecting",
  "realtime.disconnected",
  "realtime.done",
  "realtime.done.hint",
  "realtime.failedToLoadVersion",
  "realtime.history",
  "realtime.history.hint",
  "realtime.leave",
  "realtime.leave.accept",
  "realtime.leave.confirmation",
  "realtime.leave.hint",
  "realtime.minorChange",
  "realtime.minorChange.hint",
  "realtime.saveStatus.saved",
  "realtime.saveStatus.saving",
  "realtime.saveStatus.unsaved",
  "realtime.sessionReplacedWarning",
  "realtime.sessionReplacedWarning.message",
  "realtime.summarize",
  "realtime.summarize.hint",
  "realtime.summarizeAndDone",
  "realtime.summarizeAndDone.hint",
  "realtime.summary",
  "realtime.users",
  "realtime.users.hint",
  "save",
];

const TRANSLATION_QUERY_ID = "xwiki-realtime-toolbar-translation-keys";

/**
 * Loads the translations of the realtime toolbar from the XWiki server, so that realtime toolbars can then be created
 * synchronously.
 *
 * @returns the vue-i18n instance holding the translations of the realtime toolbar
 * @since 18.9.0RC1
 * @beta
 */
async function loadXWikiToolbarTranslations(): Promise<I18n> {
  // We use the xwiki-l10n RequireJS plugin because it shares the translations (cache) with the rest of the page.
  (globalThis as unknown as { define: Define }).define(TRANSLATION_QUERY_ID, {
    keys: TRANSLATION_KEYS,
  });
  const translations = await loadById<ResolvedTranslations>(
    `xwiki-l10n!${TRANSLATION_QUERY_ID}`,
  );
  const messages = Object.fromEntries(
    TRANSLATION_KEYS.filter((key) => typeof translations[key] === "string").map(
      (key) => [key, translations[key] as string],
    ),
  );
  const locale = document.documentElement.lang || "en";
  return createI18n({
    legacy: false,
    locale,
    messages: { [locale]: messages },
    // Display the translation key when the translation is missing.
    missingWarn: false,
    fallbackWarn: false,
  });
}

export { loadXWikiToolbarTranslations };
