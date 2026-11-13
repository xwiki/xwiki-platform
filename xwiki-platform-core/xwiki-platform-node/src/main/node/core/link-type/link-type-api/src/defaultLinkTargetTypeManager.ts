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
import { injectable, multiInject, optional } from "inversify";
import type { LinkContext } from "./linkContext";
import type { LinkTarget } from "./linkData";
import type { LinkTargetType } from "./linkTargetType";
import type { LinkTargetTypeManager } from "./linkTargetTypeManager";
import type { UnsupportedLinkTargetConfig } from "./unsupportedLinkTarget";
import type { ResourceReference } from "@xwiki/platform-rendering-api";

/**
 * The order of the link target types that don't specify one.
 */
const DEFAULT_ORDER = 1000;

/**
 * Default {@link LinkTargetTypeManager}, using the link target types registered when it is instantiated.
 */
@injectable()
class DefaultLinkTargetTypeManager implements LinkTargetTypeManager {
  private readonly linkTargetTypes: LinkTargetType[];

  constructor(
    @multiInject(linkTargetTypeRole)
    @optional()
    linkTargetTypes: LinkTargetType[] | undefined,
  ) {
    this.linkTargetTypes = [...(linkTargetTypes ?? [])].sort(
      (a, b) => (a.order ?? DEFAULT_ORDER) - (b.order ?? DEFAULT_ORDER),
    );
  }

  list(): LinkTargetType[] {
    // Copy the list, so that callers can't modify it.
    return this.linkTargetTypes.slice();
  }

  fromURL(url: string): LinkTarget {
    for (const linkTargetType of this.linkTargetTypes) {
      const config = linkTargetType.fromURL(url);
      if (config !== null) {
        return { type: linkTargetType.type, config };
      }
    }

    throw new Error(`No link target type accepts the URL [${url}]`);
  }

  toURL(target: LinkTarget, context?: LinkContext): string {
    const linkTargetType = this.get(target.type);
    if (!linkTargetType) {
      throw new Error(`No link target type of type [${target.type}]`);
    }

    return linkTargetType.toURL(target.config, context);
  }

  fromReference(
    reference: ResourceReference,
    context?: LinkContext,
  ): LinkTarget {
    for (const linkTargetType of this.linkTargetTypes) {
      const config = linkTargetType.fromReference?.(reference, context) ?? null;
      if (config !== null) {
        return { type: linkTargetType.type, config };
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

  toReference(
    target: LinkTarget,
    context?: LinkContext,
  ): ResourceReference | undefined {
    if (target.type === unsupportedLinkTargetType) {
      const { reference } = target.config as UnsupportedLinkTargetConfig;
      return reference.reference.trim() === "" ? undefined : reference;
    }

    return this.get(target.type)?.toReference?.(target.config, context);
  }

  private get(type: string): LinkTargetType | undefined {
    return this.linkTargetTypes.find(
      (linkTargetType) => linkTargetType.type === type,
    );
  }
}

export { DefaultLinkTargetTypeManager };
