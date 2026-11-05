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
import { AbstractPlainTextLinkTargetType } from "@xwiki/platform-link-type-api";
import { ResourceType } from "@xwiki/platform-rendering-api";
import { injectable } from "inversify";
import type { PlainTextLinkTargetConfig } from "@xwiki/platform-link-type-api";

/**
 * Built-in link target type for links to an external URL.
 *
 * @since 18.10.0RC1
 * @beta
 */
@injectable()
class URLLinkTargetType extends AbstractPlainTextLinkTargetType {
  readonly type = "url";

  // Last, since it parses any URL (see fromURL).
  readonly order = Number.MAX_SAFE_INTEGER;

  protected readonly resourceType = ResourceType.URL;

  // An untyped URL is parsed back as a URL, thanks to its scheme.
  protected readonly typedReference = false;

  getLabel(locale: string): Promise<string> {
    return labelFromTranslations("link-type.target-types.url.label")(locale);
  }

  /**
   * @returns the URL as is: this is the catch-all link target type, used for any URL that no other link target type
   *   matches
   */
  override fromURL(url: string): PlainTextLinkTargetConfig {
    return { location: url };
  }
}

export { URLLinkTargetType };
