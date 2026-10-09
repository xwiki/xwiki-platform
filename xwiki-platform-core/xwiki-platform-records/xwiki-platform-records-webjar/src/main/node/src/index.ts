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
  clearMessage,
  clearMessages,
  findDataTypeInput,
  findDataTypeLabel,
  findScope,
  resetDerivedParameters,
  setTabsVisible,
  showMessage,
  showProblems,
} from "./dialog";
import { hasDataTypeFields, loadDescriptors, loadOptions } from "./fieldPicker";
import {
  asTypes,
  canFilter,
  canSort,
  filterProblem,
  sortProblem,
  typesUrl,
} from "./fieldUse";
import {
  FILTER_SEPARATOR,
  asValues,
  createFilterOption,
  isIncomplete,
  resolveFilterOption,
  splitTyped,
  toFieldOptions,
  toValueOptions,
  valuesUrl,
} from "./filterPicker";
import { fieldOf, resolveSortOption, toSortOptions } from "./sortPicker";
import { loadById } from "@xwiki/platform-xwiki-utils";
import type {
  FieldOption,
  JsonFetcher,
  PropertyDescriptor,
} from "./fieldPicker";
import type { Problem, PropertyType } from "./fieldUse";
import type { DirectionLabels } from "./sortPicker";

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
 * The module holding the translation keys the dialog needs, which the `xwiki-l10n` loader plugin resolves into the
 * translated messages. The keys are the ones of the macro's translation bundle, without the prefix.
 */
const TRANSLATION_KEYS_MODULE = "xwiki-records-translation-keys";

/**
 * The prefix the translation keys of the macro's bundle share.
 */
const TRANSLATION_PREFIX = "rendering.macro.records.";

/**
 * The translated messages, as the `xwiki-l10n` loader plugin hands them over.
 */
interface Messages {
  get: (key: string, ...args: string[]) => string | null;
}

/**
 * The messages the dialog displays. Set when the module is initialized, which is before anything reads it.
 */
let messages: Messages = { get: () => null };

/**
 * @param key - a translation key, without the prefix
 * @param args - the values of the message parameters
 * @returns the translated message, or the key when the bundle does not have it, so that a missing translation shows
 *   up as an identifier rather than as an empty label
 */
function translate(key: string, ...args: string[]): string {
  return messages.get(key, ...args) ?? key;
}

/**
 * @returns the translated labels a sort criterion is offered under
 */
function directionLabels(): DirectionLabels {
  return {
    ascending: translate("picker.sort.ascending"),
    descending: translate("picker.sort.descending"),
    defaultOrder: translate("picker.sort.default"),
  };
}

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
  create: boolean | ((input: string) => FieldOption | null);
  hidePlaceholder: boolean;
  delimiter?: string;
  onItemAdd?: (this: Suggester, value: string) => void;
  onItemRemove?: (this: Suggester, value: string) => void;
  onFocus?: (this: Suggester) => void;
}

/**
 * The part of the suggest widget's API this module drives.
 */
