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

import { USERNAME } from "@xwiki/cristal-authentication-nextcloud";
import {
  AttachmentReference,
  DocumentReference,
  EntityReference,
  SpaceReference,
} from "@xwiki/cristal-model-api";
import { RemoteURLParser } from "@xwiki/cristal-model-remote-url-api";
import { inject, injectable } from "inversify";
import type { CristalApp } from "@xwiki/cristal-api";

@injectable()
class NextcloudRemoteURLParser implements RemoteURLParser {
  constructor(
    @inject<CristalApp>("CristalApp") private readonly cristalApp: CristalApp,
  ) {}

  parse(urlStr: string): EntityReference | undefined {
    const baseRestURL = this.getWikiConfig().baseRestURL;
    if (urlStr.includes("://") && !urlStr.startsWith(baseRestURL)) {
      return undefined;
    }
    urlStr = urlStr.replace(`${baseRestURL}/${USERNAME}/.cristal/`, "");
    const segments = this.computeSegments(urlStr);

    if (
      segments.length >= 3 &&
      segments[segments.length - 2] == "attachments"
    ) {
      return new AttachmentReference(
        segments[segments.length - 1],
        this.buildDocumentReference(
          segments[segments.length - 3],
          segments.splice(0, segments.length - 3),
        ),
      );
    } else {
      return this.buildDocumentReference(
        segments[segments.length - 1],
        segments.splice(0, segments.length - 1),
      );
    }
  }

  private computeSegments(urlStr: string) {
    let segments = decodeURIComponent(urlStr).split("/");
    if (segments[0] === "" || segments[0] === ".") {
      segments = segments.slice(1);
    }

    if (segments[segments.length - 1] === "") {
      segments = segments.slice(0, segments.length - 1);
    }
    return segments;
  }

  private buildDocumentReference(
    documentReferenceName: string,
    spaces: string[],
  ) {
    return new DocumentReference(
      documentReferenceName,
      new SpaceReference(undefined, ...spaces),
    );
  }

  private getWikiConfig() {
    return this.cristalApp.getWikiConfig();
  }
}

export { NextcloudRemoteURLParser };
