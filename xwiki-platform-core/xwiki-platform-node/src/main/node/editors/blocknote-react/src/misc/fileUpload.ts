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
import { insertOrUpdateBlockForSlashMenu } from "@blocknote/core";
import {
  AttachmentReference,
  DocumentReference,
} from "@xwiki/platform-model-api";
import { useCallback } from "react";
import type { EditorType } from "../blocknote";
import type { AttachmentsService } from "@xwiki/platform-attachments-api";
import type { DocumentService } from "@xwiki/platform-document-api";
import type { ModelReferenceParserProvider } from "@xwiki/platform-model-reference-api";
import type { RemoteURLSerializerProvider } from "@xwiki/platform-model-remote-url-api";
import type { Container } from "inversify";

// eslint-disable-next-line max-statements
async function uploadSelectedFile(
  file: File,
  depsContainer: Container,
): Promise<{ url: string }> {
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
    return { url: uploadedFilesUrls[0] };
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

  return { url };
}

/**
 * Opens the system file picker and uploads the selected file as an attachment of the current page.
 *
 * @param depsContainer - the container providing the upload services
 * @param accept - the file types the file picker offers, e.g. "image/*" (all types when not specified)
 * @returns the URL of the uploaded file, or `undefined` when the user closes the file picker without
 *   selecting a file (rejected when the upload fails)
 */
function triggerUserFileUpload(
  depsContainer: Container,
  accept?: string,
): Promise<{ url: string } | undefined> {
  return new Promise((resolve, reject) => {
    // Creates a hidden file input element and temporarily adds it to the DOM,
    // so we can open the system file picker by clicking it. See
    // https://developer.mozilla.org/en-US/docs/Web/API/File_API/Using_files_from_web_applications#using_hidden_file_input_elements_using_the_click_method
    const input = document.createElement("input");
    input.type = "file";
    if (accept) {
      input.accept = accept;
    }
    input.style.display = "none";

    input.addEventListener("change", async () => {
      const file = input.files?.[0];
      input.remove();

      if (!file) {
        resolve(undefined);
        return;
      }

      try {
        resolve(await uploadSelectedFile(file, depsContainer));
      } catch (error) {
        reject(error);
      }
    });

    // Removes the input from the DOM again if the user closes the file
    // picker without selecting a file.
    input.addEventListener("cancel", () => {
      input.remove();
      resolve(undefined);
    });

    document.body.appendChild(input);
    input.click();
  });
}

/**
 * Opens the system file picker for images and uploads the selected image as an attachment of the
 * current page.
 *
 * Meant for UI event handlers, which don't handle the returned promise: an upload failure is logged
 * rather than rejected.
 *
 * @param depsContainer - the container providing the upload services
 * @returns the URL of the uploaded image, or `undefined` when the user closes the file picker without
 *   selecting an image or when the upload fails
 */
async function triggerUserImageUpload(
  depsContainer: Container,
): Promise<string | undefined> {
  try {
    return (await triggerUserFileUpload(depsContainer, "image/*"))?.url;
  } catch (error) {
    console.error("Failed to upload the selected image.", error);
    return undefined;
  }
}

function useImageUploader(editor: EditorType, depsContainer: Container) {
  const uploadImage = useCallback(async () => {
    const url = await triggerUserImageUpload(depsContainer);

    if (url) {
      insertOrUpdateBlockForSlashMenu(editor, {
        type: "image",
        props: { url },
      });
    }
  }, [depsContainer, editor]);

  return uploadImage;
}

export {
  triggerUserFileUpload,
  triggerUserImageUpload,
  uploadSelectedFile,
  useImageUploader,
};
