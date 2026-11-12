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
import type { LinkContext } from "./linkContext";
import type { LinkTarget } from "./linkData";
import type { LinkTargetType } from "./linkTargetType";
import type { ResourceReference } from "@xwiki/platform-rendering-api";

/**
 * Inversify role of the {@link LinkTargetTypeManager}.
 *
 * @since 18.10.0RC1
 * @beta
 */
const linkTargetTypeManagerRole = "LinkTargetTypeManager";

/**
 * Gives access to the registered {@link LinkTargetType}s, and converts link targets from and to the URLs and the
 * resource references of the links, using the link target type matching each of them.
 *
 * @since 18.10.0RC1
 * @beta
 */
interface LinkTargetTypeManager {
  /**
   * List the registered link target types.
   *
   * @returns the registered link target types, sorted by ascending {@link LinkTargetType.order}
   */
  list(): LinkTargetType[];

  /**
   * Convert a URL into a link target, using the first link target type, in ascending
   * {@link LinkTargetType.order}, that accepts it (see {@link LinkTargetType.fromURL}).
   *
   * @param url - the URL of a link
   * @returns the target of the link
   * @throws {@link Error} when no registered link target type accepts the URL, which doesn't happen as long as a
   *   catch-all link target type, such as the built-in `"url"` type, is registered
   */
  fromURL(url: string): LinkTarget;

  /**
   * Convert a link target into a URL, using the link target type matching its type. The reverse of
   * {@link LinkTargetTypeManager.fromURL}.
   *
   * @param target - the target of a link
   * @param context - where the link is (see {@link LinkTargetType.toURL})
   * @returns the URL of the link
   * @throws {@link Error} when no registered link target type matches the type of the target, which is also the case
   *   for a target of the {@link unsupportedLinkTargetType} type: the URL of a resource that no link target type
   *   supports can't be computed client-side
   */
  toURL(target: LinkTarget, context?: LinkContext): string;

  /**
   * Convert the reference of a linked resource into a link target, using the first link target type, in ascending
   * {@link LinkTargetType.order}, that accepts it (see {@link LinkTargetType.fromReference}).
   *
   * @param reference - the reference of a linked resource
   * @param context - (since 18.10.0RC1) where the link is, e.g. the document that a relative reference is resolved
   *   against
   * @returns the target of the link, or a target of the {@link unsupportedLinkTargetType} type holding a copy of the
   *   reference when no registered link target type accepts it, so that the link can be edited without changing the
   *   type of the linked resource
   */
  fromReference(
    reference: ResourceReference,
    context?: LinkContext,
  ): LinkTarget;

  /**
   * Convert a link target into the reference of the linked resource, using the link target type matching its type.
   * The reverse of {@link LinkTargetTypeManager.fromReference}.
   *
   * @param target - the target of a link
   * @param context - (since 18.10.0RC1) where the link is, e.g. the document that the reference is made relative to
   * @returns the reference of the linked resource (for a target of the {@link unsupportedLinkTargetType} type, the
   *   reference it holds), or `undefined` when the target doesn't designate a resource yet (e.g. no page selected,
   *   empty URL), or when no registered link target type converts it (see {@link LinkTargetType.toReference})
   */
  toReference(
    target: LinkTarget,
    context?: LinkContext,
  ): ResourceReference | undefined;
}

export { linkTargetTypeManagerRole };
export type { LinkTargetTypeManager };
