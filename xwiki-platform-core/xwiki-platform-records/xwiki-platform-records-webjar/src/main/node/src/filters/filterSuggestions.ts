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
import { dataTypeName } from "../common/dialog";
import { canFilter, filterProblem } from "../common/fieldUse";
import { loadDescriptors } from "../common/fields";
import { loadTypes } from "../common/remote";
import { resolveAgain } from "../common/suggester";
import { metadataLabels, withProblem } from "../common/translations";
import type {
  FieldOption,
  JsonFetcher,
  PropertyDescriptor,
} from "../common/fields";
import type { Suggester, Suggestions } from "../common/suggester";

/**
 * The filters picker: one item per `field=value` constraint.
 *
 * The suggestions come in two steps, because a constraint has two halves and only the author knows the first one.
 * Until a value separator is typed the dropdown offers the fields; once one is typed, it offers that field's
 * values when the source reports a way to suggest them, and otherwise stays out of the way so the author can type
 * a value freely. That is also why this is the one picker accepting free text.
 */
const filterSuggestions: Suggestions = {
  load: async (element, query, fetchJson) => {
    const sourceTypes = await loadTypes(fetchJson);
    const descriptors = await loadDescriptors(
      element,
      XWiki.contextPath,
      fetchJson,
      metadataLabels(),
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
      metadataLabels(),
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
    create(input, callback) {
      const option = createFilterOption(input);
      callback(option ?? undefined);
      return option !== null;
    },
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
    onItemAdd(this: Suggester, value) {
      const constraint = String(value);
      if (isIncomplete(constraint)) {
        this.removeItem(constraint, true);
        this.setTextboxValue(constraint);
      } else {
        this.setTextboxValue("");
        resolveAgain(this, constraint);
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
): Promise<string[]> {
  try {
    return asValues(
      await fetchJson(valuesUrl(searchURL, query, window.location.href)),
    );
  } catch {
    return [];
  }
}

export { filterSuggestions, loadValues };
