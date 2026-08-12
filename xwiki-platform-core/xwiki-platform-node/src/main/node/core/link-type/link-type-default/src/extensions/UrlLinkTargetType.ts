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
import { labelFromTranslations } from "./labels";
import { untypedResourceReference } from "./referenceHelpers";
import UrlConfig from "../vue/UrlConfig.vue";
import { ResourceType } from "@xwiki/platform-rendering-api";
import { injectable } from "inversify";
import type { LinkUrlConfig } from "../data/linkType";
import type { LinkTargetTypeExtension } from "@xwiki/platform-link-type-api";
import type { ResourceReference } from "@xwiki/platform-rendering-api";
import type { Component } from "vue";

/**
 * Built-in link target type for a plain, arbitrary URL. Acts as the catch-all: it always matches, so it must be
 * registered with the lowest priority (highest {@link order} value) among all registered link target types, so
 * that every other, more specific type gets a chance to match first.
 *
 * @since 18.7.0RC1
 * @beta
 */
@injectable()
class UrlLinkTargetType implements LinkTargetTypeExtension<LinkUrlConfig> {
  readonly type = "url";
  readonly order = Number.MAX_SAFE_INTEGER;

  getLabel = labelFromTranslations("link-type.target-types.url.label");

  createDefaultConfig(): LinkUrlConfig {
    return { url: "" };
  }

  component(): Component {
    return UrlConfig;
  }

  tryParseUrl(url: string): LinkUrlConfig | null {
    return { url };
  }

  serializeUrl({ url }: LinkUrlConfig): string {
    return url;
  }

  /**
   * Unlike {@link tryParseUrl}, this is NOT a catch-all: it only claims the resource types that
   * genuinely designate a bare URL, so that {@link resourceReferenceToLinkTarget} correctly falls
   * through to `undefined` (and the caller to {@link parseLinkTarget}) for resource types with no
   * matching link target (e.g. interwiki, space, user).
   */
  tryParseReference(reference: ResourceReference): LinkUrlConfig | null {
    switch (reference.type) {
      case ResourceType.URL:
      case ResourceType.PATH:
      case ResourceType.UNC:
      case ResourceType.UNKNOWN:
        return { url: reference.reference };
      default:
        return null;
    }
  }

  configToReference({ url }: LinkUrlConfig): ResourceReference | undefined {
    return url.trim() === ""
      ? undefined
      : untypedResourceReference(ResourceType.URL, url);
  }
}

export { UrlLinkTargetType };
