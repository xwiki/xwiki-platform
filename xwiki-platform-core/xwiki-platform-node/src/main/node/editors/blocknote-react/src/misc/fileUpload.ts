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
import {
  AttachmentReference,
  DocumentReference,
} from "@xwiki/platform-model-api";
import type { AttachmentsService } from "@xwiki/platform-attachments-api";
import type { DocumentService } from "@xwiki/platform-document-api";
import type { ModelReferenceParserProvider } from "@xwiki/platform-model-reference-api";
import type { RemoteURLSerializerProvider } from "@xwiki/platform-model-remote-url-api";
import type { Container } from "inversify";

// eslint-disable-next-line max-statements
export async function uploadFile(
  file: File,
  depsContainer: Container,
): Promise<string> {
  const remoteURLSerializer = depsContainer
    .get<RemoteURLSerializerProvider>("RemoteURLSerializerProvider")
    .get()!;

  const modelReferenceParser = depsContainer
    .get<ModelReferenceParserProvider>("ModelReferenceParserProvider")
    .get()!;

  const documentService = depsContainer.get<DocumentService>("DocumentService");

  const attachmentsService =
    depsContainer.get<AttachmentsService>("AttachmentsService")!;

  const currentPageName =
    documentService.getCurrentDocumentReferenceString().value ?? "";

  const uploadedFilesUrls = await attachmentsService.upload(currentPageName, [
    file,
  ]);

  if (uploadedFilesUrls && uploadedFilesUrls[0]) {
    return uploadedFilesUrls[0];
  }

  const parser = modelReferenceParser?.parse(currentPageName, {
    relative: false,
  });

  const url = remoteURLSerializer?.serialize(
    new AttachmentReference(file.name, parser as DocumentReference),
  );

  if (!url) {
    throw new Error("Internal error: could not get URL for uploaded file");
  }

  return url;
}
