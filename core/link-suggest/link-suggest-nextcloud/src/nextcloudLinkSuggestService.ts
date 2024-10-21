/*
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
import {
  Link,
  type LinkSuggestService,
  LinkType,
} from "@xwiki/cristal-link-suggest-api";
import { inject, injectable } from "inversify";
import type { CristalApp } from "@xwiki/cristal-api";
import xmlescape from "xml-escape";

// TODO: To be replaced by an actual authentication with CRISTAL-267
const USERNAME = "test";
const PASSWORD = "test";

/**
 * @since 0.11
 */
@injectable()
export class NextcloudLinkSuggestService implements LinkSuggestService {
  constructor(
    @inject<CristalApp>("CristalApp") private readonly cristalApp: CristalApp,
  ) {}

  async getLinks(query: string): Promise<Link[]> {
    const baseRestURL = this.cristalApp
      .getWikiConfig()
      .baseRestURL.replace(/\/files$/, "");
    const options = {
      method: "SEARCH",
      headers: {
        "Content-Type": "text/xml",
        ...this.getBaseHeaders(),
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
                 <d:href>/files/${USERNAME}/.cristal</d:href>
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

    const attachmentsSegment = "/attachments/";
    try {
      const response = await fetch(baseRestURL, options);
      const txt = await response.text();
      const xml = new window.DOMParser().parseFromString(txt, "text/xml");
      const responseNodes = xml.getElementsByTagName("d:response");

      const links: Link[] = [];

      for (let i = 0; i < responseNodes.length; i++) {
        const responseNode = responseNodes.item(i)!;
        const dHref = responseNode.querySelector("href")!.textContent!;
        const isAttachmentFolder = dHref.endsWith(attachmentsSegment);
        const isFolder: boolean =
          responseNode.querySelector("getcontenttype")!.textContent == "";
        if (!isAttachmentFolder) {
          const displayName =
            responseNode.querySelector("displayname")!.textContent!;
          const reference = dHref.replace(/^.+\/\.cristal/, "");
          const url = `.${reference}`;

          if (isFolder) {
            // handle folder
            if (!dHref.includes(attachmentsSegment)) {
              // OK ADD as actual page
              links.push({
                id: dHref,
                url,
                reference,
                label: displayName,
                hint: displayName,
                type: LinkType.PAGE,
              });
            }
          } else {
            // handle file
            if (dHref.includes(attachmentsSegment)) {
              // ok ADD as actual attachment
              links.push({
                id: dHref,
                url,
                reference,
                label: displayName,
                hint: displayName,
                type: LinkType.ATTACHMENT,
              });
            }
          }
        }
      }
      return links;
    } catch (e) {
      console.log(`Failed to search for link with query = [${query}]`, e);
      return [];
    }
  }

  private getBaseHeaders() {
    // TODO: the authentication is currently hardcoded.
    return {
      Authorization: `Basic ${btoa(`${USERNAME}:${PASSWORD}`)}`,
    };
  }
}
