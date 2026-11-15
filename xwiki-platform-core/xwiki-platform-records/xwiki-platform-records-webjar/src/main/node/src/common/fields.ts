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
 * The field picker of the Records macro dialog.
 *
 * The candidate columns of a Records table are the fields of the data type the author picked in the sibling
 * `class` parameter, plus the page metadata. The macro editor has no declarative way to express that dependency:
 * it builds every parameter widget by handing a displayer its own type, default value and attributes, and nothing
 * else, and it caches the resulting templates per macro id, so a template can never be rendered against the
 * current values.
 *
 * What makes an assisted widget possible anyway is that a suggester's data source is a callback invoked on each
 * keystroke rather than a URL fixed at render time. {@link findDataType} therefore reads the `class` input when the
 * author opens the dropdown, not when the template was rendered.
 *
 * That coupling is to the input *named after the parameter*. It is worth being explicit about why that is
 * acceptable: it is the same contract the macro editor itself relies on, since it assembles a macro call by
 * scraping the modal's inputs and matching their names against parameter ids. It cannot be renamed without
 * breaking every macro. This module deliberately does not reach for anything else in the editor's DOM.
 */

/**
 * The part of a Live Data property descriptor this picker needs.
 *
 * @see https://www.xwiki.org/xwiki/bin/view/Documentation/UserGuide/Features/LiveData/
 */
interface PropertyDescriptor {
  /**
   * The property identifier, which is what the `properties` macro parameter holds.
   */
  id: string;
  /**
   * The human readable name, already translated by the property store.
   */
  name?: string;
  /**
   * The property type, shown as a hint because two fields with the same name do not necessarily filter the same
   * way.
   */
  type?: string;
  /**
   * How the property is filtered, when the source says so.
   */
  filter?: FilterDescriptor;
}

/**
 * How a property is filtered.
 */
interface FilterDescriptor {
  /**
   * The filter, such as `text`, `number` or `boolean`. Absent when the property leaves it to its type.
   */
  id?: string;
  /**
   * The URL suggesting the property's values, with an `{encodedQuery}` placeholder. Reported for the properties
   * whose values are enumerable, and used by the filter picker exactly as the Live Data filter row uses it.
   */
  searchURL?: string;
  /**
   * The value a `boolean` filter matches true with.
   */
  trueValue?: unknown;
  /**
   * The value a `boolean` filter matches false with.
   */
  falseValue?: unknown;
}

/**
 * One entry of the picker's dropdown.
 */
interface FieldOption {
  /**
   * The value stored in the macro parameter.
   */
  value: string;
  /**
   * The label shown to the author.
   */
  label: string;
  /**
   * The hint shown next to the label, if the property declares a type.
   */
  hint?: string;
  /**
   * The group this option is listed under.
   */
  optgroup: string;
  /**
   * Why the stored value this option shows back is ignored when the table is displayed, already translated.
   */
  problem?: string;
}

/**
 * The identifier prefix of the page metadata properties, as opposed to the data type's own fields.
 */
const METADATA_PREFIX = "doc.";

/**
 * The page metadata offered as columns.
 *
 * They are the page columns the App Within Minutes wizard offers for its live table, so that an author meets the same
 * choices in both places. The `liveTable` source reports more, such as `doc.fullName` or
 * `doc.enforceRequiredRights`, but those repeat another column or describe how the page is stored rather than the
 * entry it holds.
 */
const METADATA_COLUMNS: string[] = [
  "doc.title",
  "doc.name",
  "doc.location",
  "doc.author",
  "doc.creator",
  "doc.date",
  "doc.creationDate",
];

/**
 * The identifier prefix of the Live Data pseudo-columns.
 *
 * The properties resource reports `_actions`, `_avatar`, `_images` and `_attachments` alongside the real ones.
 * They are rendering affordances rather than data, they carry no type, and offering them as columns would put
 * columns in the table that the data type does not have.
 */
const INTERNAL_PREFIX = "_";

/**
 * The group the page metadata properties are listed under.
 */
const METADATA_GROUP = "metadata";

/**
 * The group the data type's own fields are listed under.
 */
const FIELDS_GROUP = "fields";

/**
 * The name of the macro parameter holding the data type.
 */
const DATA_TYPE_PARAMETER = "class";