interface Suggester {
  /**
   * The values currently selected.
   */
  items: string[];
  /**
   * The options the widget knows, by value.
   */
  options: Record<string, FieldOption>;
  settings: PickerSettings;
  /**
   * The element the widget is rendered in, which replaces the enhanced field on screen.
   */
  wrapper: HTMLElement;
  addOption: (option: FieldOption) => void;
  updateOption: (value: string, option: FieldOption) => void;
  /**
   * Asks the `load` setting for the options matching a query, unless that query has already been loaded.
   */
  load: (query: string) => void;
  /**
   * @returns the text the author has typed in the widget
   */
  inputValue: () => string;
  removeItem: (value: string, silent?: boolean) => void;
  setTextboxValue: (value: string) => void;
  refreshOptions: (triggerDropdown?: boolean) => void;
  /**
   * Drops every option that is not backing a selected item, and clears the search cache with them.
   */
  clearOptions: () => void;
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
  // A failure to reach the properties resource must not break the keystroke handler, and must not pass for "this
  // object type has no fields" either: the dropdown has nothing to offer, and a message under the widget says why
  // and offers to try again. What the author already chose stays selected, and is saved as it is.
  const guard = (
    produce: (query: string, selected: string[]) => Promise<FieldOption[]>,
  ) =>
    // Not an arrow function: the widget calls this with itself as `this`, which is how a picker knows what is
    // already selected.
    function (
      this: Suggester | undefined,
      query: string,
      callback: (options: FieldOption[]) => void,
    ) {
      const selected = this?.items ?? [];
      void (async () => {
        try {
          callback(await produce(query, selected));
          clearMessage(this?.wrapper ?? element, "error");
          if (this) {
            refreshProblems(this);
          }
        } catch {
          callback([]);
          reportLoadFailure(element, this);
        }
      })();
    };
  return {
    load: guard((query, selected) =>
      offer.load(element, query, fetchJson, selected),
    ),
    loadSelected: guard((value) => offer.resolve(element, value, fetchJson)),
    optgroups: [
      { value: "fields", label: translate("picker.group.fields") },
      { value: "metadata", label: translate("picker.group.metadata") },
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
    // The widget asks for the options of the empty query once, when it is created, and afterwards only as the author
    // types. A picker created before the data type was picked, or whose options were dropped since, would otherwise
    // open empty until something is typed. Asking costs nothing when the query is already loaded: the widget
    // remembers what it has asked for.
    onFocus() {
      this.load(this.inputValue());
    },
    ...offer.settings,
  };
}

/**
 * Drops the options a picker has loaded, except those backing its selected items, and asks for the ones matching
 * what is typed again.
 *
 * @param suggester - the widget to load again
 */
function reload(suggester: Suggester): void {
  suggester.clearOptions();
  suggester.load(suggester.inputValue());
}

/**
 * Tells the author, under the picker, that the fields of the data type could not be loaded, and offers to try again.
 *
 * @param element - the field input of the picker
 * @param suggester - the widget enhancing it, when the failure happened in one
 */
function reportLoadFailure(
  element: Element,
  suggester: Suggester | undefined,
): void {
  const dataTypeInput = findDataTypeInput(findScope(element));
  const dataType = dataTypeInput ? findDataTypeLabel(dataTypeInput) : "";
  showMessage(
    suggester?.wrapper ?? element,
    "error",
    translate("picker.loadFailed", dataType),
    suggester && {
      label: translate("picker.retry"),
      run: () => retry(suggester),
    },
  );
}

/**
 * Loads a picker's options again after a failure.
 *
 * The widget remembers every query it has asked for, failed or not, so that memory is dropped first. The selected
 * values are resolved again too: when the failure happened as the dialog opened, they are still shown as raw
 * identifiers rather than under their labels.
 *
 * @param suggester - the widget to load again
 */
function retry(suggester: Suggester): void {
  suggester.clearOptions();
  suggester.items.forEach((value) => resolveAgain(suggester, value));
  suggester.load(suggester.inputValue());
}

/**
 * Resolves one selected value again, replacing the option it is shown with.
 *
 * @param suggester - the widget holding the value
 * @param value - the selected value
 */
function resolveAgain(suggester: Suggester, value: string): void {
  suggester.settings.loadSelected.call(suggester, value, (options) => {
    options.forEach((option) => {
      if (Object.hasOwn(suggester.options, option.value)) {
        suggester.updateOption(option.value, option);
      } else {
        suggester.addOption(option);
      }
    });
  });
}

/**
 * Points at the selected items of a picker that the table ignores, from the problem their options carry.
 *
 * @param suggester - the widget whose items to check
 */
function refreshProblems(suggester: Suggester): void {
  showProblems(suggester.wrapper, (value) => suggester.options[value]?.problem);
}

/**
 * Keeps the problems of a picker's items shown as the items change: when the stored ones are resolved, and when
 * the author adds or removes one.
 *
 * @param suggester - the widget to watch
 */
function watchProblems(suggester: Suggester): void {
  const control = suggester.wrapper.querySelector(".ts-control");
  if (control === null) {
    return;
  }
  // Only the list of items is watched: marking an item changes its attributes, which must not trigger it again.
  new MutationObserver(() => refreshProblems(suggester)).observe(control, {
    childList: true,
  });
}

/**
 * The property types of the source, loaded once per page since they do not depend on the data type.
 */
let types: Promise<PropertyType[]> | null = null;

/**
 * @param fetchJson - fetches and parses the types resource
 * @returns the property types of the source, which say what a field of each type can be used for
 */
function loadTypes(fetchJson: JsonFetcher): Promise<PropertyType[]> {
  if (types === null) {
    types = fetchJson(typesUrl(XWiki.contextPath)).then(asTypes);
    // A failure is reported by the picker that asked, and the next load tries again.
    types.catch(() => {
      types = null;
    });
  }
  return types;
}

/**
 * @param element - a picker element
 * @returns the name the author knows the selected data type under
 */
function dataTypeName(element: Element): string {
  const dataTypeInput = findDataTypeInput(findScope(element));
  return dataTypeInput ? findDataTypeLabel(dataTypeInput) : "";
}

/**
 * @param option - the option showing a stored value back
 * @param problem - why the table ignores that value, if it does
 * @returns the option, carrying the translated problem
 */
function withProblem(
  option: FieldOption,
  problem: Problem | null,
): FieldOption {
  return problem === null
    ? option
    : { ...option, problem: translate(problem.key, ...problem.args) };
}

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
 * What one picker offers: the options for a query, and the option that shows a stored value back.
 */
interface Offer {
  load: (
    element: Element,
    query: string,
    fetchJson: JsonFetcher,
    selected: string[],
  ) => Promise<FieldOption[]>;
  resolve: (
    element: Element,
    value: string,
    fetchJson: JsonFetcher,
  ) => Promise<FieldOption[]>;
  /**
   * What this picker needs on top of the settings every picker shares.
   */
  settings?: Partial<PickerSettings>;
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
 * The sort picker: two options per field, one per direction, minus the fields already sorted on, since sorting a
 * field twice adds nothing. A stored criterion is resolved exactly rather than searched, for the reason given on
 * {@link resolveSortOption}.
 */
const sortOffer: Offer = {
  load: async (element, query, fetchJson, selected) => {
    const sourceTypes = await loadTypes(fetchJson);
    return toSortOptions(
      await loadDescriptors(element, XWiki.contextPath, fetchJson),
      query,
      selected,
      directionLabels(),
      (descriptor) => canSort(descriptor, sourceTypes),
    );
  },
  resolve: async (element, value, fetchJson) => {
    const sourceTypes = await loadTypes(fetchJson);
    const descriptors = await loadDescriptors(
      element,
      XWiki.contextPath,
      fetchJson,
    );
    return [
      withProblem(
        resolveSortOption(descriptors, value, directionLabels()),
        sortProblem(
          descriptors,
          sourceTypes,
          fieldOf(value),
          dataTypeName(element),
        ),
      ),
    ];
  },
  settings: {
    // A field that has just been used, or has just been freed, changes what should be offered. The widget caches
    // what it has loaded per query and keeps the options it has already seen, so both are dropped here, and loaded
    // again right away since the dropdown stays open for the next criterion.
    onItemAdd() {
      reload(this);
    },
    onItemRemove() {
      reload(this);
    },
  },
};

/**
 * The filters picker: one item per `field=value` constraint.
 *
 * The suggestions come in two steps, because a constraint has two halves and only the author knows the first one.
 * Until a value separator is typed the dropdown offers the fields; once one is typed, it offers that field's
 * values when the source reports a way to suggest them, and otherwise stays out of the way so the author can type
 * a value freely. That is also why this is the one picker accepting free text.
 */
const filtersOffer: Offer = {
  load: async (element, query, fetchJson) => {
    const sourceTypes = await loadTypes(fetchJson);
    const descriptors = await loadDescriptors(
      element,
      XWiki.contextPath,
      fetchJson,
    );
    const filterable = descriptors.filter((descriptor) =>
      canFilter(descriptor, sourceTypes),
    );
    const constraint = splitTyped(query);
    return constraint === null
      ? toFieldOptions(filterable, query)
      : loadValueOptions(filterable, constraint, fetchJson);
  },
  resolve: async (element, value, fetchJson) => {
    const sourceTypes = await loadTypes(fetchJson);
    const descriptors = await loadDescriptors(
      element,
      XWiki.contextPath,
      fetchJson,
    );
    return [
      withProblem(
        resolveFilterOption(descriptors, value),
        filterProblem(descriptors, sourceTypes, value, dataTypeName(element)),
      ),
    ];
  },
  settings: {
    // One item is one constraint, and the parameter separates them the way a query string does. The displayer
    // renders a text input rather than a multiple select so that this delimiter is what builds the stored value:
    // the macro editor would join the options of a select with a comma.
    delimiter: FILTER_SEPARATOR,
    // Most properties have no value suggester, so a value has to be typeable, and it is encoded like a suggested one.
    create: createFilterOption,
    // Filters apply together, so unlike columns and sort criteria their order carries no meaning.
    plugins: ["remove_button"],
    // Picking a field is picking half a constraint. Rather than leaving `status=` behind as an item that filters
    // on the empty value, put it back in the text box: the author carries on with the value, and typing the
    // separator is what brings up that field's values.
    //
    // A complete constraint, on the other hand, must leave an empty text box behind. Tom Select only clears what
    // was typed when a typed value is created, not when a suggested one is picked, and the text left over from
    // picking `status=published` would be read as the start of the next constraint.
    //
    // A typed constraint is resolved like a stored one, so that a value its field cannot take, or a field that cannot
    // be filtered on, is pointed at as soon as it is added.
    onItemAdd(value) {
      if (isIncomplete(value)) {
        this.removeItem(value, true);
        this.setTextboxValue(value);
      } else {
        this.setTextboxValue("");
        resolveAgain(this, value);
      }
      this.refreshOptions(true);
    },
  },
};

/**
 * Offers the values of the field a typed constraint names, when the field suggests any.
 *
 * @param descriptors - the fields that can be filtered on
 * @param constraint - what the author typed, split into a field and a value
 * @param fetchJson - fetches and parses the suggestions
 * @returns the constraints to offer, empty when the field suggests no value
 */
async function loadValueOptions(
  descriptors: PropertyDescriptor[],
  constraint: { field: string; value: string },
  fetchJson: JsonFetcher,
): Promise<FieldOption[]> {
  const descriptor = descriptors.find(
    (candidate) => candidate.id === constraint.field,
  );
  const searchURL = descriptor?.filter?.searchURL;
  if (descriptor === undefined || searchURL === undefined) {
    return [];
  }
  return toValueOptions(
    await loadValues(searchURL, constraint.value, fetchJson),
    descriptor,
    constraint.value,
  );
}

/**
 * Loads the values a field suggests for what the author typed after the value separator.
 *
 * A failure yields no suggestion rather than an error: the fields did load, and a value can always be typed, so the
 * author is left as with a field that has no suggester rather than told that the fields could not be loaded.
 *
 * @param searchURL - the field's value suggestion URL, as its descriptor reports it
 * @param query - the typed value
 * @param fetchJson - fetches and parses the suggestions
 * @returns the suggested values, empty when they could not be loaded
 */
async function loadValues(
  searchURL: string,
  query: string,
  fetchJson: JsonFetcher,
): Promise<ReturnType<typeof asValues>> {
  try {
    return asValues(
      await fetchJson(valuesUrl(searchURL, query, window.location.href)),
    );
  } catch {
    return [];
  }
}

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
 * Enhances the three pickers of one dialog.
 *
 * They differ only in what they offer, which is what {@link Offer} carries: the settings they share — the widget,
 * the failure handling, the placeholder behaviour — are built once in {@link createSettings}.
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
  $(picker).xwikiSelectize(createSettings(picker, fetchJson));
  const others: [string, Offer][] = [
    [SORT_SELECTOR, sortOffer],
    [FILTERS_SELECTOR, filtersOffer],
  ];
  others.forEach(([selector, offer]) => {
    scope.querySelectorAll(selector).forEach((element) => {
      $(element).xwikiSelectize(createSettings(element, fetchJson, offer));
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

// An empty dependency list and a factory returning the value is the typed form of a plain value module.
define(TRANSLATION_KEYS_MODULE, [], () => ({
  prefix: TRANSLATION_PREFIX,
  keys: [
    "picker.group.fields",
    "picker.group.metadata",
    "picker.sort.ascending",
    "picker.sort.descending",
    "picker.sort.default",
    "picker.resetWarning",
    "picker.loadFailed",
    "picker.retry",
    "picker.dataTypeMissing",
    "picker.problem.fieldMissing",
    "picker.problem.notSortable",
    "picker.problem.notFilterable",
    "picker.problem.valueDoesNotFit",
    "picker.problem.unreadable",
  ],
}));

const [$, , translations] = await loadById<[JQueryStatic, unknown, Messages]>(
  "jquery",
  "xwiki-selectize",
  `xwiki-l10n!${TRANSLATION_KEYS_MODULE}`,
);
messages = translations;
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
