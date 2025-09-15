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
import { inject, injectable } from "inversify";
import type { DocumentService } from "@xwiki/cristal-document-api";
import type { EntityReference } from "@xwiki/cristal-model-api";
import type { ModelReferenceParser } from "@xwiki/cristal-model-reference-api";

@injectable()
export class FileSystemModelReferenceParser implements ModelReferenceParser {
  constructor(
    @inject("DocumentService")
    private readonly documentService: DocumentService,
  ) {}

  // eslint-disable-next-line max-statements
  parse(reference: string): EntityReference {
    if (/^https?:\/\//.test(reference)) {
      throw new Error(`[${reference}] is not a valid entity reference`);
    }
    const segments = reference.split(/(?<!\\)\//);
    if (
      segments[segments.length - 2] == "attachments" &&
      segments[segments.length - 3].startsWith(".")
    ) {
      const currentDocumentSegments = this.documentService
        .getCurrentDocument()
        .value?.id.split("/");

      const references = reference
        .split(/(?<!\\)\//)
        .map((s) => this.unescape(s));
      const spaces = references.slice(0, references.length - 3);

      const currentSegments =
        currentDocumentSegments?.slice(
          0,
          currentDocumentSegments?.length - 1,
        ) ?? [];

      while (spaces[0] == "..") {
        currentSegments.pop();
        spaces.pop();
      }

      return new AttachmentReference(
        this.unescape(segments[segments.length - 1]),
        new DocumentReference(
          this.unescape(segments[segments.length - 3]).replace(/^\./, ""),
          new SpaceReference(undefined, ...[...currentSegments, ...spaces]),
        ),
      );
    } else {
      const currentDocumentSegments = this.documentService
        .getCurrentDocument()
        .value?.id.split("/");

      const references = reference
        .split(/(?<!\\)\//)
        .map((s) => this.unescape(s));
      const spaces = references.slice(0, references.length - 1);

      const currentSegments =
        currentDocumentSegments?.slice(
          0,
          currentDocumentSegments?.length - 1,
        ) ?? [];

      while (spaces[0] == "..") {
        currentSegments.pop();
        spaces.pop();
      }

      return new DocumentReference(
        this.unescape(segments[segments.length - 1]),
        new SpaceReference(undefined, ...[...currentSegments, ...spaces]),
      );
    }
  }

  async parseAsync(reference: string): Promise<EntityReference> {
    return this.parse(reference);
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
