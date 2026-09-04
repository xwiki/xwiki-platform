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
import { linkTargetTypeExtensionRole } from "./linkTargetType";
import type { LinkTarget } from "./data/linkType";
import type {
  LinkTargetReferenceContext,
  LinkTargetTypeExtension,
  LinkTargetUrlContext,
} from "./linkTargetType";
import type { ResourceReference } from "@xwiki/platform-rendering-api";
import type { Container } from "inversify";

/**
 * @param container - the `depsContainer` to look up registered {@link LinkTargetTypeExtension}s from
 * @returns every registered link target type extension, sorted by ascending {@link LinkTargetTypeExtension.order}
 *
 * @since 18.8.0RC1
 * @beta
 */
function listLinkTargetTypeExtensions(
  container: Container,
): LinkTargetTypeExtension[] {
  const extensions = container.getAll<LinkTargetTypeExtension>(
    linkTargetTypeExtensionRole,
  );

  return [...extensions].sort((a, b) => (a.order ?? 1000) - (b.order ?? 1000));
}

/**
 * Parse a link target from a raw URL, by trying each registered {@link LinkTargetTypeExtension} in ascending
 * {@link LinkTargetTypeExtension.order}, and keeping the first one that matches (see
 * {@link LinkTargetTypeExtension.tryParseUrl}).
 *
 * @param url - the URL to parse
 * @param container - the `depsContainer` to look up registered link target types from
 * @param ctx - services needed by extensions to inspect the URL
 *
 * @returns the parsed link target
 *
 * @throws {@link Error} when no registered extension matches the URL (should not happen as long as a
 *   permissive catch-all extension, such as the built-in `"url"` type, is registered)
 *
 * @since 18.8.0RC1
 * @beta
 */
function parseLinkTarget(
  url: string,
  container: Container,
  ctx: LinkTargetUrlContext,
): LinkTarget {
  const extensions = listLinkTargetTypeExtensions(container);

  for (const extension of extensions) {
    const config = extension.tryParseUrl(url, ctx);

    if (config !== null) {
      return { type: extension.type, config };
    }
  }

  throw new Error(
    `No link target type extension could parse this URL: "${url}"`,
  );
}

/**
 * Serialize a link target back into a raw URL, using the registered {@link LinkTargetTypeExtension} matching
 * its `type` field. The reverse of {@link parseLinkTarget}.
 *
 * @param target - the link target to serialize
 * @param container - the `depsContainer` to look up registered link target types from
 * @param ctx - services needed by extensions to build the URL
 *
 * @returns the serialized URL
 *
 * @throws {@link Error} when no registered extension matches `target.type`
 *
 * @since 18.8.0RC1
 * @beta
 */
function serializeLinkTarget(
  target: LinkTarget,
  container: Container,
  ctx: LinkTargetUrlContext,
): string {
  const extensions = listLinkTargetTypeExtensions(container);
  const extension = extensions.find((e) => e.type === target.type);

  if (!extension) {
    throw new Error(
      `No registered link target type extension for type "${target.type}"`,
    );
  }

  return extension.serializeUrl(target.config, ctx);
}

/**
 * Convert the reference of a linked resource into the link target used to configure it in the link
 * modal, by trying each registered {@link LinkTargetTypeExtension} in ascending
 * {@link LinkTargetTypeExtension.order} (see {@link LinkTargetTypeExtension.tryParseReference}). The
 * reverse of {@link linkTargetToResourceReference}.
 *
 * @param reference - the reference of the linked resource
 * @param container - the `depsContainer` to look up registered link target types from
 * @param ctx - services needed by extensions to inspect the reference
 *
 * @returns the matching link target, or `undefined` when no registered extension claims this resource
 *   type, so that the caller can fall back on {@link parseLinkTarget}
 *
 * @since 18.8.0RC1
 * @beta
 */
function resourceReferenceToLinkTarget(
  reference: ResourceReference,
  container: Container,
  ctx: LinkTargetReferenceContext,
): LinkTarget | undefined {
  const extensions = listLinkTargetTypeExtensions(container);

  for (const extension of extensions) {
    const config = extension.tryParseReference?.(reference, ctx) ?? null;

    if (config !== null) {
      return { type: extension.type, config };
    }
  }

  return undefined;
}

/**
 * Convert a link target into the reference of the resource it designates, using the registered
 * {@link LinkTargetTypeExtension} matching its `type` field. The reverse of
 * {@link resourceReferenceToLinkTarget}.
 *
 * @param target - the link target to convert
 * @param container - the `depsContainer` to look up registered link target types from
 * @param ctx - services needed by extensions to build the reference
 *
 * @returns the reference of the linked resource, or `undefined` when no registered extension matches
 *   `target.type`, or when the matching extension doesn't implement
 *   {@link LinkTargetTypeExtension.configToReference}, or when it does but the configuration doesn't
 *   designate a resource yet (e.g. no page selected, empty URL)
 *
 * @since 18.8.0RC1
 * @beta
 */
function linkTargetToResourceReference(
  target: LinkTarget,
  container: Container,
  ctx: LinkTargetReferenceContext,
): ResourceReference | undefined {
  const extensions = listLinkTargetTypeExtensions(container);
  const extension = extensions.find((e) => e.type === target.type);

  return extension?.configToReference?.(target.config, ctx);
}

export {
  linkTargetToResourceReference,
  listLinkTargetTypeExtensions,
  parseLinkTarget,
  resourceReferenceToLinkTarget,
  serializeLinkTarget,
};
