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
import { EntityType } from "@xwiki/platform-model-api";
import { XMLParser } from "fast-xml-parser";
import { inject, injectable } from "inversify";
import type { InternalLinksSerializer } from "./internal-links-serializer";
import type { UniAstToMarkdownConverter } from "../../uni-ast-to-markdown-converter";
import type { CristalApp } from "@xwiki/platform-api";
import type { DocumentService } from "@xwiki/platform-document-api";
import type { RemoteURLSerializerProvider } from "@xwiki/platform-model-remote-url-api";
import type { Link, LinkTarget } from "@xwiki/platform-uniast-api";

/**
 * @since 18.0.0RC1
 */
@injectable()
export class NextcloudInternalLinkSerializer
  implements InternalLinksSerializer
{
  constructor(
    @inject("RemoteURLSerializerProvider")
    private readonly remoteURLSerializerProvider: RemoteURLSerializerProvider,
    @inject("CristalApp") private readonly cristalApp: CristalApp,
    @inject("DocumentService")
    private readonly documentService: DocumentService,
  ) {}

  async serialize(
    content: Link["content"],
    target: Extract<LinkTarget, { type: "internal" }>,
    uniAstToMarkdownConverter: UniAstToMarkdownConverter,
  ): Promise<string> {
    const label =
      await uniAstToMarkdownConverter.convertInlineContents(content);
    const urlFromReference = this.remoteURLSerializerProvider
      .get()!
      .serialize(target.parsedReference ?? undefined)!;
    const response = await fetch(urlFromReference, {
      method: "PROPFIND",
      body: `<?xml version="1.0" encoding="UTF-8"?>
 <d:propfind xmlns:d="DAV:">
   <d:prop xmlns:oc="http://owncloud.org/ns">
    <oc:fileid/>
   </d:prop>
 </d:propfind>`,
      headers: {
        Authorization: `Basic ${btoa("admin:admin")}`,
      },
    });
    const xml = new XMLParser().parse(await response.text());
    const fileId =
      xml["d:multistatus"]["d:response"]["d:propstat"]["d:prop"]["oc:fileid"];
    const baseURL = this.cristalApp.getWikiConfig().baseURL;
    const url = `${baseURL}/f/${fileId}`;
    return `[${label}](${url})`;
  }

  // eslint-disable-next-line max-statements
  async serializeImage(
    target: Extract<LinkTarget, { type: "internal" }>,
    alt?: string,
  ): Promise<string> {
    let ref: string;
    if (target.parsedReference) {
      const currentDocumentReference =
        this.documentService.getCurrentDocumentReference().value!;
      if (target.parsedReference.type == EntityType.ATTACHMENT) {
        const parsedReference = target.parsedReference;
        const imageDocumentReference = parsedReference.document;
        if (currentDocumentReference === imageDocumentReference) {
          ref = `.${imageDocumentReference.name}/attachments/${parsedReference.name}`;
        } else {
          const down = [
            ...(currentDocumentReference.space &&
            currentDocumentReference.space.names.length > 0
              ? currentDocumentReference.space.names.map(() => "..")
              : ["."]),
          ].join("/");
          const up = [
            ...(parsedReference.document.space?.names ?? []),
            "." + parsedReference.document.name,
          ]
            .map(encodeURI)
            .join("/");
          ref = `${down}/${up}/attachments/${encodeURI(parsedReference.name)}`;
        }
      } else {
        throw new Error(
          `Unexpected type ${target.parsedReference.type} for link serialization`,
        );
      }
    } else {
      ref = target.rawReference;
    }

    return `![${alt ?? ""}](${ref})`;
  }
}
