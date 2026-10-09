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
 * The filter picker of the Records macro dialog.
 *
 * The `filters` parameter is a query string, `status=Active&client=Acme`, which the renderer parses by splitting on
 * `&`, then on the first `=`, and URL-decoding both halves; a field named twice becomes two constraints on that
 * field. So one filter is one `field=value` pair, which is what makes a multiple-value suggester with `&` as its
 * delimiter the natural widget: each selected item is one constraint.
 *
 * Two consequences of that encoding drive this module. Every value is percent-encoded, whether suggested or typed,
 * because the renderer decodes it: a value holding `&` or `=` would otherwise be read as another constraint, a `%`
 * would make the whole table fail to load, and a `+` would turn into a space. And the same field may
 * legitimately appear more than once, which works because an item's value is the whole pair rather than the field.
 *
 * The candidate values come from the property descriptor itself: the Live Data properties resource reports a
 * `filter.searchURL` for the properties that have one, with an `{encodedQuery}` placeholder. That is the very URL
 * the Live Data filter row uses to suggest values, so this picker suggests exactly what a reader would be offered
 * in the rendered table.
 */

import {
  FIELDS_GROUP,
  groupOf,
  isCandidate,
  isOffered,
  matches,
} from "./fieldPicker";
import type { FieldOption, PropertyDescriptor } from "./fieldPicker";

/**
 * Separates a field from its value in one constraint.
 */
const VALUE_SEPARATOR = "=";

/**
 * Separates the constraints in the parameter, and therefore the widget's delimiter.
 */
const FILTER_SEPARATOR = "&";

/**
 * The placeholder a `filter.searchURL` carries where the typed text goes.
 */
const QUERY_PLACEHOLDER = "{encodedQuery}";

/**
 * One constraint, split into its two halves.
 */
interface Constraint {
  /**
   * The field identifier, as authored.
   */
  field: string;
  /**
   * The value, decoded.
   */
  value: string;
}

/**
 * Splits what the author typed into a field and a value, taken literally.
 *
 * Typed text is not encoded: `completion=100%` means a value of `100%`, and `language=C++` a value of `C++`.
 *
 * @param text - the text the author is typing
 * @returns the two halves, or null when no value separator has been typed yet
 */
function splitTyped(text: string): Constraint | null {
  const separator = text.indexOf(VALUE_SEPARATOR);
  if (separator === -1) {
    return null;
  }
  return {
    field: text.slice(0, separator),
    value: text.slice(separator + 1),
  };
}

/**
 * Splits a stored constraint into its field and its decoded value.
 *
 * @param text - one constraint, as stored in the parameter
 * @returns the two halves, or null when the constraint has no value separator
 */
function splitConstraint(text: string): Constraint | null {
  const constraint = splitTyped(text);
  return constraint === null
    ? null
    : { field: constraint.field, value: decode(constraint.value) };
}

/**
 * Decodes the value half of a stored constraint the way the renderer does, tolerating one that is not valid
 * percent-encoding.
 *
 * The renderer decodes the parameter as form data, where `+` stands for a space, so it is read as one here too:
 * otherwise the dialog would show a hand-written `a+b` as something other than what the table filters on. A value
 * holding a bare `%`, which {@link decodeURIComponent} refuses, is shown as it is rather than failing inside a
 * keystroke handler; the macro reports it when the table is rendered.
 *
 * @param value - the value as it appears in the parameter
 * @returns the decoded value, or the value unchanged when it cannot be decoded
 */
function decode(value: string): string {
  const spaced = value.replaceAll("+", " ");
  try {
    return decodeURIComponent(spaced);
  } catch {
    return spaced;
  }
}

/**
 * Builds one constraint, encoded as the parameter needs it.
 *
 * @param field - the field identifier
 * @param value - the value, decoded
 * @returns the constraint to store
 */
function encodeConstraint(field: string, value: string): string {
  return `${field}${VALUE_SEPARATOR}${encodeURIComponent(value)}`;
}

/**
 * Turns what the author typed into a constraint, which is how a value no resource suggests gets in.
 *
 * The value is encoded exactly as a suggested one is, since the renderer decodes every constraint: left as typed, a
 * `%` would make the whole table fail to load and a `+` would turn into a space. The label shows the text as typed.
 *
 * @param input - the text the author typed
 * @returns the option to create, or null when the text names no field and value, since it would not be a
 *   constraint
 */
function createFilterOption(input: string): FieldOption | null {
  const constraint = splitTyped(input);
  if (constraint === null || constraint.field.trim() === "") {
    return null;
  }
  const field = constraint.field.trim();
  return {
    value: encodeConstraint(field, constraint.value),
    label: `${field} ${VALUE_SEPARATOR} ${constraint.value}`,
    optgroup: FIELDS_GROUP,
  };
}

