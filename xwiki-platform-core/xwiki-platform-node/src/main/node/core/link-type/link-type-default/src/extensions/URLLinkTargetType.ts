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
import { AbstractPlainTextLinkTargetType } from "./AbstractPlainTextLinkTargetType";
import { URLLinkTargetForm } from "../vue/plainTextLinkTargetForms";
import { ResourceType } from "@xwiki/platform-rendering-api";
import { injectable } from "inversify";
import type { PlainTextLinkTargetConfig } from "../data/linkType";
import type { Component } from "vue";

/**
 * Built-in link target type for links to an external URL.
 *
 * @since 18.10.0RC1
 * @beta
 */
@injectable()
class URLLinkTargetType extends AbstractPlainTextLinkTargetType {
  readonly type = "url";

  // Last, since it parses any URL (see tryParseURL).
  readonly order = Number.MAX_SAFE_INTEGER;

  protected readonly resourceType = ResourceType.URL;

  // An untyped URL is parsed back as a URL, thanks to its scheme.
  protected readonly typedReference = false;

  protected readonly labelKey = "link-type.target-types.url.label";

  component(): Component {
    return URLLinkTargetForm;
  }

  /**
   * @returns the URL as is: this is the catch-all link target type, used for any URL that no other link target type
   *   matches
   */
  override tryParseURL(url: string): PlainTextLinkTargetConfig {
    return { location: url };
  }
}

export { URLLinkTargetType };
