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
import { ResourceType } from "@xwiki/platform-rendering-api";
import type { LinkTarget } from "./data/linkType";
import type { EntityReference } from "@xwiki/platform-model-api";
import type {
  ModelReferenceParser,
  ModelReferenceSerializer,
} from "@xwiki/platform-model-reference-api";
import type { RemoteURLParser } from "@xwiki/platform-model-remote-url-api";
import type { ResourceReference } from "@xwiki/platform-rendering-api";

/**
 * Parse a link target from an URL
 *
 * @param url - URL to parse
 * @param remoteURLParser - Remote URL parser
 *
 * @returns The parsed link target
 *
 * @since 18.5.0RC1
 * @beta
 */
function parseLinkTarget(
  url: string,
  remoteURLParser: RemoteURLParser,
): LinkTarget {
  // When no URL is input, show the "page" link selector by default
  if (url.trim() === "") {
    return { type: "page", config: { ref: null } };
  }

  const ref = tryFallible(() => remoteURLParser.parse(url));

  const parsedUrl = tryFallible(() => new URL(url));

  switch (ref?.type) {
    case EntityType.DOCUMENT:
      return {
        type: "page",
        config: {
          ref,
          anchor: parsedUrl?.hash,
          queryString: parsedUrl?.search,
        },
      };

    case EntityType.ATTACHMENT:
      return {
        type: "attachment",
        config: {
          ref,
          queryString: parsedUrl?.search,
        },
      };
  }

  if (parsedUrl?.protocol === "mailto:") {
    const params = new URLSearchParams(parsedUrl.searchParams);

    return {
      type: "email",
      config: {
        address: parsedUrl.pathname,
        messageSubject: params.get("subject") ?? undefined,
        messageBody: params.get("body") ?? undefined,
      },
    };
  }

  return { type: "url", config: { url } };
}

/**
 * Convert a link target into the reference of the resource it designates.
 *
 * @param target - the link target, as configured in the link modal
 * @param modelReferenceSerializer - used to serialize the entity reference of a page or attachment
 *   target
 *
 * @returns the reference of the linked resource, or undefined when the target doesn't designate one
 *   yet (e.g. no page selected, empty URL)
 *
 * @since 18.7.0RC1
 * @beta
 */
function linkTargetToResourceReference(
  target: LinkTarget,
  modelReferenceSerializer: ModelReferenceSerializer,
): ResourceReference | undefined {
  switch (target.type) {
    case "page":
      return entityResourceReference(
        ResourceType.DOCUMENT,
        target.config.ref,
        modelReferenceSerializer,
      );

    case "attachment":
      return entityResourceReference(
        ResourceType.ATTACHMENT,
        target.config.ref,
        modelReferenceSerializer,
      );

    case "url":
      // A bare URL is an untyped resource reference, like when it is parsed from the wiki syntax.
      return target.config.url.trim() === ""
        ? undefined
        : untypedResourceReference(ResourceType.URL, target.config.url);

    case "email":
      // 'mailto' is a known URI scheme, so the resource reference is a typed one.
      return target.config.address.trim() === ""
        ? undefined
        : typedResourceReference(ResourceType.MAILTO, target.config.address);
  }
}

/**
 * Convert the reference of a linked resource into the link target used to configure it in the link
 * modal. This is the reverse of {@link linkTargetToResourceReference}.
 *
 * @param reference - the reference of the linked resource
 * @param modelReferenceParser - used to parse the entity reference of a page or attachment reference
 *
 * @returns the matching link target, or undefined when the resource type has no matching link target
 *   (e.g. an interwiki reference), so that the caller can fall back on {@link parseLinkTarget}
 *
 * @since 18.7.0RC1
 * @beta
 */
function resourceReferenceToLinkTarget(
  reference: ResourceReference,
  modelReferenceParser: ModelReferenceParser,
): LinkTarget | undefined {
  switch (reference.type) {
    case ResourceType.DOCUMENT: {
      const ref = parseEntityReference(
        reference.reference,
        EntityType.DOCUMENT,
        modelReferenceParser,
      );
      return ref?.type === EntityType.DOCUMENT
        ? { type: "page", config: { ref } }
        : undefined;
    }

    case ResourceType.ATTACHMENT: {
      const ref = parseEntityReference(
        reference.reference,
        EntityType.ATTACHMENT,
        modelReferenceParser,
      );
      return ref?.type === EntityType.ATTACHMENT
        ? { type: "attachment", config: { ref } }
        : undefined;
    }

    case ResourceType.MAILTO:
      return { type: "email", config: { address: reference.reference } };

    case ResourceType.URL:
    case ResourceType.PATH:
    case ResourceType.UNC:
    case ResourceType.UNKNOWN:
      return { type: "url", config: { url: reference.reference } };

    default:
      // The link modal has no configuration for the remaining resource types (space, page,
      // pageAttach, interwiki, icon, data, user) for now.
      return undefined;
  }
}

/**
 * Build the reference of a linked page or attachment.
 *
 * The model reference serializer produces a typed reference (e.g. "doc:Space.Page") while a resource
 * reference holds the type and the entity reference separately, so the type prefix is stripped. Not
 * all serializers add it though, so its presence is not assumed.
 */
function entityResourceReference(
  type: string,
  ref: EntityReference | null,
  modelReferenceSerializer: ModelReferenceSerializer,
): ResourceReference | undefined {
  const serialized =
    ref === null ? undefined : modelReferenceSerializer.serialize(ref);
  if (!serialized) {
    return undefined;
  }
  const prefix = `${type}:`;
  return typedResourceReference(
    type,
    serialized.startsWith(prefix)
      ? serialized.slice(prefix.length)
      : serialized,
  );
}

function typedResourceReference(
  type: string,
  reference: string,
): ResourceReference {
  return { type, typed: true, reference, parameters: {} };
}

function untypedResourceReference(
  type: string,
  reference: string,
): ResourceReference {
  return { type, typed: false, reference, parameters: {} };
}

function parseEntityReference(
  reference: string,
  type: EntityType,
  modelReferenceParser: ModelReferenceParser,
): EntityReference | null {
  return tryFallible(() => modelReferenceParser.parse(reference, { type }));
}

export {
  linkTargetToResourceReference,
  parseLinkTarget,
  resourceReferenceToLinkTarget,
};
