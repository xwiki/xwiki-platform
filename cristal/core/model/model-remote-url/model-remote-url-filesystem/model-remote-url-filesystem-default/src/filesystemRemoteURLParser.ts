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
} from "@xwiki/cristal-model-api";
import { protocol } from "@xwiki/cristal-model-remote-url-filesystem-api";
import { injectable } from "inversify";
import type { EntityReference } from "@xwiki/cristal-model-api";
import type { RemoteURLParser } from "@xwiki/cristal-model-remote-url-api";

@injectable()
class FileSystemRemoteURLParser implements RemoteURLParser {
  // TODO: reduce the number of statements in the following method and reactivate the disabled eslint rule.
  // eslint-disable-next-line max-statements
  parse(urlStr: string): EntityReference | undefined {
    const startWithFilesystemProtocol = urlStr.startsWith(`${protocol}://`);
    if (!startWithFilesystemProtocol && urlStr.includes("://")) {
      return undefined;
    }

    if (startWithFilesystemProtocol) {
      urlStr = urlStr.split("://", 2)[1];
    }

    let segments = urlStr.split("/");
    if (segments[0] === "" || segments[0] === ".") {
      segments = segments.slice(1);
    }

    if (segments[segments.length - 1] === "") {
      segments = segments.slice(0, segments.length - 1);
    }

    segments = segments.map(decodeURIComponent);

    // Hidden elements are only allowed in the path of attachments (e.g., a/.b/file_1.png)
    if (segments.some((e) => e.startsWith("."))) {
      return new AttachmentReference(
        segments[segments.length - 1],
        new DocumentReference(
          // Remove the starting dot from the hidden directory containing the document attached metadata
          segments[segments.length - 3].slice(1),
          new SpaceReference(
            undefined,
            ...segments.splice(0, segments.length - 3),
          ),
        ),
      );
    } else {
      return new DocumentReference(
        this.removeExtension(segments[segments.length - 1]),
        new SpaceReference(
          undefined,
          ...segments.splice(0, segments.length - 1),
        ),
      );
    }
  }

  private removeExtension(file: string): string {
    if (!file.includes(".")) {
      return file;
    }
    return file.slice(0, file.lastIndexOf("."));
  }
}

export { FileSystemRemoteURLParser };
