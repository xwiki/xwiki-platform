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

import {
  findDataTypeInput,
  findScope,
  resetDerivedParameters,
  setTabsVisible,
} from "./dialog";
import { loadDescriptors, loadOptions } from "./fieldPicker";
import { resolveSortOption, toSortOptions } from "./sortPicker";
import type { FieldOption, JsonFetcher } from "./fieldPicker";

/**
 * Wires the Records macro dialog: the field picker, and the two behaviours that depend on the data type.
 *
 * jQuery and the suggest widget are taken as RequireJS dependencies rather than imported, so that they stay the
 * single instances the rest of the page already uses and nothing is bundled twice. That is also why this module
 * registers itself with `define` and then requires itself.
 *
 * The bundle deliberately exports nothing. The macro editor builds each parameter field in a detached element and
 * only then appends it to the dialog, and jQuery evaluates a field's scripts at that append: a classic script runs,
 * a `type="module"` one is skipped outright, and ES syntax in a classic one fails to parse. Keeping this module
 * free of imports and exports leaves the emitted bundle valid as a classic script, which is what the displayer
 * template loads it as.
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
 * Marks a dialog as already wired, since `xwiki:dom:updated` fires more than once per dialog.
 */
const WIRED_FLAG = "recordsDialogWired";

/**
 * What the author is warned with before a change of data type discards their configuration.
 *
 * Kept in English here rather than read from a translation bundle because a webjar has no access to one; the
 * message belongs in the macro's bundle once the picker gets a proper in-dialog error channel.
 */
const RESET_WARNING =
  "Changing the data type resets the columns, the filters and the sort, " +
  "because they name fields of the data type you are leaving.\n\nChange it anyway?";

/**
 * The suggest widget settings this picker needs.
 */
interface PickerSettings {
  load: (query: string, callback: (options: FieldOption[]) => void) => void;
  loadSelected: (
    value: string,
    callback: (options: FieldOption[]) => void,
  ) => void;
  optgroups: { value: string; label: string }[];
  optgroupField: string;
  labelField: string;
  valueField: string;
  searchField: string[];
  plugins: string[];
  persist: boolean;
  create: boolean;
  hidePlaceholder: boolean;
}

/**
 * Builds the suggest widget settings for one picker input.
 *
 * `persist` is off and the options are cleared whenever the data type changes, because the widget would otherwise
 * keep offering the previous data type's columns: the suggest widget caches what it has loaded, and the author
 * changing their mind about the data type is exactly the case that must not silently keep stale fields.
 *
 * `hidePlaceholder` is set for the reason given below: it is not the widget's default for a multiple-value field,
 * and every such field in the platform has carried the placeholder alongside its items since Tom Select replaced
 * Selectize.
 *
 * @param element - the field input being enhanced
 * @param fetchJson - fetches and parses the Live Data properties resource
 * @returns the settings to hand to the suggest widget
 */
function createSettings(
  element: Element,
  fetchJson: JsonFetcher,
  offer: Offer = columnsOffer,
): PickerSettings {
  // A failure to reach the properties resource must not break the keystroke handler: the dropdown simply has
  // nothing to offer, which is the same state as "no data type picked yet".
  const guard =
    (produce: (query: string) => Promise<FieldOption[]>) =>
    (query: string, callback: (options: FieldOption[]) => void) => {
      void (async () => {
        try {
          callback(await produce(query));
        } catch {
          callback([]);
        }
      })();
    };
  return {
    load: guard((query) => offer.load(element, query, fetchJson)),
    loadSelected: guard((value) => offer.resolve(element, value, fetchJson)),
    optgroups: [
      { value: "fields", label: "Fields" },
      { value: "metadata", label: "Entry metadata" },
    ],
    optgroupField: "optgroup",
    labelField: "label",
    valueField: "value",
    searchField: ["label", "value"],
    // Both the column order and the order of the sort criteria are authored, so the selected items must be
    // reorderable.
    plugins: ["drag_drop", "remove_button"],
    persist: false,
    // A column the data type does not have would render an empty column, so free text is refused.
    create: false,
    // The suggest widget is backed by Tom Select, whose `hidePlaceholder` defaults to `mode !==
    // 'multi'`, so a multiple-value field keeps its placeholder next to the selected items. Here that
    // placeholder names the default column list, so leaving it visible would tell the author the table
    // shows the title and every field while they are looking at the columns they just picked.
    hidePlaceholder: true,
  };
}

/**
 * What one picker offers: the options for a query, and the option that shows a stored value back.
 */
interface Offer {
  load: (
    element: Element,
    query: string,
    fetchJson: JsonFetcher,
  ) => Promise<FieldOption[]>;
  resolve: (
    element: Element,
    value: string,
    fetchJson: JsonFetcher,
  ) => Promise<FieldOption[]>;
}

/**
 * The columns picker: one option per field, and a stored value is an identifier the same request resolves.
 */
const columnsOffer: Offer = {
  load: (element, query, fetchJson) =>
    loadOptions(element, query, XWiki.contextPath, fetchJson),
  resolve: (element, value, fetchJson) =>
    loadOptions(element, value, XWiki.contextPath, fetchJson),
};

/**
 * The sort picker: two options per field, one per direction, and a stored criterion is resolved exactly rather
 * than searched, for the reason given on {@link resolveSortOption}.
 */
const sortOffer: Offer = {
  load: async (element, query, fetchJson) =>
    toSortOptions(
      await loadDescriptors(element, XWiki.contextPath, fetchJson),
      query,
    ),
  resolve: async (element, value, fetchJson) => [
    resolveSortOption(
      await loadDescriptors(element, XWiki.contextPath, fetchJson),
      value,
    ),
  ],
};

/**
 * Fetches and parses a JSON document.
 *
 * @param url - the URL to fetch
 * @returns the parsed body
 */
async function fetchJson(url: string): Promise<unknown> {
  const response = await fetch(url, {
    headers: { Accept: "application/json" },
  });
  if (!response.ok) {
    throw new Error(`Failed to load [${url}]: ${response.status}`);
  }
  return response.json();
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
  const $picker = $(picker);
  const scope = findScope(picker);
  const dataTypeInput = findDataTypeInput(scope);

  $picker.xwikiSelectize(createSettings(picker, fetchJson));
  scope.querySelectorAll(SORT_SELECTOR).forEach((sort) => {
    $(sort).xwikiSelectize(createSettings(sort, fetchJson, sortOffer));
  });

  // The tabs are all derived from the data type, so there is nothing to show until one is picked.
  setTabsVisible(scope, dataTypeInput !== null && dataTypeInput.value !== "");
  if (dataTypeInput === null) {
    return;
  }

  let previous = dataTypeInput.value;
  $(dataTypeInput).on("change", () => {
    const current = dataTypeInput.value;
    if (current === previous) {
      return;
    }
    // Only warn when there is something to lose: the first choice discards nothing.
    if (previous !== "" && !window.confirm(RESET_WARNING)) {
      revert(dataTypeInput, previous);
      return;
    }
    resetDerivedParameters(scope);
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
  const enhanced = (
    dataTypeInput as unknown as {
      selectize?: { setValue: (value: string, silent?: boolean) => void };
    }
  ).selectize;
  enhanced?.setValue(value, true);
}

define("xwiki-records-fields", ["jquery", "xwiki-selectize"], function (
  $: JQueryStatic,
) {
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
});

// Kick the module off, since `define` on its own only registers it. The `requirejs` alias is used rather than
// `require` so that this reads as the AMD loader it is rather than as a CommonJS import.
requirejs(["xwiki-records-fields"], () => {});
