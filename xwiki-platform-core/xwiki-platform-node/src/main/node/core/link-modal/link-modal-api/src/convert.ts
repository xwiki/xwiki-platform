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
import type { Resolver } from "@xwiki/platform-component-manager-api";
import type { ResourceReference } from "@xwiki/platform-rendering-api";

/**
 * @param resolver - the component manager resolver to look up registered {@link LinkTargetTypeExtension}s from
 * @returns every registered link target type extension, sorted by ascending {@link LinkTargetTypeExtension.order}
 *
 * @since 18.7.0RC1
 * @beta
 */
async function listLinkTargetTypeExtensions(
  resolver: Resolver,
): Promise<LinkTargetTypeExtension[]> {
  const extensions = await resolver.getAllAsync<LinkTargetTypeExtension>(
    linkTargetTypeExtensionRole,
  );

  return [...extensions].sort((a, b) => (a.order ?? 1000) - (b.order ?? 1000));
}

/**
 * @param resolver - the component manager resolver to look up registered `LinkTargetTypeExtension`s from
 * @returns the registered link target type extensions whose `LinkTargetTypeExtension.isEnabled` resolves
 *   to `true` (or is unset), sorted by ascending `LinkTargetTypeExtension.order`. Intended for populating
 *   the link type selector — `parseLinkTarget`/`serializeLinkTarget` deliberately consider *every*
 *   registered extension (see `listLinkTargetTypeExtensions`), so that a link using a type that was
 *   disabled after being created can still be displayed and re-submitted correctly.
 *
 * @since 18.7.0RC1
 * @beta
 */
async function listEnabledLinkTargetTypeExtensions(
  resolver: Resolver,
): Promise<LinkTargetTypeExtension[]> {
  const extensions = await listLinkTargetTypeExtensions(resolver);

  const withEnabledFlag = await Promise.all(
    extensions.map(async (extension) => ({
      extension,
      enabled: (await extension.isEnabled?.()) ?? true,
    })),
  );

  return withEnabledFlag
    .filter(({ enabled }) => enabled)
    .map(({ extension }) => extension);
}

/**
 * Parse a link target from a raw URL, by trying each registered {@link LinkTargetTypeExtension} in ascending
 * {@link LinkTargetTypeExtension.order}, and keeping the first one that matches (see
 * {@link LinkTargetTypeExtension.tryParseUrl}).
 *
 * @param url - the URL to parse
 * @param resolver - the component manager resolver to look up registered link target types from
 * @param ctx - services needed by extensions to inspect the URL
 *
 * @returns the parsed link target
 *
 * @throws {@link Error} when no registered extension matches the URL (should not happen as long as a
 *   permissive catch-all extension, such as the built-in `"url"` type, is registered)
 *
 * @since 18.7.0RC1
 * @beta
 */
async function parseLinkTarget(
  url: string,
  resolver: Resolver,
  ctx: LinkTargetUrlContext,
): Promise<LinkTarget> {
  const extensions = await listLinkTargetTypeExtensions(resolver);

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
 * @param resolver - the component manager resolver to look up registered link target types from
 * @param ctx - services needed by extensions to build the URL
 *
 * @returns the serialized URL
 *
 * @throws {@link Error} when no registered extension matches `target.type`
 *
 * @since 18.7.0RC1
 * @beta
 */
async function serializeLinkTarget(
  target: LinkTarget,
  resolver: Resolver,
  ctx: LinkTargetUrlContext,
): Promise<string> {
  const extensions = await listLinkTargetTypeExtensions(resolver);
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
 * Every registered extension is considered, not just the enabled ones (see
 * {@link listLinkTargetTypeExtensions}), for the same reason as {@link parseLinkTarget}.
 *
 * @param reference - the reference of the linked resource
 * @param resolver - the component manager resolver to look up registered link target types from
 * @param ctx - services needed by extensions to inspect the reference
 *
 * @returns the matching link target, or `undefined` when no registered extension claims this resource
 *   type, so that the caller can fall back on {@link parseLinkTarget}
 *
 * @since 18.7.0RC1
 * @beta
 */
async function resourceReferenceToLinkTarget(
  reference: ResourceReference,
  resolver: Resolver,
  ctx: LinkTargetReferenceContext,
): Promise<LinkTarget | undefined> {
  const extensions = await listLinkTargetTypeExtensions(resolver);

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
 * @param resolver - the component manager resolver to look up registered link target types from
 * @param ctx - services needed by extensions to build the reference
 *
 * @returns the reference of the linked resource, or `undefined` when no registered extension matches
 *   `target.type`, or when the matching extension doesn't implement
 *   {@link LinkTargetTypeExtension.configToReference}, or when it does but the configuration doesn't
 *   designate a resource yet (e.g. no page selected, empty URL)
 *
 * @since 18.7.0RC1
 * @beta
 */
async function linkTargetToResourceReference(
  target: LinkTarget,
  resolver: Resolver,
  ctx: LinkTargetReferenceContext,
): Promise<ResourceReference | undefined> {
  const extensions = await listLinkTargetTypeExtensions(resolver);
  const extension = extensions.find((e) => e.type === target.type);

  return extension?.configToReference?.(target.config, ctx);
}

export {
  linkTargetToResourceReference,
  listEnabledLinkTargetTypeExtensions,
  listLinkTargetTypeExtensions,
  parseLinkTarget,
  resourceReferenceToLinkTarget,
  serializeLinkTarget,
};
