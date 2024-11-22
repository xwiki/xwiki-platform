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
  AttachmentReference,
  DocumentReference,
  SpaceReference,
} from "@xwiki/cristal-model-api";
import { RemoteURLParser } from "@xwiki/cristal-model-remote-url-api";
import { inject, injectable } from "inversify";
import type { CristalApp } from "@xwiki/cristal-api";
import type { EntityReference } from "@xwiki/cristal-model-api";

@injectable()
class XWikiRemoteURLParser implements RemoteURLParser {
  constructor(
    @inject<CristalApp>("CristalApp") private readonly cristalApp: CristalApp,
  ) {}

  parse(urlStr: string): EntityReference {
    const baseURLstr = this.cristalApp.getWikiConfig().baseURL;
    if (!urlStr.startsWith(baseURLstr)) {
      throw new Error(
        `[${urlStr}] does not start with base url [${baseURLstr}]`,
      );
    }
    const baseURL = new URL(baseURLstr);
    const url = new URL(urlStr);

    const endPath = decodeURIComponent(
      url.pathname.replace(baseURL.pathname, ""),
    );
    let segments = endPath.split("/");
    if (segments[0] === "") {
      segments = segments.slice(1);
    }
    if (segments[segments.length - 1] === "") {
      segments[segments.length - 1] = "WebHome";
    }
    const [bin, action] = segments;
    // TODO: the current approach is easy but does not work if some url rewriting is done in front of XWiki.
    if (bin == "bin" && action == "view") {
      segments = segments.slice(2);
      const pageName = segments[segments.length - 1];
      const spaces = segments.slice(0, segments.length - 1);
      return new DocumentReference(
        pageName,
        new SpaceReference(undefined, ...spaces),
      );
    } else if (bin == "bin" && action == "download") {
      segments = segments.slice(2);
      const attachmentName = segments[segments.length - 1];
      const pageName = segments[segments.length - 2];
      const spaces = segments.slice(0, segments.length - 2);
      return new AttachmentReference(
        attachmentName,
        new DocumentReference(
          pageName,
          new SpaceReference(undefined, ...spaces),
        ),
      );
    } else {
      throw new Error(`Impossible to resolve [${urlStr}] to an entity`);
    }
  }
}

export { XWikiRemoteURLParser };
