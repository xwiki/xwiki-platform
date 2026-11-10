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
import { takeOverFileTransfers } from "./insertFilesInline";
import { injectLinkId } from "./linkId";
import { trackPaste } from "../attachments/pasteTracker";
import { withUniqueName } from "../attachments/uniqueFileName";
import {
  AttachmentReference,
  EntityType,
  Model,
} from "@xwiki/platform-xwiki-model-api";
import { uuidv4 } from "lib0/random";
import type { BlockNoteDocument } from "./BlockNoteProcessor";
import type {
  InlineLink,
  InsertInlineContent,
  MoveCaretTo,
} from "./insertFilesInline";
import type { PageAttachment } from "@xwiki/platform-api";
import type { StorageProvider } from "@xwiki/platform-backend-api";
import type { ResourceReference } from "@xwiki/platform-rendering-api";
import type { Container } from "inversify";

// A whole partial block, not just props, which is what lets us replace the block BlockNote guessed.
type UploadResult = Record<string, unknown>;

type FileUploaderDependencies = {
  // Where the paste and drop events are listened for, scoped to the editor so that two editors on the same page don't
  // see each other's.
  eventTarget: EventTarget;

  // Functions because neither is known yet when the editor is created.
  getDocumentReference: () => string;
  getBlockNoteDocument: () => BlockNoteDocument | undefined;

  insertInlineContent: InsertInlineContent;

  moveCaretTo: MoveCaretTo;

  // Whether the files are uploaded to the temporary store of the user session, from where the save request attaches
  // them to the document, rather than being attached right away.
  isTemporaryUpload: () => boolean;

  // Called for each file uploaded to the temporary store, with the name under which the save request has to ask for
  // it, otherwise the file is never attached to the document.
  registerTemporaryAttachment: (fileName: string) => void;

  notifyUploadFailed: (fileName: string, error: unknown) => void;
};

/**
 * Attaches to the edited document the files pasted and dropped over the editor, producing the same wiki syntax as the
 * CKEditor integration. Its upload function is what BlockNote expects as its `uploadFile` option.
 */
type FileUploader = {
  upload: (file: File, blockId?: string) => Promise<UploadResult>;

  // Stops listening for the paste and drop events. Call it when the editor goes away.
  dispose: () => void;
};

const EMPTY_PARAGRAPH: UploadResult = { type: "paragraph", content: [] };

// The browser names a pasted file after its media type, so a second paste would overwrite the first.
function toAttach(file: File, paste: boolean): File {
  return paste || !file.name ? withUniqueName(file) : file;
}

/**
 * @param container - the component container the storage and the model components are taken from
 * @param dependencies - what only the editor knows
 * @returns a file uploader for the document the editor is editing
 */
function createFileUploader(
  container: Container,
  dependencies: FileUploaderDependencies,
): FileUploader {
  const {
    eventTarget,
    getDocumentReference,
    getBlockNoteDocument,
    insertInlineContent,
    moveCaretTo,
    isTemporaryUpload,
    registerTemporaryAttachment,
    notifyUploadFailed,
  } = dependencies;

  const storageProvider = container.get<StorageProvider>("StorageProvider");

  // Pasted files are renamed, so we need to know how the file reached the editor.
  const pasteTracker = trackPaste(
    eventTarget,
    takeOverFileTransfers(
      insertInlineContent,
      moveCaretTo,
      async (file, paste) => {
        const toUpload = toAttach(file, paste);
        // Only the upload is caught here: a link that can't be built is a bug rather than something to report as
        // a failed upload, and insertFilesInline logs it as one.
        const attachment = await attach(toUpload).catch((error) => {
          notifyUploadFailed(toUpload.name, error);
          return undefined;
        });
        return attachment && linkTo(attachment, toUpload.name);
      },
    ),
  );

  // The file is attached to the edited document, so its reference is serialized relative to that document, as the
  // WYSIWYG editors save it, which also escapes a name the reference syntax would otherwise split (e.g. logo@2x.png).
  function attachmentReference(
    name: string,
    typed: boolean,
  ): ResourceReference {
    const documentReference = Model.resolve(
      getDocumentReference(),
      EntityType.DOCUMENT,
    );
    const reference = documentReference
      ? Model.serialize(
          new AttachmentReference(name, documentReference),
          documentReference,
        )
      : name;
    return { type: "attach", typed, reference, parameters: {} };
  }

  async function attach(file: File): Promise<PageAttachment> {
    const temporary = isTemporaryUpload();
    const result = await storageProvider
      .get()
      .saveAttachments(getDocumentReference(), [file], { temporary });
    const attachment = result?.[0];
    if (!attachment) {
      throw new Error(`Failed to attach [${file.name}].`);
    }
    if (temporary) {
      // The upload end-point can rename the file, so the name to commit is the one it reports rather than the one we
      // sent.
      registerTemporaryAttachment(attachment.reference);
    }
    return attachment;
  }

  function toImageBlock(
    attachment: PageAttachment,
    blockId?: string,
  ): UploadResult {
    if (blockId) {
      getBlockNoteDocument()!.getMetadata(blockId, true)!.xwikiReference =
        attachmentReference(attachment.reference, false);
    }
    // Empty name, otherwise BlockNote saves it as the alt text of the image (||alt="image.gif"), and for a pasted
    // file that is the name the browser made up rather than the attachment we uploaded. XWikiImageBlock displays a
    // label instead, without saving one.
    return { props: { url: attachment.href, name: "" } };
  }

  // The file, video and audio blocks are saved as macro calls, but those macros don't exist yet, so such a file
  // becomes a link to the attachment instead. The link metadata hangs off a synthetic id carried in the href,
  // because links have no id of their own.
  function linkTo(attachment: PageAttachment, label: string): InlineLink {
    const linkId = uuidv4();
    const metadata = getBlockNoteDocument()!.getMetadata(linkId, true)!;
    metadata.xwikiReference = attachmentReference(attachment.reference, true);
    // Don't add xwikiFreestanding here: it saves the link without its [[ ]], and then it stops being a link
    // the next time the page is edited.
    metadata.xwikiGeneratedLabel = label;
    return {
      type: "link",
      href: injectLinkId(attachment.href, linkId),
      content: label,
    };
  }

  async function upload(file: File, blockId?: string): Promise<UploadResult> {
    const toUpload = toAttach(file, pasteTracker.isPaste());
    try {
      const attachment = await attach(toUpload);
      if (file.type.startsWith("image/")) {
        return toImageBlock(attachment, blockId);
      }
      // Only the files BlockNote inserted a block for reach this, i.e. the file panel of a file block and the
      // transfers that also carry an image, so the link goes back as a paragraph, to replace that block.
      return {
        type: "paragraph",
        content: [linkTo(attachment, toUpload.name)],
      };
    } catch (error) {
      // BlockNote doesn't catch our errors and updates the inserted block as soon as we return, so rejecting would
      // leave an unhandled rejection and an orphan block.
      notifyUploadFailed(toUpload.name, error);
      return EMPTY_PARAGRAPH;
    }
  }

  return {
    upload,
    // Not the method itself: that would assume it never comes to use `this`.
    dispose: () => pasteTracker.dispose(),
  };
}

export {
  type FileUploader,
  type FileUploaderDependencies,
  type UploadResult,
  createFileUploader,
};
