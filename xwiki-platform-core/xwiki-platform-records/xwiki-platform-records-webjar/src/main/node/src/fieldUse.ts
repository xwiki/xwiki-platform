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
 * What a table can do with a field of the data type: sort on it, filter on it, and with which values.
 *
 * The macro decides this when it renders the table, and ignores, with a warning, the sort criteria and filters a
 * field does not allow. The dialog applies the same rules, so that it does not offer what the table would ignore,
 * and points at what it would ignore in a stored configuration.
 *
 * The answer comes from the type of the field rather than from its descriptor. The properties resource reports a
 * `sortable` and a `filterable` flag on every descriptor, but it reports `false` for a flag the source left unset,
 * which is most of them: the source leaves them to the defaults of the field's type, which the types resource
 * reports. A flag a source sets on one field only, such as a multiple selection list that cannot be sorted, is
 * therefore not seen here, and the table still warns about it when it is rendered.
 */

import { hasDataTypeFields } from "./fieldPicker";
import type { FilterDescriptor, PropertyDescriptor } from "./fieldPicker";

/**
 * The defaults of one property type, as the Live Data types resource reports them.
 */
interface PropertyType {
  /**
   * The type, which is what a property descriptor's `type` names.
   */
  id: string;
  sortable?: boolean;
  filterable?: boolean;
  filter?: FilterDescriptor;
}

/**
 * Why a stored sort criterion or filter is ignored when the table is displayed.
 */
interface Problem {
  /**
   * The translation key of the message, without the prefix.
   */
  key: string;
  /**
   * The values of the message parameters.
   */
  args: string[];
}

/**
 * The identifier prefix of the Live Data pseudo-columns, which the macro passes through without checking them.
 */
const INTERNAL_PREFIX = "_";

/**
 * What a number is, for the `liveTable` source: it parses the value of a number filter, and matches no entry at all
 * with a value that is not one.
 */
const NUMBER = /^[+-]?(\d+\.?\d*|\.\d+)([eE][+-]?\d+)?$/;

/**
 * Builds the URL of the Live Data types resource of the `liveTable` source.
 *
 * @param contextPath - the wiki context path, as `XWiki.contextPath` gives it
 * @returns the URL to fetch the property types from
 */
function typesUrl(contextPath: string): string {
  return `${contextPath}/rest/liveData/sources/liveTable/types?media=json`;
}

/**
 * Reads the property types out of what the types resource returned, `{ links, types }`.
 *
 * @param payload - the parsed response body
 * @returns the property types it holds, possibly none
 */
function asTypes(payload: unknown): PropertyType[] {
  const candidates = (payload as { types?: unknown })?.types;
  if (!Array.isArray(candidates)) {
    return [];
  }
  return candidates.filter(
    (item): item is PropertyType =>
      typeof (item as { id?: unknown })?.id === "string",
  );
}

/**
 * @param descriptor - a property descriptor
 * @param types - the property types of the source
 * @returns the defaults of the descriptor's type, if the source has any
 */
function typeOf(
  descriptor: PropertyDescriptor,
  types: PropertyType[],
): PropertyType | undefined {
  return types.find((type) => type.id === descriptor.type);
}

/**
 * @param descriptor - a property descriptor
 * @param types - the property types of the source
 * @returns whether the table can be sorted on the property
 */
function canSort(
  descriptor: PropertyDescriptor,
  types: PropertyType[],
): boolean {
  return typeOf(descriptor, types)?.sortable === true;
}

/**
 * @param descriptor - a property descriptor
 * @param types - the property types of the source
 * @returns whether the table can be filtered on the property
 */
function canFilter(
  descriptor: PropertyDescriptor,
  types: PropertyType[],
): boolean {
  return typeOf(descriptor, types)?.filterable === true;
}

/**
 * Tells whether the `liveTable` source can match a property with a filter value. It parses the value of a number
 * filter, and compares that of a boolean filter with the filter's own true and false values, which are `1` and `0`
 * for a field and `true` and `false` for the page metadata. An empty value always fits, since it filters nothing.
 *
 * @param descriptor - a property descriptor
 * @param types - the property types of the source
 * @param value - the decoded filter value
 * @returns whether the value fits the property
 */
function fits(
  descriptor: PropertyDescriptor,
  types: PropertyType[],
  value: string,
): boolean {
  const filter = descriptor.filter?.id
    ? descriptor.filter
    : typeOf(descriptor, types)?.filter;
  const trimmed = value.trim();
  if (trimmed === "") {
    return true;
  }
  if (filter?.id === "number") {
    return NUMBER.test(trimmed);
  }
  if (filter?.id === "boolean") {
    return [filter.trueValue, filter.falseValue]
      .filter((accepted) => accepted !== undefined && accepted !== null)
      .map(String)
      .includes(trimmed);
  }
  return true;
}

