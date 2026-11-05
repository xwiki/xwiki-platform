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
import { linkTargetTypeRole } from "./linkTargetType";
import { unsupportedLinkTargetType } from "./unsupportedLinkTarget";
import type { LinkTarget } from "./data/linkType";
import type { LinkTargetType } from "./linkTargetType";
import type { UnsupportedLinkTargetConfig } from "./unsupportedLinkTarget";
import type { ResourceReference } from "@xwiki/platform-rendering-api";
import type { Container } from "inversify";

/**
 * @param container - the `depsContainer` to look up registered {@link LinkTargetType}s from
 * @returns every registered link target type, sorted by ascending {@link LinkTargetType.order}
 *
 * @since 18.10.0RC1
 * @beta
 */
function listLinkTargetTypes(container: Container): LinkTargetType[] {
  const targetTypes = container.getAll<LinkTargetType>(linkTargetTypeRole);

  return [...targetTypes].sort((a, b) => (a.order ?? 1000) - (b.order ?? 1000));
}

/**
 * Parse a link target from a raw URL, by trying each registered {@link LinkTargetType} in ascending
 * {@link LinkTargetType.order}, and keeping the first one that matches (see
 * {@link LinkTargetType.tryParseURL}).
 *
 * @param url - the URL to parse
 * @param container - the `depsContainer` to look up registered link target types from
 *
 * @returns the parsed link target
 *
 * @throws {@link Error} when no registered link target type matches the URL (should not happen as long as a
 *   permissive catch-all link target type, such as the built-in `"url"` type, is registered)
 *
 * @since 18.10.0RC1
 * @beta
 */
function parseLinkTarget(url: string, container: Container): LinkTarget {
  const targetTypes = listLinkTargetTypes(container);

  for (const targetType of targetTypes) {
    const config = targetType.tryParseURL(url);

    if (config !== null) {
      return { type: targetType.type, config };
    }
  }

  throw new Error(`No link target type could parse this URL: "${url}"`);
}

/**
 * Serialize a link target back into a raw URL, using the registered {@link LinkTargetType} matching
 * its `type` field. The reverse of {@link parseLinkTarget}.
 *
 * @param target - the link target to serialize
 * @param container - the `depsContainer` to look up registered link target types from
 *
 * @returns the serialized URL
 *
 * @throws {@link Error} when no registered link target type matches `target.type`, which is also the case for a
 *   target of the {@link unsupportedLinkTargetType} type: the URL of an unsupported resource can't be computed
 *   client-side
 *
 * @since 18.10.0RC1
 * @beta
 */
function serializeLinkTarget(target: LinkTarget, container: Container): string {
  const targetTypes = listLinkTargetTypes(container);
  const targetType = targetTypes.find(
    (candidate) => candidate.type === target.type,
  );

  if (!targetType) {
    throw new Error(`No registered link target type for type "${target.type}"`);
  }

  return targetType.serializeURL(target.config);
}

/**
 * Convert the reference of a linked resource into the link target used to configure it in the link
 * modal, by trying each registered {@link LinkTargetType} in ascending
 * {@link LinkTargetType.order} (see {@link LinkTargetType.tryParseReference}). The
 * reverse of {@link linkTargetToResourceReference}.
 *
 * @param reference - the reference of the linked resource
 * @param container - the `depsContainer` to look up registered link target types from
 *
 * @returns the matching link target, or a target of the {@link unsupportedLinkTargetType} type holding a copy of the
 *   reference when no registered link target type claims this resource type, so that the link can be edited without
 *   changing the type of the linked resource
 *
 * @since 18.10.0RC1
 * @beta
 */
function resourceReferenceToLinkTarget(
  reference: ResourceReference,
  container: Container,
): LinkTarget {
  const targetTypes = listLinkTargetTypes(container);

  for (const targetType of targetTypes) {
    const config = targetType.tryParseReference?.(reference) ?? null;

    if (config !== null) {
      return { type: targetType.type, config };
    }
  }

  // Copy the reference, so that editing the link target (which the link modal does in place) doesn't modify the
  // reference of the edited link. A JSON round trip is enough because a resource reference is plain JSON data, and
  // unlike structuredClone() it also accepts a Vue reactive proxy.
  const config: UnsupportedLinkTargetConfig = {
    reference: JSON.parse(JSON.stringify(reference)),
  };
  return { type: unsupportedLinkTargetType, config };
}

/**
 * Convert a link target into the reference of the resource it designates, using the registered
 * {@link LinkTargetType} matching its `type` field. The reverse of
 * {@link resourceReferenceToLinkTarget}.
 *
 * @param target - the link target to convert
 * @param container - the `depsContainer` to look up registered link target types from
 *
 * @returns the reference of the linked resource, or `undefined` when no registered link target type matches
 *   `target.type`, or when the matching link target type doesn't implement
 *   {@link LinkTargetType.configToReference}, or when it does but the configuration doesn't
 *   designate a resource yet (e.g. no page selected, empty URL). A target of the {@link unsupportedLinkTargetType}
 *   type is converted to the reference it holds, unless that reference is empty.
 *
 * @since 18.10.0RC1
 * @beta
 */
function linkTargetToResourceReference(
  target: LinkTarget,
  container: Container,
): ResourceReference | undefined {
  if (target.type === unsupportedLinkTargetType) {
    const { reference } = target.config as UnsupportedLinkTargetConfig;
    return reference.reference.trim() === "" ? undefined : reference;
  }

  const targetTypes = listLinkTargetTypes(container);
  const targetType = targetTypes.find(
    (candidate) => candidate.type === target.type,
  );

  return targetType?.configToReference?.(target.config);
}

export {
  linkTargetToResourceReference,
  listLinkTargetTypes,
  parseLinkTarget,
  resourceReferenceToLinkTarget,
  serializeLinkTarget,
};
