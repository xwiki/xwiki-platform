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
  EntityReference,
  SpaceReference,
} from "@xwiki/cristal-model-api";
import { ModelReferenceParser } from "@xwiki/cristal-model-reference-api";
import { injectable } from "inversify";

@injectable()
export class FileSystemModelReferenceParser implements ModelReferenceParser {
  parse(reference: string): EntityReference {
    const segments = reference.split(/(?<!\\)\//);
    if (segments[segments.length - 2] == "attachments") {
      return new AttachmentReference(
        this.unescape(segments[segments.length - 1]),
        new DocumentReference(
          this.unescape(segments[segments.length - 3]),
          new SpaceReference(
            undefined,
            ...segments
              .slice(0, segments.length - 3)
              .map((s) => this.unescape(s)),
          ),
        ),
      );
    } else {
      return new DocumentReference(
        this.unescape(segments[segments.length - 1]),
        new SpaceReference(
          undefined,
          ...segments
            .slice(0, segments.length - 1)
            .map((s) => this.unescape(s)),
        ),
      );
    }
  }

  private unescape(string: string) {
    let ret = "";
    for (let index = 0; index < string.length; index++) {
      const { index: newIndex, char } = this.unescapeLocal(index, string);
      index = newIndex;
      ret += char;
    }
    return ret;
  }

  private unescapeLocal(
    index: number,
    string: string,
  ): { char: string; index: number } {
    if (index < string.length - 1) {
      const c0 = string[index];
      const c1 = string[index + 1];
      if (c0 == "\\" && ["/", "\\"].includes(c1)) {
        return { char: c1, index: index + 1 };
      } else {
        return { char: c0, index };
      }
    } else if (index < string.length) {
      return { char: string[index], index };
    } else {
      return { char: "", index };
    }
  }
}