/**
 * @param descriptors - the property descriptors of the data type
 * @param field - a field identifier
 * @returns the descriptor of the field, the pseudo-columns included
 */
function findField(
  descriptors: PropertyDescriptor[],
  field: string,
): PropertyDescriptor | undefined {
  return descriptors.find((descriptor) => descriptor.id === field);
}

/**
 * @param descriptor - a property descriptor
 * @returns the name the author knows the property under
 */
function nameOf(descriptor: PropertyDescriptor): string {
  return descriptor.name ?? descriptor.id;
}

/**
 * Tells why the field of a stored criterion or constraint makes the table ignore it, if it does.
 *
 * Nothing is said when the resource reports no field of the data type: the data type is then gone or out of the
 * author's reach, which the dialog reports on its own, and the stored configuration cannot be checked.
 *
 * @param descriptors - the property descriptors of the data type
 * @param field - the field identifier
 * @param dataType - the name of the data type, as the author knows it
 * @param usable - whether the table can use the field for what the item does
 * @param unusableKey - the translation key saying it cannot
 * @returns the problem, or null when there is none
 */
function fieldProblem(
  descriptors: PropertyDescriptor[],
  field: string,
  dataType: string,
  usable: (descriptor: PropertyDescriptor) => boolean,
  unusableKey: string,
): Problem | null {
  if (field === "" || !hasDataTypeFields(descriptors)) {
    return null;
  }
  const descriptor = findField(descriptors, field);
  if (descriptor === undefined) {
    // The macro passes a pseudo-column it does not know through as it is.
    return field.startsWith(INTERNAL_PREFIX)
      ? null
      : { key: "picker.problem.fieldMissing", args: [field, dataType] };
  }
  return usable(descriptor)
    ? null
    : { key: unusableKey, args: [nameOf(descriptor)] };
}

/**
 * Tells why a stored sort criterion is ignored when the table is displayed, if it is.
 *
 * @param descriptors - the property descriptors of the data type
 * @param types - the property types of the source
 * @param field - the field the criterion sorts on
 * @param dataType - the name of the data type, as the author knows it
 * @returns the problem, or null when there is none
 */
function sortProblem(
  descriptors: PropertyDescriptor[],
  types: PropertyType[],
  field: string,
  dataType: string,
): Problem | null {
  return fieldProblem(
    descriptors,
    field.trim(),
    dataType,
    (descriptor) => canSort(descriptor, types),
    "picker.problem.notSortable",
  );
}

/**
 * Reads a stored constraint as the renderer does: as form data, so `+` is a space, with both halves decoded and
 * trimmed.
 *
 * @param constraint - one constraint, as stored in the parameter
 * @returns its field and value, or null when the renderer cannot decode it
 */
function readConstraint(
  constraint: string,
): { field: string; value: string } | null {
  const separator = constraint.indexOf("=");
  const [field, value] =
    separator === -1
      ? [constraint, ""]
      : [constraint.slice(0, separator), constraint.slice(separator + 1)];
  try {
    return {
      field: decodeURIComponent(field.replaceAll("+", " ")).trim(),
      value: decodeURIComponent(value.replaceAll("+", " ")).trim(),
    };
  } catch {
    return null;
  }
}

/**
 * Tells why a stored filter constraint is ignored when the table is displayed, if it is.
 *
 * One the renderer cannot decode at all is ignored as a whole.
 *
 * @param descriptors - the property descriptors of the data type
 * @param types - the property types of the source
 * @param constraint - one constraint, as stored in the parameter
 * @param dataType - the name of the data type, as the author knows it
 * @returns the problem, or null when there is none
 */
function filterProblem(
  descriptors: PropertyDescriptor[],
  types: PropertyType[],
  constraint: string,
  dataType: string,
): Problem | null {
  const read = readConstraint(constraint);
  if (read === null) {
    return { key: "picker.problem.unreadable", args: [constraint] };
  }
  const { field, value } = read;
  const problem = fieldProblem(
    descriptors,
    field,
    dataType,
    (descriptor) => canFilter(descriptor, types),
    "picker.problem.notFilterable",
  );
  if (problem !== null) {
    return problem;
  }
  const descriptor = findField(descriptors, field);
  return descriptor === undefined || fits(descriptor, types, value)
    ? null
    : {
        key: "picker.problem.valueDoesNotFit",
        args: [nameOf(descriptor), value],
      };
}

export {
  asTypes,
  canFilter,
  canSort,
  filterProblem,
  fits,
  sortProblem,
  typesUrl,
};
export type { Problem, PropertyType };
