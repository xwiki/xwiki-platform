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
import type { FileTransfer } from "../attachments/pasteTracker";

type InlineLink = {
  type: "link";
  href: string;
  content: string;
};

type LinkFor = (file: File, paste: boolean) => Promise<InlineLink | undefined>;

type InsertInlineContent = (content: (InlineLink | string)[]) => void;

// Returns false when there is nowhere to put the caret under the pointer, e.g. a drop on the chrome around the
// content.
type MoveCaretTo = (coordinates: { left: number; top: number }) => boolean;

function isImage(file: File): boolean {
  return file.type.startsWith("image/");
}

/**
 * Inserts the link of each file where the user is, rather than leaving the files to BlockNote, which gives each of
 * them a block of its own and would leave the link alone in a paragraph.
 */
async function insertFilesInline(
  insertInlineContent: InsertInlineContent,
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
        // Each insertion leaves the caret where the next file belongs, separated by a space so that the links
        // don't run into each other.
        insertInlineContent(first ? [link] : [" ", link]);
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
 * Decides which transfers to insert rather than leave to BlockNote. A transfer that mixes images with other files
 * is left to BlockNote as a whole, rather than being split over two code paths.
 *
 * @param insertInlineContent - inserts the links in the editor
 * @param moveCaretTo - moves the caret under the pointer, telling whether there was a place for it there
 * @param linkFor - attaches a file and returns the link to it, or nothing if that failed
 * @returns the handler the paste tracker calls to offer a transfer, true meaning that the transfer is ours now
 */
function takeOverFileTransfers(
  insertInlineContent: InsertInlineContent,
  moveCaretTo: MoveCaretTo,
  linkFor: LinkFor,
): (transfer: FileTransfer) => boolean {
  return function takeOver(transfer) {
    if (transfer.files.some(isImage)) {
      return false;
    }
    // The caret moves now rather than once the files are uploaded, because the coordinates are in the viewport:
    // scrolling while a large file uploads would otherwise point them at whatever text has moved under them. A
    // drop with nowhere to put the caret isn't on the content at all, e.g. on a dialog over the editor, so it
    // goes back to BlockNote.
    if (transfer.coordinates && !moveCaretTo(transfer.coordinates)) {
      return false;
    }
    // Not awaited: the handler has to answer while the event is still live, the uploads take far longer.
    void insertFilesInline(insertInlineContent, transfer, linkFor);
    return true;
  };
}

export {
  type InlineLink,
  type InsertInlineContent,
  type MoveCaretTo,
  takeOverFileTransfers,
};
