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
 * @param config - an e-mail link configuration
 * @returns the query string of the `mailto:` URI holding the message subject and body, with its "?" URL separator
 *   (empty when there are none). The spaces are encoded as "%20" rather than "+", which RFC 6068 doesn't decode as a
 *   space (some mail clients would display it as is).
 */
function mailtoQueryString({
  messageSubject,
  messageBody,
}: EmailLinkTargetConfig): string {
  const fields: string[] = [];
  if (messageSubject) {
    fields.push(`subject=${encodeURIComponent(messageSubject)}`);
  }
  if (messageBody) {
    fields.push(`body=${encodeURIComponent(messageBody)}`);
  }
  return fields.length > 0 ? `?${fields.join("&")}` : "";
}

/**
 * Parse the part of a `mailto:` URI that follows the scheme, which is also how XWiki Rendering stores an e-mail link
 * reference: the message subject and body are held by its query string (e.g. `jdoe@example.org?subject=Hello`), not
 * by the parameters of the resource reference. The CKEditor WYSIWYG editor stores them the same way.
 *
 * @param value - the e-mail address, followed by the query string holding the message subject and body, if any
 * @returns the matching e-mail link configuration
 */
function parseMailto(value: string): EmailLinkTargetConfig {
  const queryStringIndex = value.indexOf("?");
  if (queryStringIndex < 0) {
    return { address: value };
  }
  // The query string is decoded like the WYSIWYG editor does, i.e. accepting "+" for a space.
  const fields = new URLSearchParams(value.slice(queryStringIndex + 1));
  return {
    address: value.slice(0, queryStringIndex),
    messageSubject: fields.get("subject") ?? undefined,
    messageBody: fields.get("body") ?? undefined,
  };
}

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

    return parseMailto(url.trim().slice(parsedURL.protocol.length));
  }

  toURL(config: EmailLinkTargetConfig): string {
    return `${new URL(`mailto:${config.address}`)}${mailtoQueryString(config)}`;
  }

  /**
   * @returns the e-mail address, without the message subject and body
   */
  getDefaultLabel({ address }: EmailLinkTargetConfig): string {
    return address.trim();
  }

  fromReference(reference: ResourceReference): EmailLinkTargetConfig | null {
    return reference.type === ResourceType.MAILTO
      ? parseMailto(reference.reference)
      : null;
  }

  toReference(config: EmailLinkTargetConfig): ResourceReference | undefined {
    return config.address.trim() === ""
      ? undefined
      : typedResourceReference(
          ResourceType.MAILTO,
          `${config.address}${mailtoQueryString(config)}`,
        );
  }
}

export { EmailLinkTargetType };
export type { EmailLinkTargetConfig };