/**
 * Reads the data type the author currently has selected.
 *
 * The lookup is scoped to the enclosing form when there is one, so that two macro dialogs open at once cannot read
 * each other's value.
 *
 * @param element - the picker element, used as the starting point of the lookup
 * @returns the serialized data type reference, or `null` when the author has not picked one yet
 */
function findDataType(element: Element): string | null {
  const scope: ParentNode = element.closest("form") ?? element.ownerDocument;
  const field = scope.querySelector(`[name="${DATA_TYPE_PARAMETER}"]`);
  if (
    !(field instanceof HTMLInputElement || field instanceof HTMLSelectElement)
  ) {
    return null;
  }
  const value = field.value.trim();
  return value === "" ? null : value;
}

/**
 * Builds the URL of the Live Data properties resource for a data type.
 *
 * No new backend is needed for this picker: the resource already reports every property of an XClass with its
 * translated name, its type and its sortable and filterable flags. The `sourceParams.` prefix is how the Live Data
 * REST resources pass parameters through to their source.
 *
 * @param contextPath - the wiki context path, as `XWiki.contextPath` gives it
 * @param dataType - the serialized reference of the data type
 * @returns the URL to fetch the property descriptors from
 */
function propertiesUrl(contextPath: string, dataType: string): string {
  const parameters = new URLSearchParams({
    "sourceParams.className": dataType,
    media: "json",
  });
  return `${contextPath}/rest/liveData/sources/liveTable/properties?${parameters.toString()}`;
}

/**
 * Turns property descriptors into dropdown options.
 *
 * The two groups matter: the page metadata and the data type's own fields are different things, and authors look
 * for them separately.
 *
 * @param descriptors - the property descriptors reported by the Live Data properties resource
 * @param query - the text the author typed, matched against both the label and the identifier
 * @returns the options to offer, metadata last so that the data type's own fields come first
 */
function toOptions(
  descriptors: PropertyDescriptor[],
  query = "",
): FieldOption[] {
  return descriptors
    .filter(isOffered)
    .map((descriptor) => ({
      value: descriptor.id,
      label: descriptor.name ?? descriptor.id,
      hint: descriptor.type,
      optgroup: groupOf(descriptor),
    }))
    .filter((option) => matches(option, query));
}

/**
 * Fetches a JSON document.
 */
type JsonFetcher = (url: string) => Promise<unknown>;

/**
 * Loads the candidate columns of the data type currently selected.
 *
 * Returns an empty list rather than throwing when no data type is selected: that is not an error, it is the state
 * the dialog opens in, and the widget says so in place.
 *
 * @param element - the picker element
 * @param query - the text the author typed
 * @param contextPath - the wiki context path
 * @param fetchJson - fetches and parses the properties resource
 * @param metadataLabels - the translated labels of the page metadata, by identifier
 * @returns the options to offer, empty when there is no data type or the resource reports none
 */
async function loadOptions(
  element: Element,
  query: string,
  contextPath: string,
  fetchJson: JsonFetcher,
  metadataLabels: Record<string, string> = {},
): Promise<FieldOption[]> {
  return toOptions(
    await loadDescriptors(element, contextPath, fetchJson, metadataLabels),
    query,
  );
}

/**
 * Loads the property descriptors of the data type currently selected.
 *
 * This is the step every picker of this dialog shares: which data type is selected is read from the sibling
 * parameter, and the answer is the same list of descriptors whether the picker turns them into columns, into sort
 * criteria or into filter constraints.
 *
 * @param element - the picker element
 * @param contextPath - the wiki context path
 * @param fetchJson - fetches and parses the properties resource
 * @param metadataLabels - the translated labels of the page metadata, by identifier
 * @returns the descriptors of the selected data type, empty when no data type is selected
 */
async function loadDescriptors(
  element: Element,
  contextPath: string,
  fetchJson: JsonFetcher,
  metadataLabels: Record<string, string> = {},
): Promise<PropertyDescriptor[]> {
  const dataType = findDataType(element);
  if (dataType === null) {
    return [];
  }
  return withMetadataLabels(
    asDescriptors(await fetchJson(propertiesUrl(contextPath, dataType))),
    metadataLabels,
  );
}

/**
 * Names the page metadata, which the properties resource reports without a name.
 *
 * @param descriptors - the property descriptors reported by the resource
 * @param metadataLabels - the translated labels of the page metadata, by identifier
 * @returns the descriptors, the page metadata named after the labels given
 */
