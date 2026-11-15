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
import { moveCaretTo } from "@xwiki/platform-editors-blocknote-headless";
import type { FileTransfer } from "../attachments/pasteTracker";
import type {
  EditorPartialLink,
  EditorType,
} from "@xwiki/platform-editors-blocknote-react";

type LinkFor = (
  file: File,
  paste: boolean,
) => Promise<EditorPartialLink | undefined>;

function isImage(file: File): boolean {
  return file.type.startsWith("image/");
}

/**
 * Inserts the link of each file where the user is, rather than leaving the files to BlockNote, which gives each of
 * them a block of its own and would leave the link alone in a paragraph.
 */
async function insertFilesInline(
  editor: EditorType,
  { files, paste }: FileTransfer,
  linkFor: LinkFor,
): Promise<void> {
  try {
    let first = true;
    for (const file of files) {
      // One upload at a time: two files renamed in the same tick would get the same generated name, since it is
      // made of the current timestamp and a small random number.
      const link = await linkFor(file, paste);
      if (link) {
        // updateSelection leaves the caret after the link, so the next file lines up behind it, separated by a
        // space so that the links don't run into each other.
        editor.insertInlineContent(first ? [link] : [" ", link], {
          updateSelection: true,
        });
        first = false;
      }
    }
  } catch (error) {
    // A failed upload is already reported to the user by linkFor, so anything caught here is a bug.
    console.error(
      "Failed to insert the links of the pasted or dropped files.",
      error,
    );
  }
}

/**
 * Puts the caret where the links of a transfer belong: under the pointer for a drop, since a drop doesn't move the
 * caret itself, and wherever it already is for a paste. Called from the event handler rather than after the
 * uploads, because a drop's coordinates are viewport pixels and point at different text once the page has
 * scrolled.
 *
 * @returns false when the transfer didn't land on the content, so there is no caret to insert at
 */
function placeCaret(editor: EditorType, transfer: FileTransfer): boolean {
  return transfer.coordinates
    ? moveCaretTo(editor, transfer.coordinates)
    : editor.isFocused();
}

/**
 * Decides which transfers to insert rather than leave to BlockNote. A transfer that mixes images with other files
 * is left to BlockNote as a whole, rather than being split over two code paths.
 *
 * @param getEditor - the editor to insert in, or nothing before it is mounted
 * @param linkFor - attaches a file and returns the link to it, or nothing if that failed
 * @returns the handler the paste tracker calls to offer a transfer, true meaning that the transfer is ours now
 */
function takeOverFileTransfers(
  getEditor: () => EditorType | undefined,
  linkFor: LinkFor,
): (transfer: FileTransfer) => boolean {
  return function takeOver(transfer) {
    const editor = getEditor();
    if (!editor || transfer.files.some(isImage)) {
      return false;
    }
    if (!placeCaret(editor, transfer)) {
      return false;
    }
    // Not awaited: the handler has to answer while the event is still live, the uploads take far longer.
    void insertFilesInline(editor, transfer, linkFor);
    return true;
  };
}

export { takeOverFileTransfers };