/**
 * Offers the fields that can be filtered on, as constraints waiting for a value.
 *
 * The value of each is `field=`, which is not a usable constraint on its own; the dialog turns picking one back
 * into typing, so that the author continues with the value and the value suggestions appear.
 *
 * @param descriptors - the property descriptors of the data type
 * @param query - the text the author typed
 * @returns one option per matching field
 */
function toFieldOptions(
  descriptors: PropertyDescriptor[],
  query = "",
): FieldOption[] {
  return descriptors
    .filter(isOffered)
    .map((descriptor) => ({
      value: `${descriptor.id}${VALUE_SEPARATOR}`,
      label: `${descriptor.name ?? descriptor.id} ${VALUE_SEPARATOR}`,
      hint: descriptor.type,
      optgroup: groupOf(descriptor),
    }))
    .filter((option) => matches(option, query));
}

/**
 * Whether an option is a field waiting for its value rather than a usable constraint.
 *
 * @param value - an option value
 * @returns true when the value names a field but no value
 */
function isIncomplete(value: string): boolean {
  return value.endsWith(VALUE_SEPARATOR);
}

/**
 * Builds the URL suggesting the values of one field.
 *
 * @param searchURL - the `filter.searchURL` of the property descriptor, holding the query placeholder
 * @param query - the text the author typed after the value separator
 * @param base - the URL relative ones are resolved against, since a `searchURL` may be a bare query string
 * @returns the URL to fetch the candidate values from
 */
function valuesUrl(searchURL: string, query: string, base: string): string {
  const url = searchURL
    .split(QUERY_PLACEHOLDER)
    .join(encodeURIComponent(query));
  return new URL(url, base).toString();
}

/**
 * Reads the candidate values out of what a value resource returned.
 *
 * Two shapes are accepted, because the descriptors point at two different resources: the class property values
 * resource answers `{propertyValues: [{value}]}`, while the user and group suggester answers a bare array. Anything
 * else yields no value rather than an exception inside a keystroke handler.
 *
 * @param payload - the parsed response body
 * @returns the values it holds, possibly none
 */
function asValues(payload: unknown): string[] {
  const candidates = Array.isArray(payload)
    ? payload
    : ((payload as { propertyValues?: unknown })?.propertyValues ?? []);
  if (!Array.isArray(candidates)) {
    return [];
  }
  return candidates
    .map((candidate) => {
      if (typeof candidate === "string") {
        return candidate;
      }
      const value = (candidate as { value?: unknown; id?: unknown })?.value;
      const id = (candidate as { id?: unknown })?.id;
      return typeof value === "string"
        ? value
        : typeof id === "string"
          ? id
          : null;
    })
    .filter((value): value is string => value !== null && value !== "");
}

/**
 * Turns the candidate values of a field into constraints.
 *
 * @param values - the candidate values
 * @param descriptor - the field they belong to
 * @param query - the text typed after the value separator, matched against the value
 * @returns one option per matching value
 */
function toValueOptions(
  values: string[],
  descriptor: PropertyDescriptor,
  query = "",
): FieldOption[] {
  const name = descriptor.name ?? descriptor.id;
  return values
    .map((value) => ({
      value: encodeConstraint(descriptor.id, value),
      label: `${name} ${VALUE_SEPARATOR} ${value}`,
      hint: descriptor.type,
      optgroup: groupOf(descriptor),
    }))
    .filter(
      (option) =>
        query.trim() === "" ||
        option.label.toLowerCase().includes(query.trim().toLowerCase()),
    );
}

/**
 * Resolves one stored constraint into the option that shows it back.
 *
 * As for the sort picker, this is not a search: a constraint on a field the data type no longer has, or one whose
 * value no resource suggests, is still what the author wrote, and an option whose value differs from the stored
 * one would lose it when the dialog reopens.
 *
 * @param descriptors - the property descriptors of the data type
 * @param value - one constraint, as stored in the parameter
 * @returns the option showing that constraint
 */
function resolveFilterOption(
  descriptors: PropertyDescriptor[],
  value: string,
): FieldOption {
  const constraint = splitConstraint(value);
  const descriptor = descriptors
    .filter(isCandidate)
    .find((candidate) => candidate.id === constraint?.field);
  if (constraint === null || descriptor === undefined) {
    return { value, label: value, optgroup: "fields" };
  }
  return {
    value,
    label: `${descriptor.name ?? descriptor.id} ${VALUE_SEPARATOR} ${constraint.value}`,
    hint: descriptor.type,
    optgroup: groupOf(descriptor),
  };
}

export {
  FILTER_SEPARATOR,
  QUERY_PLACEHOLDER,
  VALUE_SEPARATOR,
  asValues,
  createFilterOption,
  encodeConstraint,
  isIncomplete,
  resolveFilterOption,
  splitConstraint,
  splitTyped,
  toFieldOptions,
  toValueOptions,
  valuesUrl,
};
export type { Constraint };
