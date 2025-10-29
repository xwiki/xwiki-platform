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

import {
  AttachmentReference,
  DocumentReference,
  SpaceReference,
  WikiReference,
} from "@xwiki/cristal-model-api";
import { inject, injectable } from "inversify";
import type { CristalApp } from "@xwiki/cristal-api";
import type { EntityReference } from "@xwiki/cristal-model-api";
import type { RemoteURLParser } from "@xwiki/cristal-model-remote-url-api";

@injectable()
class NextcloudRemoteURLParser implements RemoteURLParser {
  constructor(@inject("CristalApp") private readonly cristalApp: CristalApp) {}

  parse(urlStr: string): EntityReference | undefined {
    const config = this.getWikiConfig();

    // urlMatch will contain either:
    //   - [<matchedRemoteUrl>, <username>, <resourceSegments>]
    //   - [<macthedRemoteUrl>, <resourceSegments>] (if no "${username}" in storageRoot)
    //   - undefined (if match failed)
    const urlMatch = [
      ...urlStr.matchAll(
        new RegExp(
          `^${config.baseRestURL}${
            config.storageRoot ?? "/files/${username}/.cristal"
          }/(.*)`.replace("${username}", "([^/]*)"),
          "g",
        ),
      ),
    ][0];

    if (urlMatch === undefined) {
      return undefined;
    }
    urlStr = urlMatch[urlMatch.length - 1];
    const segments = this.computeSegments(urlStr);

    if (
      segments.length >= 3 &&
      segments[segments.length - 2] == "attachments"
    ) {
      return this.buildAttachmentReference(
        segments,
        urlMatch.length == 3 ? urlMatch[1].toString() : undefined,
      );
    } else if (
      segments[segments.length - 1].endsWith(".md") &&
      segments[segments.length - 2] != "attachments"
    ) {
      return this.buildDocumentReference(
        segments,
        urlMatch.length == 3 ? urlMatch[1].toString() : undefined,
      );
    } else {
      return this.buildSpaceReference(
        segments,
        urlMatch.length == 3 ? urlMatch[1].toString() : undefined,
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

  private buildAttachmentReference(
    segments: string[],
    username: string | undefined,
  ) {
    const spaces = segments.slice(0, segments.length - 2);
    const newSpaces = [];
    newSpaces.push(...spaces.slice(0, spaces.length - 1));
    newSpaces.push(spaces[spaces.length - 1].slice(1));
    return new AttachmentReference(
      segments[segments.length - 1],
      this.buildDocumentReference(newSpaces, username),
    );
  }

  private buildDocumentReference(
    segments: string[],
    username: string | undefined,
  ) {
    return new DocumentReference(
      this.removeExtension(segments[segments.length - 1]),
      this.buildSpaceReference(
        segments.slice(0, segments.length - 1),
        username,
      ),
    );
  }

  private buildSpaceReference(spaces: string[], username: string | undefined) {
    return new SpaceReference(
      username ? new WikiReference(username) : undefined,
      ...spaces,
    );
  }

  private getWikiConfig() {
    return this.cristalApp.getWikiConfig();
  }

  private removeExtension(file: string): string {
    if (!file.includes(".")) {
      return file;
    }
    return file.slice(0, file.lastIndexOf("."));
  }
}

export { NextcloudRemoteURLParser };
