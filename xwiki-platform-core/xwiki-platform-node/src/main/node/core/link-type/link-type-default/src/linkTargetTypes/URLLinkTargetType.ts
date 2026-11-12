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
import { completeURL, hasURLScheme } from "./urlCompletion";
import { AbstractPlainTextLinkTargetType } from "@xwiki/platform-link-type-api";
import { ResourceType } from "@xwiki/platform-rendering-api";
import { injectable } from "inversify";
import type { PlainTextLinkTargetConfig } from "@xwiki/platform-link-type-api";
import type { ResourceReference } from "@xwiki/platform-rendering-api";

/**
 * Built-in link target type for links to an external URL.
 */
@injectable()
class URLLinkTargetType extends AbstractPlainTextLinkTargetType {
  readonly type = "url";

  // Last, since it parses any URL (see fromURL).
  readonly order = Number.MAX_SAFE_INTEGER;

  protected readonly resourceType = ResourceType.URL;

  // An untyped URL is parsed back as a URL thanks to its scheme, see toReference for the URLs without one.
  protected readonly typedReference = false;

  getLabel(locale: string): Promise<string> {
    return translateLabel("link-type.target-types.url.label", locale);
  }

  /**
   * @returns the URL as is: this is the catch-all link target type, used for any URL that no other link target type
   *   matches
   */
  override fromURL(url: string): PlainTextLinkTargetConfig {
    return { location: url };
  }

  /**
   * @returns the URL typed by the user, completed (see {@link completeURL})
   */
  override toURL({ location }: PlainTextLinkTargetConfig): string {
    return completeURL(location);
  }

  /**
   * @returns the reference of the completed URL (see {@link completeURL}), or, when it has no scheme (e.g. a relative
   *   URL), a typed path reference: XWiki Rendering accepts a URL reference only with a scheme, and parses anything else
   *   back as a document reference, even when typed (i.e. `url:/xwiki/bin/view/Main/` is a link to a page named that
   *   way). The link is then edited as a path link.
   */
  override toReference({
    location,
  }: PlainTextLinkTargetConfig): ResourceReference | undefined {
    const url = completeURL(location);
    if (url === "") {
      return undefined;
    }
    const absolute = hasURLScheme(url);
    return {
      type: absolute ? this.resourceType : ResourceType.PATH,
      typed: !absolute,
      reference: url,
      parameters: {},
    };
  }
}

export { URLLinkTargetType };
