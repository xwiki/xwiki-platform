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
import { translateLabel } from "./labels";
import { typedResourceReference } from "./referenceHelpers";
import { tryFallible } from "@xwiki/platform-fn-utils";
import { ResourceType } from "@xwiki/platform-rendering-api";
import { injectable } from "inversify";
import type { LinkTargetType } from "@xwiki/platform-link-type-api";
import type { ResourceReference } from "@xwiki/platform-rendering-api";

/**
 * Configuration for an e-mail link
 *
 * @since 18.10.0RC1
 * @beta
 */
type EmailLinkTargetConfig = {
  address: string;
  messageSubject?: string;
  messageBody?: string;
};

/**
 * Built-in link target type for `mailto:` links.
 */
@injectable()
class EmailLinkTargetType implements LinkTargetType<EmailLinkTargetConfig> {
  readonly type = "email";
  readonly order = 200;

  getLabel(locale: string): Promise<string> {
    return translateLabel("link-type.target-types.email.label", locale);
  }

  createDefaultConfig(): EmailLinkTargetConfig {
    return { address: "" };
  }

  fromURL(url: string): EmailLinkTargetConfig | null {
    const parsedURL = tryFallible(() => new URL(url));

    if (parsedURL?.protocol !== "mailto:") {
      return null;
    }

    return {
      address: parsedURL.pathname,
      messageSubject: parsedURL.searchParams.get("subject") ?? undefined,
      messageBody: parsedURL.searchParams.get("body") ?? undefined,
    };
  }

  toURL({
    address,
    messageSubject,
    messageBody,
  }: EmailLinkTargetConfig): string {
    const url = new URL(`mailto:${address}`);

    if (messageSubject) {
      url.searchParams.set("subject", messageSubject);
    }

    if (messageBody) {
      url.searchParams.set("body", messageBody);
    }

    return url.toString();
  }

  fromReference(reference: ResourceReference): EmailLinkTargetConfig | null {
    return reference.type === ResourceType.MAILTO
      ? { address: reference.reference }
      : null;
  }

  toReference({
    address,
  }: EmailLinkTargetConfig): ResourceReference | undefined {
    return address.trim() === ""
      ? undefined
      : typedResourceReference(ResourceType.MAILTO, address);
  }
}

export { EmailLinkTargetType };
export type { EmailLinkTargetConfig };
