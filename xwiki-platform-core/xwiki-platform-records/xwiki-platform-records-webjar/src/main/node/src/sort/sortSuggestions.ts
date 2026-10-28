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
import { fieldOf, resolveSortOption, toSortOptions } from "./sortPicker";
import { dataTypeName } from "../common/dialog";
import { canSort, sortProblem } from "../common/fieldUse";
import { loadDescriptors } from "../common/fields";
import { loadTypes } from "../common/remote";
import { reload } from "../common/suggester";
import { metadataLabels, translate, withProblem } from "../common/translations";
import type { DirectionLabels } from "./sortPicker";
import type { Suggester, Suggestions } from "../common/suggester";

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
 * The sort picker: two options per field, one per direction, minus the fields already sorted on, since sorting a
 * field twice adds nothing. A stored criterion is resolved exactly rather than searched, for the reason given on
 * {@link resolveSortOption}.
 */
const sortSuggestions: Suggestions = {
  load: async (element, query, fetchJson, selected) => {
    const sourceTypes = await loadTypes(fetchJson);
    return toSortOptions(
      await loadDescriptors(
        element,
        XWiki.contextPath,
        fetchJson,
        metadataLabels(),
      ),
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
      metadataLabels(),
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
    onItemAdd(this: Suggester) {
      reload(this);
    },
    onItemRemove(this: Suggester) {
      reload(this);
    },
  },
};

export { directionLabels, sortSuggestions };
