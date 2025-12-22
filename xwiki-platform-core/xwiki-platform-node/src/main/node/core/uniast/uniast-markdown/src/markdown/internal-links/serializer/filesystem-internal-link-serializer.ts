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
import { AttachmentReference, EntityType } from "@xwiki/platform-model-api";
import { inject, injectable } from "inversify";
import type { InternalLinksSerializer } from "./internal-links-serializer";
import type { UniAstToMarkdownConverter } from "../../uni-ast-to-markdown-converter";
import type { DocumentService } from "@xwiki/platform-document-api";
import type { EntityReference } from "@xwiki/platform-model-api";
import type { Link, LinkTarget } from "@xwiki/platform-uniast-api";

/**
 * @since 18.0.0RC1
 */
@injectable()
export class FilesystemInternalLinkSerializer
  implements InternalLinksSerializer
{
  constructor(
    @inject("DocumentService")
    private readonly documentService: DocumentService,
  ) {}

  async serialize(
    content: Link["content"],
    target: Extract<LinkTarget, { type: "internal" }>,
    uniAstToMarkdownConverter: UniAstToMarkdownConverter,
  ): Promise<string> {
    const linkText = `${await uniAstToMarkdownConverter.convertInlineContents(
      content,
    )}`;
    return `[${linkText}](${this.serializeTarget(target)})`;
  }

  async serializeImage(
    target: Extract<LinkTarget, { type: "internal" }>,
    alt?: string,
  ): Promise<string> {
    return `![${alt ?? ""}](${this.serializeTarget(target)})`;
  }

  // eslint-disable-next-line max-statements
  private serializeTarget(
    target: Extract<
      {
        type: "internal";
        rawReference: string;
        parsedReference: EntityReference | null;
      },
      { type: "internal" }
    >,
  ) {
    if (target.parsedReference) {
      const currentDocumentReference =
        this.documentService.getCurrentDocumentReference().value!;
      let parsedReference = target.parsedReference;
      const isAttachment = parsedReference.type === EntityType.ATTACHMENT;
      if (isAttachment) {
        parsedReference = (parsedReference as AttachmentReference).document;
      } else if (parsedReference.type !== EntityType.DOCUMENT) {
        throw new Error(
          `Unexpected type ${parsedReference.type} for link serialization`,
        );
      }

      const down = [
        ...(currentDocumentReference.space &&
        currentDocumentReference.space.names.length > 0
          ? currentDocumentReference.space.names.map(() => "..")
          : ["."]),
      ].join("/");

      const name = (isAttachment ? "." : "") + parsedReference.name;
      const up = [...(parsedReference.space?.names ?? []), name]
        .map(encodeURI)
        .join("/");
      if (!isAttachment) {
        return `${down}/${up}.md`.replace(/^\.\//, "");
      } else {
        return `${down}/${up}/attachments/${encodeURI((target.parsedReference as AttachmentReference).name)}`.replace(
          /^\.\//,
          "",
        );
      }
    } else {
      return target.rawReference;
    }
  }
}
