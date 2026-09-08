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
 * `class` parameter, plus the entry metadata. The macro editor has no declarative way to express that dependency:
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
}

/**
 * The identifier prefix of the entry metadata properties, as opposed to the data type's own fields.
 */
const METADATA_PREFIX = "doc.";

/**
 * The identifier prefix of the Live Data pseudo-columns.
 *
 * The properties resource reports `_actions`, `_avatar`, `_images` and `_attachments` alongside the real ones.
 * They are rendering affordances rather than data, they carry no type, and offering them as columns would put
 * columns in the table that the data type does not have.
 */
const INTERNAL_PREFIX = "_";

/**
 * The group the entry metadata properties are listed under.
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
 * The two groups matter: the entry metadata and the data type's own fields are different things, and authors look
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
  const normalizedQuery = query.trim().toLowerCase();
  return descriptors
    .filter(
      (descriptor) =>
        typeof descriptor?.id === "string" &&
        descriptor.id !== "" &&
        !descriptor.id.startsWith(INTERNAL_PREFIX),
    )
    .map((descriptor) => ({
      value: descriptor.id,
      label: descriptor.name ?? descriptor.id,
      hint: descriptor.type,
      optgroup: descriptor.id.startsWith(METADATA_PREFIX)
        ? METADATA_GROUP
        : FIELDS_GROUP,
    }))
    .filter(
      (option) =>
        normalizedQuery === "" ||
        option.label.toLowerCase().includes(normalizedQuery) ||
        option.value.toLowerCase().includes(normalizedQuery),
    );
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
 * @returns the options to offer, empty when there is no data type or the resource reports none
 */
async function loadOptions(
  element: Element,
  query: string,
  contextPath: string,
  fetchJson: JsonFetcher,
): Promise<FieldOption[]> {
  const dataType = findDataType(element);
  if (dataType === null) {
    return [];
  }
  const payload = await fetchJson(propertiesUrl(contextPath, dataType));
  return toOptions(asDescriptors(payload), query);
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
  METADATA_GROUP,
  METADATA_PREFIX,
  asDescriptors,
  findDataType,
  loadOptions,
  propertiesUrl,
  toOptions,
};
export type { FieldOption, JsonFetcher, PropertyDescriptor };
