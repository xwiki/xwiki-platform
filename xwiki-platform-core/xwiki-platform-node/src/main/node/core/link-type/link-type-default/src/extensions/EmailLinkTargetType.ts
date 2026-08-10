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
import { typedResourceReference } from "./referenceHelpers";
import { labelFromTranslations } from "./labels";
import EmailConfig from "../vue/EmailConfig.vue";
import { tryFallible } from "@xwiki/platform-fn-utils";
import { ResourceType } from "@xwiki/platform-rendering-api";
import { injectable } from "inversify";
import type { LinkEmailConfig } from "../data/linkType";
import type { LinkTargetTypeExtension } from "@xwiki/platform-link-type-api";
import type { ResourceReference } from "@xwiki/platform-rendering-api";
import type { Component } from "vue";

/**
 * Built-in link target type for `mailto:` links.
 *
 * @since 18.7.0RC1
 * @beta
 */
@injectable()
class EmailLinkTargetType implements LinkTargetTypeExtension<LinkEmailConfig> {
  readonly type = "email";
  readonly order = 200;

  getLabel = labelFromTranslations("link-type.target-types.email.label");

  createDefaultConfig(): LinkEmailConfig {
    return { address: "" };
  }

  component(): Component {
    return EmailConfig;
  }

  tryParseUrl(url: string): LinkEmailConfig | null {
    const parsedUrl = tryFallible(() => new URL(url));

    if (parsedUrl?.protocol !== "mailto:") {
      return null;
    }

    return {
      address: parsedUrl.pathname,
      messageSubject: parsedUrl.searchParams.get("subject") ?? undefined,
      messageBody: parsedUrl.searchParams.get("body") ?? undefined,
    };
  }

  serializeUrl({
    address,
    messageSubject,
    messageBody,
  }: LinkEmailConfig): string {
    const url = new URL(`mailto:${address}`);

    if (messageSubject) {
      url.searchParams.set("subject", messageSubject);
    }

    if (messageBody) {
      url.searchParams.set("body", messageBody);
    }

    return url.toString();
  }

  tryParseReference(reference: ResourceReference): LinkEmailConfig | null {
    return reference.type === ResourceType.MAILTO
      ? { address: reference.reference }
      : null;
  }

  configToReference({
    address,
  }: LinkEmailConfig): ResourceReference | undefined {
    return address.trim() === ""
      ? undefined
      : typedResourceReference(ResourceType.MAILTO, address);
  }
}

export { EmailLinkTargetType };
