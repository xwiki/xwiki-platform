/**
 * See the LICENSE file distributed with this work for additional
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

import { LinkType } from "@xwiki/cristal-link-suggest-api";
import { EntityType } from "@xwiki/cristal-model-api";
import { inject, injectable } from "inversify";
import xmlescape from "xml-escape";
import type { CristalApp } from "@xwiki/cristal-api";
import type { AuthenticationManagerProvider } from "@xwiki/cristal-authentication-api";
import type { Link, LinkSuggestService } from "@xwiki/cristal-link-suggest-api";
import type { ModelReferenceSerializerProvider } from "@xwiki/cristal-model-reference-api";
import type { RemoteURLParserProvider } from "@xwiki/cristal-model-remote-url-api";

/**
 * @since 0.11
 */
@injectable()
export class NextcloudLinkSuggestService implements LinkSuggestService {
  constructor(
    @inject("CristalApp") private readonly cristalApp: CristalApp,
    @inject("AuthenticationManagerProvider")
    private authenticationManagerProvider: AuthenticationManagerProvider,
    @inject("RemoteURLParserProvider")
    private readonly remoteURLParserProvider: RemoteURLParserProvider,
    @inject("ModelReferenceSerializerProvider")
    private readonly modelReferenceSerializerProvider: ModelReferenceSerializerProvider,
  ) {}

  // TODO: reduce the number of statements in the following method and reactivate the disabled eslint rule.
  // eslint-disable-next-line max-statements
  async getLinks(query: string, linkType?: LinkType): Promise<Link[]> {
    const username = (
      await this.authenticationManagerProvider.get()?.getUserDetails()
    )?.username;
    if (!username) {
      console.error(
        "Could not fetch links to suggest, the user is not properly logged-in.",
      );
      return [];
    }

    const config = this.cristalApp.getWikiConfig();
    const storageRoot = (
      config.storageRoot ?? `/files/${username}/.cristal`
    ).replace("${username}", username);

    const options = {
      method: "SEARCH",
      headers: {
        "Content-Type": "text/xml",
        Authorization: (await this.authenticationManagerProvider
          .get()!
          .getAuthorizationHeader())!,
      },
      body: `<?xml version="1.0" encoding="UTF-8"?>
 <d:searchrequest xmlns:d="DAV:" xmlns:oc="http://owncloud.org/ns">
     <d:basicsearch>
         <d:select>
             <d:prop>
                 <oc:fileid/>
                 <d:displayname/>
                 <d:getcontenttype/>
                 <d:getetag/>
                 <oc:size/>
             </d:prop>
         </d:select>
         <d:from>
             <d:scope>
                 <d:href>${storageRoot}</d:href>
                 <d:depth>infinity</d:depth>
             </d:scope>
         </d:from>
         <d:where>
             <d:like>
                 <d:prop>
                     <d:displayname/>
                 </d:prop>
                 <d:literal>%${xmlescape(query)}%</d:literal>
             </d:like>
         </d:where>
         <d:orderby/>
    </d:basicsearch>
</d:searchrequest>`,
    };

    try {
      const response = await fetch(config.baseRestURL, options);
      const txt = await response.text();
      const xml = new window.DOMParser().parseFromString(txt, "text/xml");
      const responseNodes = xml.getElementsByTagName("d:response");

      const links: Link[] = [];

      for (let i = 0; i < responseNodes.length; i++) {
        const responseNode = responseNodes.item(i)!;
        const dHref = responseNode.querySelector("href")!.textContent!;

        const url = `${this.cristalApp.getWikiConfig().baseURL}${dHref}`;
        const parsed = this.remoteURLParserProvider.get()?.parse(url);
        const reference = this.modelReferenceSerializerProvider
          .get()
          ?.serialize(parsed);

        if (!reference) {
          console.error(`Unable to resolve [${dHref}]`);
          continue;
        }

        if (
          parsed?.type === EntityType.DOCUMENT ||
          parsed?.type === EntityType.ATTACHMENT
        ) {
          const type =
            parsed?.type === EntityType.DOCUMENT
              ? LinkType.PAGE
              : LinkType.ATTACHMENT;
          if (type == linkType || linkType == undefined) {
            links.push({
              id: dHref,
              url,
              reference,
              label: parsed.name,
              hint: parsed.name,
              type,
            });
          }
        }
      }
      return links;
    } catch (e) {
      console.log(`Failed to search for link with query = [${query}]`, e);
      return [];
    }
  }
}
