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
import { ref, watchEffect } from "vue";
import type { Ref } from "vue";

type Define = (id: string, definition: unknown) => void;

const REQUIRED_KEY = "core.validation.required";

const TRANSLATION_QUERY_ID = "xwiki-flamingo-design-system-translation-keys";

// Shared by all the fields, so that the translation is loaded only once.
const requiredMark: Ref<string | undefined> = ref();

let loading: Promise<void> | undefined;

async function loadRequiredMark(): Promise<void> {
  try {
    // We use the xwiki-l10n RequireJS plugin because it shares the translations (cache) with the rest of the page.
    (globalThis as unknown as { define: Define }).define(TRANSLATION_QUERY_ID, {
      keys: [REQUIRED_KEY],
    });
    const translations = await loadById<Record<string, unknown>>(
      `xwiki-l10n!${TRANSLATION_QUERY_ID}`,
    );
    const translation = translations[REQUIRED_KEY];
    if (typeof translation === "string") {
      requiredMark.value = translation;
    }
  } catch {
    // The field is still required, it's just not marked as such in its label. The next required field tries again.
    loading = undefined;
  }
}

/**
 * The text marking the label of a required field, as XWiki forms do (e.g. "(Required)"). It is loaded from the server
 * the first time a required field is displayed.
 *
 * @param required - whether the field is required
 * @returns the text marking the label of a required field, once loaded
 */
function useRequiredMark(
  required: () => boolean | undefined,
): Readonly<Ref<string | undefined>> {
  watchEffect(() => {
    if (required()) {
      loading ??= loadRequiredMark();
    }
  });
  return requiredMark;
}

export { useRequiredMark };
