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

// The inline link to an attachment, as BlockNote takes it.
type InlineLink = {
  type: "link";
  href: string;
  content: string;
};

// Attaches a pasted or dropped file to the edited document and returns the link to it.
type LinkFor = (file: File, paste: boolean) => Promise<InlineLink | undefined>;

// Inserts inline content where the user is: under the pointer when the files were dropped, at the caret otherwise.
type InsertInlineContent = (
  content: InlineLink[],
  coordinates?: { left: number; top: number },
) => void;

function isImage(file: File): boolean {
  return file.type.startsWith("image/");
}

/**
 * Links the files of a transfer where they landed, which is what the CKEditor integration does, rather than leaving
 * them to BlockNote: it gives each file a block of its own, so the link would end up alone in a paragraph.
 */
async function insertFilesInline(
  insertInlineContent: InsertInlineContent,
  { files, paste, coordinates }: FileTransfer,
  linkFor: LinkFor,
): Promise<void> {
  for (const file of files) {
    const link = await linkFor(file, paste);
    if (link) {
      // Only the first insertion has to look for the drop position: it leaves the caret after the link, which is
      // where the next file of the transfer belongs.
      insertInlineContent([link], coordinates);
      coordinates = undefined;
    }
  }
}

/**
 * Decides which transfers to insert rather than leave to BlockNote. A transfer that mixes images with other files is
 * left to it as a whole, rather than being split over two code paths.
 *
 * @param insertInlineContent - inserts the links in the editor
 * @param linkFor - attaches a file to the edited document and returns the link to it, or nothing if that failed
 * @returns what the paste tracker calls to offer a transfer, which takes it over by returning true
 */
function takeOverFileTransfers(
  insertInlineContent: InsertInlineContent,
  linkFor: LinkFor,
): (transfer: FileTransfer) => boolean {
  return (transfer) => {
    if (transfer.files.some(isImage)) {
      return false;
    }
    void insertFilesInline(insertInlineContent, transfer, linkFor);
    return true;
  };
}

export { type InlineLink, type InsertInlineContent, takeOverFileTransfers };
