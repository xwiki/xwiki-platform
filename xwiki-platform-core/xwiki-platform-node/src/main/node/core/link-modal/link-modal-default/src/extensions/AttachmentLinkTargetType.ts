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
import AttachmentConfig from "../vue/AttachmentConfig.vue";
import { tryFallible } from "@xwiki/platform-fn-utils";
import { EntityType } from "@xwiki/platform-model-api";
import { injectable } from "inversify";
import type { LinkAttachmentConfig } from "../data/linkType";
import type {
  LinkTargetTypeExtension,
  LinkTargetUrlContext,
} from "@xwiki/platform-link-modal-api";
import type { Component } from "vue";

/**
 * Built-in link target type for links to an attachment.
 *
 * @since 18.7.0RC1
 * @beta
 */
@injectable()
class AttachmentLinkTargetType
  implements LinkTargetTypeExtension<LinkAttachmentConfig>
{
  readonly type = "attachment";
  readonly order = 100;

  getLabel = labelFromTranslations("link-modal.target-types.attachment.label");

  createDefaultConfig(): LinkAttachmentConfig {
    return { ref: null };
  }

  component(): Component {
    return AttachmentConfig;
  }

  tryParseUrl(
    url: string,
    { remoteURLParser }: LinkTargetUrlContext,
  ): LinkAttachmentConfig | null {
    const ref = tryFallible(() => remoteURLParser.parse(url));

    if (ref?.type !== EntityType.ATTACHMENT) {
      return null;
    }

    const parsedUrl = tryFallible(() => new URL(url));

    return {
      ref,
      queryString: parsedUrl?.search,
    };
  }

  serializeUrl(
    { ref, queryString }: LinkAttachmentConfig,
    { remoteURLSerializer }: LinkTargetUrlContext,
  ): string {
    const base = remoteURLSerializer.serialize(ref ?? undefined);

    return `${base ?? ""}${queryString ?? ""}`;
  }
}

export { AttachmentLinkTargetType };
