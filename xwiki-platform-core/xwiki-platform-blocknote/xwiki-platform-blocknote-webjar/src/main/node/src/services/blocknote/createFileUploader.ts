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
import { trackPaste } from "../attachments/pasteTracker";
import { withUniqueName } from "../attachments/uniqueFileName";
import { EntityType } from "@xwiki/platform-model-api";
import { uuidv4 } from "lib0/random";
import type { BlockNoteDocument } from "./BlockNoteProcessor";
import type { StorageProvider } from "@xwiki/platform-backend-api";
import type { ModelReferenceParserProvider } from "@xwiki/platform-model-reference-api";
import type { RemoteURLSerializerProvider } from "@xwiki/platform-model-remote-url-api";
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

function attachmentReference(
  reference: string,
  typed: boolean,
): ResourceReference {
  return { type: "attach", typed, reference, parameters: {} };
}

// The components the uploader needs, so that the editor doesn't have to know about them.
function getComponents(container: Container) {
  return {
    storageProvider: container.get<StorageProvider>("StorageProvider"),
    modelReferenceParser: container
      .get<ModelReferenceParserProvider>("ModelReferenceParserProvider")
      .get()!,
    remoteURLSerializer: container
      .get<RemoteURLSerializerProvider>("RemoteURLSerializerProvider")
      .get()!,
  };
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
    isTemporaryUpload,
    registerTemporaryAttachment,
    notifyUploadFailed,
  } = dependencies;

  const { storageProvider, modelReferenceParser, remoteURLSerializer } =
    getComponents(container);

  // Pasted files are renamed, so we need to know how the file reached the editor.
  const pasteTracker = trackPaste(eventTarget);

  async function attach(file: File): Promise<string> {
    const temporary = isTemporaryUpload();
    const result = await storageProvider
      .get()
      .saveAttachments(getDocumentReference(), [file], { temporary });
    const reference = result?.[0];
    if (!reference) {
      throw new Error(`Failed to attach [${file.name}].`);
    }
    if (temporary) {
      registerTemporaryAttachment(attachmentName(reference) ?? file.name);
    }
    return reference;
  }

  // The upload end-point can rename the file, so the name to commit is the one of the returned reference rather than
  // the one we sent.
  function attachmentName(reference: string): string | undefined {
    const parsed = modelReferenceParser.parse(reference, {
      type: EntityType.ATTACHMENT,
    });
    return parsed.type === EntityType.ATTACHMENT ? parsed.name : undefined;
  }

  function getDownloadURL(reference: string): string {
    return (
      remoteURLSerializer.serialize(
        modelReferenceParser.parse(reference, { type: EntityType.ATTACHMENT }),
      ) ?? ""
    );
  }

  function toImageBlock(reference: string, blockId?: string): UploadResult {
    if (blockId) {
      getBlockNoteDocument()!.getMetadata(blockId, true)!.xwikiReference =
        attachmentReference(reference, false);
    }
    // Empty name, otherwise BlockNote defaults the alt text to the file name.
    return { props: { url: getDownloadURL(reference), name: "" } };
  }

  // BlockNote's file, video and audio blocks have no XWiki counterpart, so they become a link instead. Its metadata
  // hangs off a synthetic id carried in the href, because links have no id of their own.
  function toAttachmentLink(reference: string, label: string): UploadResult {
    const linkId = uuidv4();
    const metadata = getBlockNoteDocument()!.getMetadata(linkId, true)!;
    metadata.xwikiReference = attachmentReference(reference, true);
    // Don't add xwikiFreestanding here: it saves the link without its [[ ]], and then it stops being a link
    // the next time the page is edited.
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

  async function upload(file: File, blockId?: string): Promise<UploadResult> {
    const toUpload =
      pasteTracker.isPaste() || !file.name ? withUniqueName(file) : file;
    try {
      const reference = await attach(toUpload);
      return file.type.startsWith("image/")
        ? toImageBlock(reference, blockId)
        : toAttachmentLink(reference, toUpload.name);
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
