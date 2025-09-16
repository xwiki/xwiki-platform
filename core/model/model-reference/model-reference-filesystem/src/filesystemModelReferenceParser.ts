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
import type {
  ModelReferenceParser,
  ModelReferenceParserOptions,
} from "@xwiki/cristal-model-reference-api";

@injectable()
export class FileSystemModelReferenceParser implements ModelReferenceParser {
  constructor(
    @inject("DocumentService")
    private readonly documentService: DocumentService,
  ) {}

  parse(
    reference: string,
    options?: ModelReferenceParserOptions,
  ): EntityReference {
    if (/^https?:\/\//.test(reference)) {
      throw new Error(`[${reference}] is not a valid entity reference`);
    }
    const isAbsolute = options?.relative === false;
    const segments = reference.split(/(?<!\\)\//).map((s) => this.unescape(s));
    const isAttachment = this.isAttachmentReference(segments);
    if (isAttachment && isAbsolute) {
      return this.parseAttachmentAbsolute(segments);
    } else if (isAttachment && !isAbsolute) {
      return this.parseAttachmentRelative(segments);
    } else if (isAbsolute) {
      return this.parseDocumentAbsolute(segments);
    } else {
      return this.parseDocumentRelative(segments);
    }
  }

  private isAttachmentReference(segments: string[]) {
    return (
      segments[segments.length - 2] == "attachments" &&
      segments[segments.length - 3].startsWith(".")
    );
  }

  async parseAsync(reference: string): Promise<EntityReference> {
    return this.parse(reference);
  }
  private parseAttachmentRelative(segments: string[]): AttachmentReference {
    const currentDocumentSegments = this.documentService
      .getCurrentDocument()
      .value?.id.split("/");

    const spaces = segments.slice(0, segments.length - 3);

    const currentSegments =
      currentDocumentSegments?.slice(0, currentDocumentSegments?.length - 1) ??
      [];

    while (spaces[0] == "..") {
      currentSegments.pop();
      spaces.shift();
    }

    return new AttachmentReference(
      segments[segments.length - 1],
      new DocumentReference(
        segments[segments.length - 3].replace(/^\./, ""),
        new SpaceReference(undefined, ...[...currentSegments, ...spaces]),
      ),
    );
  }

  private parseAttachmentAbsolute(segments: string[]): AttachmentReference {
    const spaces = segments.slice(0, segments.length - 3);
    return new AttachmentReference(
      segments[segments.length - 1],
      new DocumentReference(
        segments[segments.length - 3].replace(/^\./, ""),
        new SpaceReference(undefined, ...spaces),
      ),
    );
  }

  private parseDocumentAbsolute(segments: string[]): DocumentReference {
    const spaces = segments.slice(0, segments.length - 1);
    return new DocumentReference(
      segments[segments.length - 1].replace(/\.md$/, ""),
      new SpaceReference(undefined, ...spaces),
    );
  }

  private parseDocumentRelative(segments: string[]): DocumentReference {
    const currentDocumentSegments = this.documentService
      .getCurrentDocument()
      .value?.id.split("/");

    const spaces = segments.slice(0, segments.length - 1);

    const currentSegments =
      currentDocumentSegments?.slice(0, currentDocumentSegments?.length - 1) ??
      [];

    while (spaces[0] == "..") {
      currentSegments.pop();
      spaces.shift();
    }

    return new DocumentReference(
      this.normalizeDocumentName(segments),
      new SpaceReference(undefined, ...[...currentSegments, ...spaces]),
    );
  }

  private normalizeDocumentName(segments: string[]) {
    return segments[segments.length - 1].replace(/\.md$/, "");
  }

  private unescape(string: string) {
    let ret = "";
    for (let index = 0; index < string.length; index++) {
      const { index: newIndex, char } = this.unescapeLocal(index, string);
      index = newIndex;
      ret += char;
    }
    return decodeURIComponent(ret);
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
