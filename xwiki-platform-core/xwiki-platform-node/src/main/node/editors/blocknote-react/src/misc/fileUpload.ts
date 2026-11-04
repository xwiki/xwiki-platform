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
import { toResourceReference } from "./resourceReference";
import { LinkType } from "@xwiki/platform-link-suggest-api";
import { AttachmentReference } from "@xwiki/platform-model-api";
import type {
  Attachment,
  AttachmentsService,
} from "@xwiki/platform-attachments-api";
import type { DocumentService } from "@xwiki/platform-document-api";
import type { DocumentReference } from "@xwiki/platform-model-api";
import type {
  ModelReferenceParserProvider,
  ModelReferenceSerializerProvider,
} from "@xwiki/platform-model-reference-api";
import type {
  ResourceReference,
  ResourceReferenceParser,
} from "@xwiki/platform-rendering-api";
import type { Container } from "inversify";

/**
 * An image uploaded by the user, ready to be inserted into the content.
 */
type UploadedImage = {
  url: string;
  /**
   * The resource reference of the uploaded image, when its attachment reference can be serialized.
   */
  reference?: ResourceReference;
};

/**
 * Opens the system file picker and uploads the selected file as an attachment of the given page.
 *
 * @param depsContainer - the container providing the upload services
 * @param page - the serialized reference of the page to upload the file to, the current page when not specified
 * @param accept - the file types the file picker offers, e.g. "image/*" (all types when not specified)
 * @returns the uploaded attachment, or `undefined` when the user closes the file picker without selecting a file
 *   (rejected when the upload fails)
 */
function triggerUserFileUpload(
  depsContainer: Container,
  page: string | undefined,
  accept?: string,
): Promise<Attachment | undefined> {
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
        const [uploadedAttachment] = await depsContainer
          .get<AttachmentsService>("AttachmentsService")
          .upload(page ?? getCurrentPage(depsContainer), [file]);
        resolve(uploadedAttachment);
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
 * given page.
 *
 * Meant for UI event handlers, which don't handle the returned promise: an upload failure is logged
 * rather than rejected.
 *
 * @param depsContainer - the container providing the upload services
 * @param page - the serialized reference of the page to upload the image to, the current page when not specified
 * @returns the uploaded image, or `undefined` when the user closes the file picker without selecting an
 *   image or when the upload fails
 */
async function triggerUserImageUpload(
  depsContainer: Container,
  page?: string,
): Promise<UploadedImage | undefined> {
  const targetPage = page ?? getCurrentPage(depsContainer);
  let uploadedAttachment;
  try {
    uploadedAttachment = await triggerUserFileUpload(
      depsContainer,
      targetPage,
      "image/*",
    );
  } catch (error) {
    console.error("Failed to upload the selected image.", error);
    return undefined;
  }

  if (!uploadedAttachment) {
    return undefined;
  }

  return {
    url: uploadedAttachment.href,
    reference: getAttachmentResourceReference(
      depsContainer,
      targetPage,
      uploadedAttachment.name,
    ),
  };
}

/**
 * @param depsContainer - the container providing the document service
 * @returns the serialized reference of the current page
 */
function getCurrentPage(depsContainer: Container): string {
  return (
    depsContainer
      .get<DocumentService>("DocumentService")
      .getCurrentDocumentReferenceString().value ?? ""
  );
}

/**
 * @param depsContainer - the container providing the model reference services
 * @param page - the serialized reference of the page holding the attachment
 * @param name - the name of the attachment
 * @returns the resource reference of the specified attachment, or `undefined` when it can't be serialized
 */
function getAttachmentResourceReference(
  depsContainer: Container,
  page: string,
  name: string,
): ResourceReference | undefined {
  const pageReference = depsContainer
    .get<ModelReferenceParserProvider>("ModelReferenceParserProvider")
    .get()
    ?.parse(page, { relative: false }) as DocumentReference | undefined;
  const serializedReference = depsContainer
    .get<ModelReferenceSerializerProvider>("ModelReferenceSerializerProvider")
    .get()
    ?.serialize(new AttachmentReference(name, pageReference!));
  return serializedReference === undefined
    ? undefined
    : toResourceReference(
        depsContainer.get<ResourceReferenceParser>("ResourceReferenceParser"),
        serializedReference,
        LinkType.ATTACHMENT,
      );
}

export { triggerUserFileUpload, triggerUserImageUpload };
export type { UploadedImage };
