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
import { DepsContainerContext } from "../../contexts";
import { Button, FileInput, VisuallyHidden } from "@mantine/core";
import {
  AttachmentReference,
  DocumentReference,
} from "@xwiki/platform-model-api";
import { useCallback, useContext, useRef } from "react";
import { useTranslation } from "react-i18next";
import type { AttachmentsService } from "@xwiki/platform-attachments-api";
import type { DocumentService } from "@xwiki/platform-document-api";
import type { ModelReferenceParserProvider } from "@xwiki/platform-model-reference-api";
import type { RemoteURLSerializerProvider } from "@xwiki/platform-model-remote-url-api";

export type ImageUploadButtonProps = {
  onUploaded: (url: string) => void;
};

export function ImageUploadButton({ onUploaded }: ImageUploadButtonProps) {
  const depsContainer = useContext(DepsContainerContext)!;

  const remoteURLSerializer = depsContainer
    .get<RemoteURLSerializerProvider>("RemoteURLSerializerProvider")
    .get()!;

  const modelReferenceParser = depsContainer
    .get<ModelReferenceParserProvider>("ModelReferenceParserProvider")
    .get()!;

  const documentService = depsContainer.get<DocumentService>("DocumentService");

  const attachmentsService =
    depsContainer.get<AttachmentsService>("AttachmentsService")!;

  const fileUploadRef = useRef<HTMLButtonElement>(null);

  const { t } = useTranslation();

  const triggerUpload = useCallback(() => {
    fileUploadRef?.current?.click();
  }, [fileUploadRef]);

  const fileSelected = useCallback(
    async (file: File) => {
      const currentPageName =
        documentService.getCurrentDocumentReferenceString().value ?? "";

      const uploadedFilesUrls = await attachmentsService.upload(
        currentPageName,
        [file],
      );

      let url: string | undefined;
      if (uploadedFilesUrls && uploadedFilesUrls[0]) {
        url = uploadedFilesUrls[0];
      } else {
        const parser = modelReferenceParser?.parse(currentPageName, {
          relative: false,
        });

        url = remoteURLSerializer?.serialize(
          new AttachmentReference(file.name, parser as DocumentReference),
        );
      }

      if (url) {
        onUploaded(url);
      }
    },
    [onUploaded],
  );

  return (
    <>
      <Button variant="default" onClick={triggerUpload}>
        {t("blocknote.imageSelector.uploadButton")}
      </Button>

      <VisuallyHidden>
        <FileInput
          ref={fileUploadRef}
          accept="image/*"
          onChange={(file) => file && fileSelected(file)}
        />
      </VisuallyHidden>
    </>
  );
}
