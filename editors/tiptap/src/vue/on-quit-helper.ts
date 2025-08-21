/**
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
import messages from "../translations";
import { useI18n } from "vue-i18n";
import type { BrowserApi } from "@xwiki/cristal-browser-api";
import type { Router } from "vue-router";

function hasUnsavedContent(currentContent: string, lastSavedContent: string) {
  return currentContent !== lastSavedContent;
}

function initTranslations() {
  const { t, mergeLocaleMessage } = useI18n();
  for (const messagesKey in messages) {
    mergeLocaleMessage(messagesKey, messages[messagesKey]);
  }
  return t;
}

export function initOnQuitHelper(
  getContent: () => string,
  router: Router,
  browserApi: BrowserApi,
): { update(): void } {
  let lastSavedContent: string = getContent();
  const t = initTranslations();

  // Specific for navigation handled by vue through the router (e.g., click on a link or a button on the UI, making
  // the user leave the current view).
  router.beforeEach(() => {
    if (hasUnsavedContent(getContent(), lastSavedContent)) {
      return confirm(t("tiptap.editor.onquit.message"));
    }
    return true;
  });

  // Specific for the case when the editor is left because the current window is left by click on an element outside
  // the webview (e.g., a tab or an electron window is closed).
  browserApi.onClose(() => {
    if (hasUnsavedContent(getContent(), lastSavedContent)) {
      return confirm(t("tiptap.editor.onquit.message"));
    }
    return false;
  });

  return {
    update() {
      lastSavedContent = getContent();
    },
  };
}
