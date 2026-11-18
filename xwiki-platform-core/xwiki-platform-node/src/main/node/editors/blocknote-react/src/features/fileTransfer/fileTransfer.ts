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
import { moveCaretTo } from "./caret";
import { withUniqueName } from "./uniqueFileName";
import { canUploadFiles, uploadFile } from "../../files/fileUploader";
import { uploadedFileLink, uploadedImage } from "../../files/uploadedFile";
import { updateImage } from "../../services/imageEditor";
import { getLinkMetadataHandler } from "../../services/linkEditor";
import { createExtension, createStore } from "@blocknote/core";
import type { EditorPartialLink } from "../../blocknote";
import type { EditorInstance } from "../../editorInstance";
import type { UploadedFile } from "../../files/fileUploader";
import type { LinkEditionData } from "../../services/linkEditor";
import type {
  EditorFeature,
  FeatureContext,
  FeatureContribution,
} from "../editorFeature";
import type { ExtensionOptions } from "@blocknote/core";

/**
 * Files pasted or dropped in the editor at once.
 */
type FileTransfer = {
  paste: boolean;

  files: File[];

  // Where the files were dropped: a drop leaves the caret where it was, so nothing else says where they belong.
  coordinates?: { left: number; top: number };
};

// What BlockNote replaces the block it inserted for a file with, when the upload fails: BlockNote leaves the block (an
// empty file or image block) in the content otherwise.
const EMPTY_PARAGRAPH = { type: "paragraph", content: [] };

function isImage(file: File): boolean {
  return file.type.startsWith("image/");
}

/**
 * The browser names a pasted file after its media type, so a second paste would overwrite the first.
 *
 * @param file - a pasted or dropped file
 * @param paste - whether the file was pasted
 * @returns the file to upload
 */
function toUpload(file: File, paste: boolean): File {
  return paste || !file.name ? withUniqueName(file) : file;
}

/**
 * @param instance - the editor instance where the file is uploaded
 * @param file - the file to upload
 * @returns the uploaded file, or undefined when the upload fails (the file upload listeners can report it to the user)
 */
async function upload(instance: EditorInstance, file: File) {
  try {
    return await uploadFile(instance, file);
  } catch (error) {
    console.error(`Failed to upload [${file.name}].`, error);
    return undefined;
  }
}

/**
 * @param instance - the editor instance where the link is inserted
 * @param link - the link to insert
 * @returns the inline link to write into the content, once the link metadata handler had its say
 */
function toInlineLink(
  instance: EditorInstance,
  link: LinkEditionData,
): EditorPartialLink {
  const { url, title } =
    getLinkMetadataHandler(instance)?.beforeUpdate(instance, link) ?? link;
  return { type: "link", href: url, content: title };
}

/**
 * Inserts a link to each file where the user is, rather than leaving the files to BlockNote, which gives each of them a
 * block of its own and would leave the link alone in a paragraph.
 */
async function insertFilesInline(
  instance: EditorInstance,
  { files, paste }: FileTransfer,
): Promise<void> {
  try {
    let first = true;
    for (const file of files) {
      // One upload at a time: two files renamed in the same tick would get the same generated name, since it is made
      // of the current timestamp and a small random number.
      const uploadedFile = await upload(instance, toUpload(file, paste));
      if (uploadedFile) {
        const link = toInlineLink(
          instance,
          uploadedFileLink(instance, uploadedFile),
        );
        // updateSelection leaves the caret after the link, so the next file lines up behind it, separated by a space
        // so that the links don't run into each other.
        instance.editor.insertInlineContent(first ? [link] : [" ", link], {
          updateSelection: true,
        });
        first = false;
      }
    }
  } catch (error) {
    // A failed upload is already handled, so anything caught here is a bug.
    console.error(
      "Failed to insert the links of the pasted or dropped files.",
      error,
    );
  }
}

/**
 * Puts the caret where the links of a transfer belong: under the pointer for a drop, since a drop doesn't move the
 * caret itself, and wherever it already is for a paste. Called from the event handler rather than after the uploads,
 * because a drop's coordinates are viewport pixels and point at different text once the page has scrolled.
 *
 * @returns false when the transfer didn't land on the content, so there is no caret to insert at
 */
function placeCaret(instance: EditorInstance, transfer: FileTransfer): boolean {
  return transfer.coordinates
    ? moveCaretTo(instance.editor, transfer.coordinates)
    : instance.editor.isFocused();
}

/**
 * Takes over the transfers of files that aren't images: their links are inserted where the user is. The images are
 * left to BlockNote, which inserts an image block for each of them, then calls {@link uploadForBlock}. A transfer that
 * mixes images with other files is left to BlockNote as a whole, rather than being split over two code paths.
 *
 * @returns true when the transfer is taken over
 */
