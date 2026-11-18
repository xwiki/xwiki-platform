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
import { getSourceDocument } from "../misc/sourceDocument";
import { AttachmentReference } from "@xwiki/platform-model-api";
import type { EditorInstance } from "../editorInstance";
import type { AttachmentsService } from "@xwiki/platform-attachments-api";
import type { DocumentReference } from "@xwiki/platform-model-api";
import type {
  ModelReferenceParserProvider,
  ModelReferenceSerializerProvider,
} from "@xwiki/platform-model-reference-api";

/**
 * The name under which the {@link FileUploader} is bound in the service container.
 *
 * @since 18.10.0RC1
 * @beta
 */
const fileUploaderName = "FileUploader";

/**
 * A file stored by the {@link FileUploader}.
 *
 * @since 18.10.0RC1
 * @beta
 */
type UploadedFile = {
  /**
   * The URL of the stored file, e.g. to display it.
   */
  url: string;

  /**
   * The name the file is stored under, which can differ from the name of the uploaded file (e.g. when the storage
   * renames it).
   */
  name: string;

  /**
   * The absolute serialized reference of the attachment holding the file, when the storage can provide it. The
   * content then points to the attachment rather than to its URL.
   */
  reference?: string;
};

/**
 * Stores the files the user uploads, whatever the way they reach the editor (pasted, dropped, or picked from an upload
 * action), as attachments of the source document of the edited content. It only stores the files: what the editor
 * does with them (e.g. insert an image, or a link) is up to the editor.
 *
 * When it isn't bound in the service container (see {@link fileUploaderName}), the files are attached with the
 * `AttachmentsService`, when there's one.
 *
 * @since 18.10.0RC1
 * @beta
 */
interface FileUploader {
  /**
   * Stores the given file. A failure is reported to the user by the uploader, when it can, and to the caller by
   * rejecting.
   *
   * @param instance - the editor instance where the file is uploaded
   * @param file - the file to store
   * @returns the stored file
   */
  upload(instance: EditorInstance, file: File): Promise<UploadedFile>;
}

/**
 * Attaches the files to the source document of the edited content (the current document when not specified) with the
 * `AttachmentsService`.
 */
const defaultFileUploader: FileUploader = {
  async upload(instance, file) {
    const { serviceContainer } = instance;
    const page = getSourceDocument(instance);
    const [attachment] = await serviceContainer
      .get<AttachmentsService>("AttachmentsService")
      .upload(page, [file]);
    if (!attachment) {
      throw new Error(`Failed to attach [${file.name}].`);
    }
    const documentReference = serviceContainer
      .get<ModelReferenceParserProvider>("ModelReferenceParserProvider")
      .get()
      ?.parse(page, { relative: false }) as DocumentReference | undefined;
    return {
      url: attachment.href,
      name: attachment.name,
      reference:
        documentReference &&
        serviceContainer
          .get<ModelReferenceSerializerProvider>(
            "ModelReferenceSerializerProvider",
          )
          .get()
          ?.serialize(
            new AttachmentReference(attachment.name, documentReference),
          ),
    };
  },
};

/**
 * Looks up the file uploader of an editor instance: the one bound in its service container, or a default one attaching
 * the files with the `AttachmentsService`.
 *
 * @param instance - the editor instance, or what is known about it before its editor is created
 * @returns the file uploader of the given editor instance, or undefined when the user can't upload files: uploading is
 *   disabled by the `upload` option, or there's nothing to store the files with
 * @since 18.10.0RC1
 * @beta
 */
function getFileUploader(
  instance: Pick<EditorInstance, "options" | "serviceContainer">,
): FileUploader | undefined {
  const { options, serviceContainer } = instance;
  if (options.upload === false) {
    return undefined;
  } else if (serviceContainer.isBound(fileUploaderName)) {
    return serviceContainer.get<FileUploader>(fileUploaderName);
  } else if (serviceContainer.isBound("AttachmentsService")) {
    return defaultFileUploader;
  }
  return undefined;
}

export { fileUploaderName, getFileUploader };
export type { FileUploader, UploadedFile };
