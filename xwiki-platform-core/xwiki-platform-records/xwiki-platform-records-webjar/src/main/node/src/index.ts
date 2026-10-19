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

// The build writes the imported styles to index.css rather than into the bundle. The displayer templates load that
// file through the link skin extension.
import "./records.css";
import { columnSuggestions } from "./columns/columnSuggestions";
import {
  clearMessages,
  findDataTypeInput,
  findDataTypeLabel,
  findScope,
  resetDerivedParameters,
  setTabsVisible,
  showMessage,
} from "./common/dialog";
import { hasDataTypeFields, loadDescriptors } from "./common/fields";
import { fetchJson } from "./common/remote";
import { createSettings, watchProblems } from "./common/suggester";
import {
  TRANSLATION_KEYS_MODULE,
  defineTranslationKeys,
  setMessages,
  translate,
} from "./common/translations";
import { filterSuggestions } from "./filters/filterSuggestions";
import { sortSuggestions } from "./sort/sortSuggestions";
import { loadById } from "@xwiki/platform-xwiki-utils";
import type { Suggester, Suggestions } from "./common/suggester";
import type { Messages } from "./common/translations";

/**
 * Wires the Records macro dialog: the field picker, and the two behaviours that depend on the data type.
 *
 * jQuery and the suggest widget are loaded through RequireJS rather than imported, so that they stay the single
 * instances the rest of the page already uses and nothing is bundled twice.
 *
 * The displayer templates load this bundle as a `type="module"` script. The macro editor injects them with jQuery,
 * which hands such a script to the browser, so the bundle is only fetched once the Records dialog is displayed, and
 * runs once however many of its displayers the dialog holds.
 */

/**
 * The CSS class the columns displayer template marks its input with.
 */
const PICKER_SELECTOR = ".suggest-records-fields";

/**
 * The CSS class the sort displayer template marks its input with.
 */
const SORT_SELECTOR = ".suggest-records-sort";

/**
 * The CSS class the filters displayer template marks its input with.
 */
const FILTERS_SELECTOR = ".suggest-records-filters";

/**
 * Marks a dialog as already wired, since `xwiki:dom:updated` fires more than once per dialog.
 */
const WIRED_FLAG = "recordsDialogWired";

/**
 * Warns, on the data type picker, when the data type a dialog was reopened on no longer exists.
 *
 * The saved configuration is left alone until the author picks a replacement, so that reopening a page does not
 * silently wipe it. The data type picker is not given the focus: that would open its dropdown over the message.
 *
 * @param picker - the columns picker element, through which the data type is read
 * @param dataTypeInput - the data type field
 */
async function warnIfDataTypeMissing(
  picker: Element,
  dataTypeInput: HTMLInputElement | HTMLSelectElement,
): Promise<void> {
  const dataType = dataTypeInput.value;
  let descriptors;
  try {
    descriptors = await loadDescriptors(picker, XWiki.contextPath, fetchJson);
  } catch {
    // The pickers make the same request and report its failure themselves.
    return;
  }
  // The author may have picked another data type while the request was running.
  if (hasDataTypeFields(descriptors) || dataTypeInput.value !== dataType) {
    return;
  }
  showMessage(
    dataTypeInput.selectize?.wrapper ?? dataTypeInput,
    "warning",
    translate("picker.dataTypeMissing", findDataTypeLabel(dataTypeInput)),
  );
}

/**
 * Enhances the three pickers of one dialog.
 *
 * They differ only in what they suggest, which is what {@link Suggestions} carries. The settings they share, such as
 * the widget, the failure handling and the placeholder behaviour, are built once in {@link createSettings}.
 *
 * @param $ - the page's jQuery instance
 * @param picker - the columns picker element
 * @param scope - the dialog's form
 */
function enhancePickers(
  $: JQueryStatic,
  picker: Element,
  scope: ParentNode,
): void {
  $(picker).xwikiSelectize(
    createSettings(picker, fetchJson, columnSuggestions),
  );
  const others: [string, Suggestions][] = [
    [SORT_SELECTOR, sortSuggestions],
    [FILTERS_SELECTOR, filterSuggestions],
  ];
  others.forEach(([selector, suggestions]) => {
    scope.querySelectorAll(selector).forEach((element) => {
      $(element).xwikiSelectize(
        createSettings(element, fetchJson, suggestions),
      );
      const suggester = element.selectize as Suggester | undefined;
      if (suggester) {
        watchProblems(suggester);
      }
    });
  });
}

/**
 * Wires one Records dialog.
 *
 * The columns picker is what identifies the dialog as a Records one, and it is also where the data type is
 * watched from, so that the warning and the reset happen once per dialog rather than once per picker.
 *
 * @param $ - the page's jQuery instance
 * @param picker - the columns picker element
 */
function wire($: JQueryStatic, picker: Element): void {
  const scope = findScope(picker);
  const dataTypeInput = findDataTypeInput(scope);

  enhancePickers($, picker, scope);

  // The tabs are all derived from the data type, so there is nothing to show until one is picked.
  setTabsVisible(scope, dataTypeInput !== null && dataTypeInput.value !== "");
  if (dataTypeInput === null) {
    return;
  }

  if (dataTypeInput.value !== "") {
    void warnIfDataTypeMissing(picker, dataTypeInput);
  }

  let previous = dataTypeInput.value;
  $(dataTypeInput).on("change", () => {
    const current = dataTypeInput.value;
    if (current === previous) {
      return;
    }
    // Only warn when there is something to lose: the first choice discards nothing.
    if (previous !== "" && !window.confirm(translate("picker.resetWarning"))) {
      revert(dataTypeInput, previous);
      return;
    }
    resetDerivedParameters(scope);
    clearMessages(scope);
    setTabsVisible(scope, current !== "");
    previous = current;
  });
}

/**
 * Puts the data type field back on the value the author declined to leave.
 *
 * The enhanced widget holds its own copy of the value, so setting the element's is not enough. It is told
 * silently, since restoring a value the author never actually left is not a change worth notifying.
 *
 * @param dataTypeInput - the data type field
 * @param value - the value to restore
 */
function revert(
  dataTypeInput: HTMLInputElement | HTMLSelectElement,
  value: string,
): void {
  dataTypeInput.value = value;
  dataTypeInput.selectize?.setValue(value, true);
}

defineTranslationKeys();

const [$, , translations] = await loadById<[JQueryStatic, unknown, Messages]>(
  "jquery",
  "xwiki-selectize",
  `xwiki-l10n!${TRANSLATION_KEYS_MODULE}`,
);
setMessages(translations);
const initialize = (event?: unknown, data?: { elements?: Element[] }) => {
  const roots: ParentNode[] = data?.elements ?? [document];
  roots.forEach((root) => {
    root.querySelectorAll(PICKER_SELECTOR).forEach((picker) => {
      const $picker = $(picker);
      if ($picker.data(WIRED_FLAG) === true) {
        return;
      }
      $picker.data(WIRED_FLAG, true);
      wire($, picker);
    });
  });
};
$(document).on("xwiki:dom:loaded xwiki:dom:updated", initialize);
initialize();
