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

/**
 * The sort picker of the Records macro dialog.
 *
 * The `sort` parameter is a comma-separated list of `field` or `field:asc` / `field:desc` criteria, in the order
 * they apply. That is Live Data's own encoding, and it is what makes a suggester the right widget rather than the
 * platform's `sortPicker` (`uicomponents/widgets/sortPicker.js`): that one edits a single criterion through two
 * selects, whereas this parameter holds a list whose order matters, which is exactly what a multiple-value
 * suggester with the drag-and-drop plugin already gives.
 *
 * Each candidate field therefore yields two options rather than one, so that picking a criterion is a single
 * choice and the direction never has to be typed. A field already used is then dropped from what is offered:
 * sorting on the same field twice says nothing the first criterion did not already say, and offering both
 * directions of a field that is already sorted invites reading the pair as a choice when it is not one.
 */

import { groupOf, isCandidate, matches } from "./fieldPicker";
import type { FieldOption, PropertyDescriptor } from "./fieldPicker";

/**
 * Separates a field from its direction in one criterion.
 */
const DIRECTION_SEPARATOR = ":";

/**
 * The directions Live Data understands, with the label each is offered under.
 *
 * English rather than translated for the same reason as the rest of this webjar: a webjar has no access to a
 * translation bundle, and these belong in the macro's bundle once the dialog has a way to reach one.
 */
const DIRECTIONS: readonly { value: string; label: string }[] = [
  { value: "asc", label: "ascending" },
  { value: "desc", label: "descending" },
];

/**
 * How a criterion that names no direction is labelled, since Live Data then applies the source's own order.
 */
const DEFAULT_DIRECTION_LABEL = "default order";

/**
 * @param criterion - one criterion, with or without a direction
 * @returns the field it sorts on
 */
function fieldOf(criterion: string): string {
  const separator = criterion.indexOf(DIRECTION_SEPARATOR);
  return separator === -1 ? criterion : criterion.slice(0, separator);
}

/**
 * Builds the sort criteria offered for a data type.
 *
 * @param descriptors - the property descriptors of the data type
 * @param query - the text the author typed, matched against both the label and the stored value
 * @param selected - the criteria already picked, whose fields are not offered again
 * @returns two options per candidate field still available, ascending first
 */
function toSortOptions(
  descriptors: PropertyDescriptor[],
  query = "",
  selected: readonly string[] = [],
): FieldOption[] {
  const used = new Set(selected.map(fieldOf));
  return descriptors
    .filter(isCandidate)
    .filter((descriptor) => !used.has(descriptor.id))
    .flatMap((descriptor) =>
      DIRECTIONS.map((direction) => ({
        value: `${descriptor.id}${DIRECTION_SEPARATOR}${direction.value}`,
        label: `${descriptor.name ?? descriptor.id} (${direction.label})`,
        hint: descriptor.type,
        optgroup: groupOf(descriptor),
      })),
    )
    .filter((option) => matches(option, query));
}

/**
 * Resolves one stored criterion into the option that shows it back.
 *
 * This is deliberately not {@link toSortOptions} with the value as the query. A saved criterion may name no
 * direction, and it may name a field the data type no longer has; in both cases the suggester is told to refuse
 * free text, so an option whose value is not exactly the stored one would silently drop the author's criterion
 * when the dialog reopens. Resolving the exact value keeps what was authored.
 *
 * @param descriptors - the property descriptors of the data type
 * @param value - one criterion, as stored in the parameter
 * @returns the option showing that criterion
 */
function resolveSortOption(
  descriptors: PropertyDescriptor[],
  value: string,
): FieldOption {
  const separator = value.indexOf(DIRECTION_SEPARATOR);
  const field = fieldOf(value);
  const direction = separator === -1 ? "" : value.slice(separator + 1);
  const descriptor = descriptors
    .filter(isCandidate)
    .find((candidate) => candidate.id === field);
  const directionLabel =
    DIRECTIONS.find((candidate) => candidate.value === direction)?.label ??
    DEFAULT_DIRECTION_LABEL;
  return {
    value,
    // A field the data type does not have is shown as it was authored: the dialog is not the place to decide that
    // a criterion is stale, and dropping it would lose it on the next save.
    label: descriptor
      ? `${descriptor.name ?? descriptor.id} (${directionLabel})`
      : value,
    hint: descriptor?.type,
    optgroup: descriptor ? groupOf(descriptor) : "fields",
  };
}

export {
  DEFAULT_DIRECTION_LABEL,
  DIRECTIONS,
  fieldOf,
  resolveSortOption,
  toSortOptions,
};