function withMetadataLabels(
  descriptors: PropertyDescriptor[],
  metadataLabels: Record<string, string>,
): PropertyDescriptor[] {
  return descriptors.map((descriptor) =>
    Object.hasOwn(metadataLabels, descriptor.id)
      ? { ...descriptor, name: metadataLabels[descriptor.id] }
      : descriptor,
  );
}

/**
 * Whether a property descriptor is offered to the author, in any of the pickers.
 *
 * Only the page metadata of {@link METADATA_COLUMNS} is offered. The other fields the resource reports stay
 * candidates, so that a value naming one of them, stored before or typed by hand, is still shown back and checked.
 *
 * @param descriptor - the descriptor to check
 * @returns whether the descriptor is a candidate the pickers offer
 */
function isOffered(descriptor: PropertyDescriptor): boolean {
  return (
    isCandidate(descriptor) &&
    (groupOf(descriptor) !== METADATA_GROUP ||
      METADATA_COLUMNS.includes(descriptor.id))
  );
}

/**
 * Whether a property descriptor names something an author can pick.
 *
 * @param descriptor - the descriptor to check
 * @returns false for the Live Data pseudo-columns and for anything without an identifier
 */
function isCandidate(descriptor: PropertyDescriptor): boolean {
  return (
    typeof descriptor?.id === "string" &&
    descriptor.id !== "" &&
    !descriptor.id.startsWith(INTERNAL_PREFIX)
  );
}

/**
 * Whether the properties resource reported any field of the data type itself.
 *
 * The resource always reports the page metadata, and adds the data type's fields only when the class exists and the
 * current user may view it. A list without any field is therefore how a deleted or unreachable data type shows, and
 * the two cannot be told apart, which suits a picker that must not reveal that something exists but is out of reach.
 * A class that exists with no field at all reads the same way, but it cannot fill a table either.
 *
 * @param descriptors - the property descriptors reported by the resource
 * @returns whether at least one of them is a field of the data type
 */
function hasDataTypeFields(descriptors: PropertyDescriptor[]): boolean {
  return descriptors.some(
    (descriptor) =>
      isCandidate(descriptor) && groupOf(descriptor) === FIELDS_GROUP,
  );
}

/**
 * @param descriptor - a property descriptor
 * @returns the group it is listed under
 */
function groupOf(descriptor: PropertyDescriptor): string {
  return descriptor.id.startsWith(METADATA_PREFIX)
    ? METADATA_GROUP
    : FIELDS_GROUP;
}

/**
 * @param option - an option to test
 * @param query - the text the author typed
 * @returns whether the option matches, on either what is shown or what is stored
 */
function matches(
  option: { label: string; value: string },
  query: string,
): boolean {
  const normalized = query.trim().toLowerCase();
  return (
    normalized === "" ||
    option.label.toLowerCase().includes(normalized) ||
    option.value.toLowerCase().includes(normalized)
  );
}

/**
 * Reads the property descriptors out of what the REST resource returned.
 *
 * The resource answers with an envelope, `{ links, properties }`, and the descriptors are its `properties` entry.
 * A bare array is accepted too, so that the picker keeps working if the resource is ever simplified. Anything else
 * yields no option rather than an exception inside a keystroke handler.
 *
 * @param payload - the parsed response body
 * @returns the property descriptors it holds, possibly none
 */
function asDescriptors(payload: unknown): PropertyDescriptor[] {
  let candidates: unknown[];
  if (Array.isArray(payload)) {
    candidates = payload;
  } else if (
    typeof payload === "object" &&
    payload !== null &&
    Array.isArray((payload as { properties?: unknown }).properties)
  ) {
    candidates = (payload as { properties: unknown[] }).properties;
  } else {
    return [];
  }
  return candidates.filter(
    (item): item is PropertyDescriptor =>
      typeof item === "object" && item !== null && "id" in item,
  );
}

export {
  DATA_TYPE_PARAMETER,
  FIELDS_GROUP,
  INTERNAL_PREFIX,
  METADATA_COLUMNS,
  METADATA_GROUP,
  METADATA_PREFIX,
  asDescriptors,
  findDataType,
  groupOf,
  hasDataTypeFields,
  isCandidate,
  isOffered,
  loadDescriptors,
  loadOptions,
  matches,
  propertiesUrl,
  toOptions,
  withMetadataLabels,
};
export type { FieldOption, FilterDescriptor, JsonFetcher, PropertyDescriptor };
