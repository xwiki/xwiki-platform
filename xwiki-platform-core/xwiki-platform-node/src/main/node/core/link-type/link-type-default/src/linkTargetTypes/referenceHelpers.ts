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
import { tryFallible } from "@xwiki/platform-fn-utils";
import { EntityType } from "@xwiki/platform-model-api";
import type { EntityReference } from "@xwiki/platform-model-api";
import type {
  ModelReferenceParserProvider,
  ModelReferenceSerializerProvider,
} from "@xwiki/platform-model-reference-api";
import type { ResourceReference } from "@xwiki/platform-rendering-api";

/**
 * The parts of a link to a page or an attachment that come after the linked entity in its URL, as held by the link
 * target configurations and by the parameters of the resource references (without their "?" and "#" URL separators).
 */
type LinkURLParts = {
  queryString?: string;
  anchor?: string;
};

/**
 * Normalize a query string or an anchor, as typed by the user or read from a link, which may start with its URL
 * separator (e.g. when copied from a URL).
 *
 * @param value - the query string or the anchor
 * @param separator - the URL separator that precedes it ("?" or "#")
 * @returns the value without its URL separator, or `undefined` when it is empty
 */
function withoutURLSeparator(
  value: string | undefined,
  separator: "?" | "#",
): string | undefined {
  const trimmed = value?.trim() ?? "";
  const stripped = trimmed.startsWith(separator) ? trimmed.slice(1) : trimmed;
  return stripped.length > 0 ? stripped : undefined;
}

/**
 * Any base to parse the relative URLs (e.g. "/xwiki/bin/view/Space/Page?key=value"), which the URL constructor rejects
 * without one: only their query string and anchor are read.
 */
const RELATIVE_URL_BASE = "http://relative.base/";

/**
 * @param href - a URL, absolute or relative
 * @returns its query string and its (decoded) anchor, without their URL separators
 */
function linkURLPartsFromURL(href: string): LinkURLParts {
  const url = tryFallible(() => new URL(href, RELATIVE_URL_BASE));
  const anchor = withoutURLSeparator(url?.hash, "#");
  return {
    queryString: withoutURLSeparator(url?.search, "?"),
    // The anchor of a link is the identifier of the target element, which the URL holds percent-encoded.
    anchor:
      anchor === undefined
        ? undefined
        : (tryFallible(() => decodeURIComponent(anchor)) ?? anchor),
  };
}

/**
 * @param parts - the query string and the anchor of a link
 * @returns the end of the link URL that holds them, with their URL separators (empty when there are none)
 */
function linkURLSuffix({ queryString, anchor }: LinkURLParts): string {
  const query = withoutURLSeparator(queryString, "?");
  const fragment = withoutURLSeparator(anchor, "#");
  return (
    (query === undefined ? "" : `?${query}`) +
    (fragment === undefined ? "" : `#${encodeURIComponent(fragment)}`)
  );
}

/**
 * @param reference - a resource reference
 * @returns the query string and the anchor held by its parameters
 */
function linkURLPartsFromReference(reference: ResourceReference): LinkURLParts {
  return {
    queryString: withoutURLSeparator(reference.parameters.queryString, "?"),
    anchor: withoutURLSeparator(reference.parameters.anchor, "#"),
  };
}

/**
 * @param parts - the query string and the anchor of a link
 * @returns the parameters of the resource reference holding them, named as in XWiki Rendering (e.g.
 *   `[[label>>doc:Space.Page||queryString="key=value" anchor="HSection"]]`), without the empty ones
 */
function linkReferenceParameters(parts: LinkURLParts): Record<string, string> {
  const parameters: Record<string, string> = {};
  const queryString = withoutURLSeparator(parts.queryString, "?");
  if (queryString !== undefined) {
    parameters.queryString = queryString;
  }
  const anchor = withoutURLSeparator(parts.anchor, "#");
  if (anchor !== undefined) {
    parameters.anchor = anchor;
  }
  return parameters;
}

/**
 * Build the reference of a linked page or attachment.
 *
 * The model reference serializer produces a typed reference (e.g. "doc:Space.Page") while a resource
 * reference holds the type and the entity reference separately, so the type prefix is stripped. Not
 * all serializers add it though, so its presence is not assumed.
 *
 * @param parts - the query string and the anchor of the link, held by the parameters of the reference
 */
function entityResourceReference(
  type: string,
  ref: EntityReference | null,
  modelReferenceSerializerProvider: ModelReferenceSerializerProvider,
  parts: LinkURLParts = {},
): ResourceReference | undefined {
  const serialized =
    ref === null
      ? undefined
      : modelReferenceSerializerProvider.get()?.serialize(ref);
  if (!serialized) {
    return undefined;
  }
  const prefix = `${type}:`;
  return {
    ...typedResourceReference(
      type,
      serialized.startsWith(prefix)
        ? serialized.slice(prefix.length)
        : serialized,
    ),
    parameters: linkReferenceParameters(parts),
  };
}

function typedResourceReference(
  type: string,
  reference: string,
): ResourceReference {
  return { type, typed: true, reference, parameters: {} };
}

function parseEntityReference(
  reference: string,
  type: EntityType,
  modelReferenceParserProvider: ModelReferenceParserProvider,
): EntityReference | null {
  return (
    tryFallible(() =>
      modelReferenceParserProvider.get()?.parse(reference, { type }),
    ) ?? null
  );
}

export {
  entityResourceReference,
  linkURLPartsFromReference,
  linkURLPartsFromURL,
  linkURLSuffix,
  parseEntityReference,
  typedResourceReference,
};
