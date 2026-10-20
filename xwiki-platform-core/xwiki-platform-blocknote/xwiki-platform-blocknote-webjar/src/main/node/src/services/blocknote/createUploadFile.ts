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
import { injectLinkId } from "./linkId";
import { withUniqueName } from "../attachments/uniqueFileName";
import { EntityType } from "@xwiki/platform-model-api";
import { uuidv4 } from "lib0/random";
import type { BlockNoteDocument } from "./BlockNoteProcessor";
import type { StorageProvider } from "@xwiki/platform-backend-api";
import type { ModelReferenceParser } from "@xwiki/platform-model-reference-api";
import type { RemoteURLSerializer } from "@xwiki/platform-model-remote-url-api";
import type { ResourceReference } from "@xwiki/platform-rendering-api";

/**
 * What BlockNote applies to the block it inserted for the uploaded file. It accepts a whole partial block, not just
 * properties, which is how we can replace the file block BlockNote guessed with something else.
 */
type UploadResult = Record<string, unknown>;

/**
 * The services the upload callback needs.
 */
type UploadFileDependencies = {
  storageProvider: StorageProvider;
  modelReferenceParser: ModelReferenceParser;
  remoteURLSerializer: RemoteURLSerializer;

  /**
   * The reference of the document being edited, which is where the files are attached. It is a function because the
   * editor is created before this is known.
   */
  getDocumentReference: () => string;

  /**
   * The document holding the metadata that the BlockNote schema can't store, such as the resource reference of the
   * uploaded attachment. It is a function because the document is loaded after the editor is created.
   */
  getBlockNoteDocument: () => BlockNoteDocument | undefined;

  /**
   * Whether the files being uploaded were pasted rather than dropped.
   */
  isPaste: () => boolean;

  /**
   * Called when the upload fails, to tell the user about it.
   */
  notifyUploadFailed: (fileName: string, error: unknown) => void;
};

// Collapses the block BlockNote inserted for the file into an empty paragraph, leaving no trace of a failed upload.
const EMPTY_PARAGRAPH: UploadResult = { type: "paragraph", content: [] };

/**
 * The resource reference saved for an uploaded attachment. Images are untyped and links are typed, like the CKEditor
 * integration does, so that an image is saved as `image:name.png` and a link as `[[attach:name.pdf]]` rather than as
 * `[[name.pdf]]`, which would be a document link.
 *
 * @param reference - the reference of an attachment of the edited document
 * @param typed - whether the reference keeps its `attach:` prefix when saved
 * @returns the resource reference to store as block metadata
 */
function attachmentReference(
  reference: string,
  typed: boolean,
): ResourceReference {
  return { type: "attach", typed, reference, parameters: {} };
}

/**
 * Creates the callback BlockNote uses to upload the files that are pasted or dropped over the editor.
 *
 * The uploaded file becomes an attachment of the edited document and the block BlockNote inserted for it is updated to
 * reference that attachment, matching what the CKEditor integration produces: an image block for an image and a
 * free-standing attachment link for anything else.
 *
 * @param dependencies - the services the upload callback needs
 * @returns the callback to pass to BlockNote as the uploadFile option
 */
function createUploadFile(
  dependencies: UploadFileDependencies,
): (file: File, blockId?: string) => Promise<UploadResult> {
  const {
    storageProvider,
    modelReferenceParser,
    remoteURLSerializer,
    getDocumentReference,
    getBlockNoteDocument,
    isPaste,
    notifyUploadFailed,
  } = dependencies;

  /**
   * @param file - the file to attach to the edited document
   * @returns the reference of the created attachment, relative to the edited document
   */
  async function upload(file: File): Promise<string> {
    const result = await storageProvider
      .get()
      .saveAttachments(getDocumentReference(), [file]);
    const reference = result?.[0];
    if (!reference) {
      throw new Error(`Failed to attach [${file.name}].`);
    }
    return reference;
  }

  /**
   * @param reference - the reference of an attachment of the edited document
   * @returns the URL the browser can use to download that attachment
   */
  function getDownloadURL(reference: string): string {
    return (
      remoteURLSerializer.serialize(
        modelReferenceParser.parse(reference, { type: EntityType.ATTACHMENT }),
      ) ?? ""
    );
  }

  /**
   * Points the image block BlockNote inserted at the uploaded attachment.
   *
   * @param reference - the reference of the created attachment
   * @param blockId - the id of the block BlockNote inserted for the file
   * @returns the properties to apply to that block
   */
  function toImageBlock(reference: string, blockId?: string): UploadResult {
    if (blockId) {
      getBlockNoteDocument()!.getMetadata(blockId, true)!.xwikiReference =
        attachmentReference(reference, false);
    }
    // The empty name drops the alt text BlockNote defaults to the file name, which the CKEditor integration doesn't
    // set either.
    return { props: { url: getDownloadURL(reference), name: "" } };
  }

  /**
   * Replaces the file, video or audio block BlockNote inserted with a free-standing attachment link, because those
   * blocks have no XWiki counterpart. The link metadata is bound to a synthetic id carried in the link URL, because
   * links have no id of their own.
   *
   * @param reference - the reference of the created attachment
   * @param label - the link label, which is the name the attachment was created with
   * @returns the paragraph that replaces the block BlockNote inserted
   */
  function toAttachmentLink(reference: string, label: string): UploadResult {
    const linkId = uuidv4();
    const metadata = getBlockNoteDocument()!.getMetadata(linkId, true)!;
    metadata.xwikiReference = attachmentReference(reference, true);
    // Saved without a label, since the label is just the attachment name. Editing the label brings it back. Note that
    // the link must not be marked as free-standing: that would save it as `attach:name.pdf`, without the surrounding
    // brackets, and a bare `attach:` reference is not parsed back as a link.
    metadata.xwikiGeneratedLabel = label;
    return {
      type: "paragraph",
      content: [
        {
          type: "link",
          href: injectLinkId(getDownloadURL(reference), linkId),
          content: label,
        },
      ],
    };
  }

  return async function uploadFile(
    file: File,
    blockId?: string,
  ): Promise<UploadResult> {
    // Pasted files are named after their media type by the browser, so they need a unique name. Dropped files keep the
    // name the user gave them.
    const toUpload = isPaste() ? withUniqueName(file) : file;
    try {
      const reference = await upload(toUpload);
      return file.type.startsWith("image/")
        ? toImageBlock(reference, blockId)
        : toAttachmentLink(reference, toUpload.name);
    } catch (error) {
      // BlockNote doesn't catch the errors of this callback and updates the block it inserted as soon as we return, so
      // rejecting would leave an unhandled rejection and an orphan block behind.
      notifyUploadFailed(toUpload.name, error);
      return EMPTY_PARAGRAPH;
    }
  };
}

export { type UploadFileDependencies, type UploadResult, createUploadFile };
