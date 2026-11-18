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
import { canUploadFiles, uploadFile } from "./fileUploader";
import { suggestedLinkData } from "../components/links/suggestedLink";
import { pickFile } from "../misc/filePicker";
import { toResourceReference } from "../misc/resourceReference";
import { LinkType } from "@xwiki/platform-link-suggest-api";
import type { LinkEditionData } from "../components/links/linkEditor";
import type { EditorInstance } from "../editorInstance";
import type { UploadedFile } from "./fileUploader";
import type {
  ResourceReference,
  ResourceReferenceParser,
} from "@xwiki/platform-rendering-api";

/**
 * @param instance - the editor instance where the image is inserted
 * @param uploadedFile - an uploaded image
 * @returns the URL of the uploaded image, and its resource reference when known
 */
function uploadedImage(
  instance: EditorInstance,
  uploadedFile: UploadedFile,
): { url: string; reference?: ResourceReference } {
  return {
    url: uploadedFile.url,
    reference:
      uploadedFile.reference === undefined
        ? undefined
        : toResourceReference(
            instance.serviceContainer.get<ResourceReferenceParser>(
              "ResourceReferenceParser",
            ),
            uploadedFile.reference,
            LinkType.ATTACHMENT,
          ),
  };
}

/**
 * @param instance - the editor instance where the link is inserted
 * @param uploadedFile - an uploaded file
 * @returns the link to the uploaded file, labeled with the name of the file
 */
function uploadedFileLink(
  instance: EditorInstance,
  uploadedFile: UploadedFile,
): LinkEditionData {
  return suggestedLinkData(instance, {
    label: uploadedFile.name,
    url: uploadedFile.url,
    reference: uploadedFile.reference,
    type: LinkType.ATTACHMENT,
  });
}

/**
 * Opens the system file picker and uploads the selected file with the file uploader of the given editor instance.
 *
 * Meant for UI event handlers, which don't handle the returned promise: an upload failure is logged rather than
 * rejected (the file upload listeners can report it to the user).
 *
 * @param instance - the editor instance where the file is uploaded
 * @param accept - the file types the file picker offers, e.g. "image/*" (all types when not specified)
 * @param failureMessage - the message logged when the upload fails
 * @returns the uploaded file, or `undefined` when the user closes the file picker without selecting a file, when the
 *   upload fails, or when the user can't upload files
 */
async function pickAndUploadFile(
  instance: EditorInstance,
  accept: string | undefined,
  failureMessage: string,
): Promise<UploadedFile | undefined> {
  const file = canUploadFiles(instance) && (await pickFile(accept));
  if (!file) {
    return undefined;
  }
  try {
    return await uploadFile(instance, file);
  } catch (error) {
    console.error(failureMessage, error);
    return undefined;
  }
}

export { pickAndUploadFile, uploadedFileLink, uploadedImage };