function takeOver(instance: EditorInstance, transfer: FileTransfer): boolean {
  if (
    transfer.files.length === 0 ||
    transfer.files.some(isImage) ||
    !placeCaret(instance, transfer)
  ) {
    return false;
  }
  // Not awaited: the event handler has to answer while the event is still live, the uploads take far longer.
  void insertFilesInline(instance, transfer);
  return true;
}

/**
 * Handles the files pasted or dropped in the editor. It remembers whether the last transfer was a paste, because
 * BlockNote doesn't tell {@link uploadForBlock} what brought the file. The flag stays set until the opposite event rather
 * than being cleared once the upload starts, because BlockNote uploads the pasted files one after another, so all but
 * the first are read after the paste event is over.
 */
const FileTransferExtension = createExtension(
  ({
    options: { getInstance },
  }: ExtensionOptions<{ getInstance: () => EditorInstance }>) => {
    const store = createStore({ paste: false });

    const offer = (event: Event, transfer: FileTransfer) => {
      store.setState(() => ({ paste: transfer.paste }));
      if (takeOver(getInstance(), transfer)) {
        // BlockNote listens on the same element, so stopping the propagation isn't enough.
        event.preventDefault();
        event.stopImmediatePropagation();
      }
    };

    return {
      key: "fileTransfer",
      store,
      mount({ dom, signal }: { dom: HTMLElement; signal: AbortSignal }) {
        // Capture phase, so that the listeners run before BlockNote's, which starts the upload of the images.
        const options = { capture: true, signal };
        dom.addEventListener(
          "paste",
          (event) =>
            offer(event, {
              paste: true,
              files: Array.from(event.clipboardData?.files ?? []),
            }),
          options,
        );
        dom.addEventListener(
          "drop",
          (event) =>
            offer(event, {
              paste: false,
              files: Array.from(event.dataTransfer?.files ?? []),
              coordinates: { left: event.clientX, top: event.clientY },
            }),
          options,
        );
      },
    } as const;
  },
);

/**
 * Uploads a file for the block BlockNote inserted for it, when the file is pasted or dropped (see
 * {@link FileTransferExtension}).
 *
 * @param instance - the editor instance where the file is uploaded
 * @param file - the file to upload
 * @param blockId - the id of the block inserted for the file
 * @returns what BlockNote updates the block with
 */
async function uploadForBlock(
  instance: EditorInstance,
  file: File,
  blockId?: string,
): Promise<string | Record<string, unknown>> {
  const paste =
    instance.editor.getExtension(FileTransferExtension)?.store.state.paste ??
    false;
  const uploadedFile = await upload(instance, toUpload(file, paste));
  if (!uploadedFile) {
    return EMPTY_PARAGRAPH;
  }
  return blockId
    ? updateBlock(instance, uploadedFile, blockId)
    : uploadedFile.url;
}

/**
 * @param instance - the editor instance where the file is uploaded
 * @param uploadedFile - the uploaded file
 * @param blockId - the id of the block inserted for the file
 * @returns what BlockNote updates the block with
 */
function updateBlock(
  instance: EditorInstance,
  uploadedFile: UploadedFile,
  blockId: string,
): Record<string, unknown> {
  const block = instance.editor.getBlock(blockId);
  if (block?.type === "image") {
    const { url, reference } = uploadedImage(instance, uploadedFile);
    // Empty name, otherwise BlockNote saves it as the alternative text of the image, and for a pasted file that is
    // the name the browser made up rather than the attachment we uploaded.
    updateImage(instance, block, { url, name: "" }, reference);
    // The block is already updated, together with its metadata.
    return {};
  } else if (block) {
    // The file, video and audio blocks are saved as macro calls, but those macros don't exist yet, so such a file
    // becomes a link to the uploaded file instead, in a paragraph replacing the file block.
    return {
      type: "paragraph",
      content: [
        toInlineLink(instance, uploadedFileLink(instance, uploadedFile)),
      ],
    };
  }
  // The block was removed while the file was uploaded.
  return {};
}

/**
 * The files pasted or dropped in the editor are uploaded with the file uploader: the images are inserted as image
 * blocks, and links to the other files are inserted where the user is. Nothing is contributed when the user can't
 * upload files, so that BlockNote ignores the pasted and dropped files.
 *
 * @since 18.10.0RC1
 * @beta
 */
const fileTransferFeature: EditorFeature = {
  id: "fileTransfer",
  contribute(context: FeatureContext): FeatureContribution | undefined {
    if (!canUploadFiles(context)) {
      return undefined;
    }
    const { getInstance } = context;
    return {
      blockNoteExtensions: [FileTransferExtension({ getInstance })],
      blockNoteOptions: {
        uploadFile: (file, blockId) =>
          uploadForBlock(getInstance(), file, blockId),
      },
    };
  },
};

export { fileTransferFeature };
