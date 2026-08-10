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
import PageConfig from "../vue/PageConfig.vue";
import { tryFallible } from "@xwiki/platform-fn-utils";
import { EntityType } from "@xwiki/platform-model-api";
import { injectable } from "inversify";
import type { LinkPageConfig } from "../data/linkType";
import type {
  LinkTargetTypeExtension,
  LinkTargetUrlContext,
} from "@xwiki/platform-link-type-api";
import type { Component } from "vue";

/**
 * Built-in link target type for links to an internal wiki page.
 *
 * @since 18.7.0RC1
 * @beta
 */
@injectable()
class PageLinkTargetType implements LinkTargetTypeExtension<LinkPageConfig> {
  readonly type = "page";
  readonly order = 0;

  getLabel = labelFromTranslations("link-type.target-types.page.label");

  createDefaultConfig(): LinkPageConfig {
    return { ref: null };
  }

  component(): Component {
    return PageConfig;
  }

  tryParseUrl(
    url: string,
    { remoteURLParser }: LinkTargetUrlContext,
  ): LinkPageConfig | null {
    if (url.trim() === "") {
      return { ref: null };
    }

    const ref = tryFallible(() => remoteURLParser.parse(url));

    if (ref?.type !== EntityType.DOCUMENT) {
      return null;
    }

    const parsedUrl = tryFallible(() => new URL(url));

    return {
      ref,
      anchor: parsedUrl?.hash,
      queryString: parsedUrl?.search,
    };
  }

  serializeUrl(
    { ref, queryString, anchor }: LinkPageConfig,
    { remoteURLSerializer }: LinkTargetUrlContext,
  ): string {
    const base = remoteURLSerializer.serialize(ref ?? undefined);

    return `${base ?? ""}${queryString ?? ""}${anchor ?? ""}`;
  }
}

export { PageLinkTargetType };
