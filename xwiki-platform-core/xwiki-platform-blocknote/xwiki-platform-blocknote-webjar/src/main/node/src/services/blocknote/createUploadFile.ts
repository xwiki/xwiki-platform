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

// A whole partial block, not just props, which is what lets us replace the block BlockNote guessed.
type UploadResult = Record<string, unknown>;

type UploadFileDependencies = {
  storageProvider: StorageProvider;
  modelReferenceParser: ModelReferenceParser;
  remoteURLSerializer: RemoteURLSerializer;

  // Functions because neither is known yet when the editor is created.
  getDocumentReference: () => string;
  getBlockNoteDocument: () => BlockNoteDocument | undefined;

  isPaste: () => boolean;
  notifyUploadFailed: (fileName: string, error: unknown) => void;
};

const EMPTY_PARAGRAPH: UploadResult = { type: "paragraph", content: [] };

function attachmentReference(
  reference: string,
  typed: boolean,
): ResourceReference {
  return { type: "attach", typed, reference, parameters: {} };
}

/**
 * The BlockNote `uploadFile` option: attaches the pasted and dropped files to the edited document, producing the same
 * wiki syntax as the CKEditor integration.
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
    // Drops the label, which is just the attachment name. Not `xwikiFreestanding`: that saves a bare
    // `attach:name.pdf` without the brackets, which isn't parsed back as a link.
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
    // Browsers name a pasted file after its media type, so the next paste would overwrite this attachment. A file
    // with no name can't be attached at all.
    const toUpload = isPaste() || !file.name ? withUniqueName(file) : file;
    try {
      const reference = await upload(toUpload);
      return file.type.startsWith("image/")
        ? toImageBlock(reference, blockId)
        : toAttachmentLink(reference, toUpload.name);
    } catch (error) {
      // BlockNote doesn't catch our errors and updates the inserted block as soon as we return, so rejecting would
      // leave an unhandled rejection and an orphan block.
      notifyUploadFailed(toUpload.name, error);
      return EMPTY_PARAGRAPH;
    }
  };
}

export { type UploadFileDependencies, type UploadResult, createUploadFile };
