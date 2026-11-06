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
  dataTypeName,
  showMessage,
  showProblems,
} from "./dialog";
import { translate } from "./translations";
import type { FieldOption, JsonFetcher } from "./fields";
import type TomSelect from "tom-select";

/**
 * The suggest widget settings this picker needs: Tom Select's, with the `load` every picker has, and the
 * `loadSelected` the suggest widget adds to them.
 *
 * Tom Select's settings type alone is not enough. `loadSelected` is not one of its settings, yet
 * {@link resolveAgain} reads it back from the widget, and a type alias cannot be augmented from outside, so it is
 * added here. `load` is picked out of the partial settings because every picker sets it, which spares its callers a
 * non-null assertion. `TomSelect["settings"]` names the settings type through the package's main entry point rather
 * than a path inside its `dist` directory.
 */
type PickerSettings = Partial<TomSelect["settings"]> &
  Pick<TomSelect["settings"], "load"> & {
    /**
     * Resolves a stored value into the option that shows it.
     */
    loadSelected: TomSelect["settings"]["load"];
  };

/**
 * A suggest widget built from {@link PickerSettings}.
 */
type Suggester = TomSelect & { settings: PickerSettings };

/**
 * What one picker suggests: the options for a query, and the option that shows a stored value back.
 */
interface Suggestions {
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
 * @param suggestions - what the picker suggests
 * @returns the settings to hand to the suggest widget
 */
function createSettings(
  element: Element,
  fetchJson: JsonFetcher,
  suggestions: Suggestions,
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
      callback: TomSelect["loadCallback"],
    ) {
      const selected = this?.items ?? [];
      void (async () => {
        try {
          callback(await produce(query, selected), []);
          clearMessage(this?.wrapper ?? element, "error");
          if (this) {
            refreshProblems(this);
          }
        } catch {
          callback([], []);
          reportLoadFailure(element, this);
        }
      })();
    };
  return {
    load: guard((query, selected) =>
      suggestions.load(element, query, fetchJson, selected),
    ),
    loadSelected: guard((value) =>
      suggestions.resolve(element, value, fetchJson),
    ),
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
    onFocus(this: Suggester) {
      this.load(this.inputValue());
    },
    ...suggestions.settings,
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
  const dataType = dataTypeName(element);
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
  suggester.settings.loadSelected.call(
    suggester,
    value,
    (options: FieldOption[]) => {
      options.forEach((option) => {
        if (Object.hasOwn(suggester.options, option.value)) {
          suggester.updateOption(option.value, option);
        } else {
          suggester.addOption(option);
        }
      });
    },
  );
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

export { createSettings, reload, resolveAgain, watchProblems };
export type { PickerSettings, Suggester, Suggestions };
