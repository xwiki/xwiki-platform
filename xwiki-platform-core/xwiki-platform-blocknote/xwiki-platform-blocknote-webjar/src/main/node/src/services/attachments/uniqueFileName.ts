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

// The lower and upper bounds of the random suffix added to a generated file name, matching the CKEditor integration.
const RANDOM_MIN = 100;
const RANDOM_MAX = 999;

/**
 * @param fileName - the name of the file about to be uploaded
 * @returns the extension of the given file name, including the leading dot, or an empty string when it has none
 */
function getExtension(fileName: string): string {
  // Everything after the first dot, so that "archive.tar.gz" keeps both extensions.
  const dotIndex = fileName.indexOf(".");
  return dotIndex < 0 ? "" : fileName.substring(dotIndex);
}

/**
 * Renames a file pasted from the clipboard, which the browser names after its media type rather than its content (e.g.
 * "image.png", or a localized form such as "graphik.png" in German Firefox). Without this, every pasted screenshot
 * would be uploaded under the same name and overwrite the previous one.
 *
 * @param file - the file about to be uploaded
 * @returns a copy of the given file named after the current timestamp, keeping the original extension
 */
function withUniqueName(file: File): File {
  const random =
    Math.floor(Math.random() * (RANDOM_MAX - RANDOM_MIN + 1)) + RANDOM_MIN;
  const name = `${Date.now()}-${random}${getExtension(file.name)}`;
  return new File([file], name, {
    type: file.type,
    lastModified: file.lastModified,
  });
}

export { withUniqueName };
