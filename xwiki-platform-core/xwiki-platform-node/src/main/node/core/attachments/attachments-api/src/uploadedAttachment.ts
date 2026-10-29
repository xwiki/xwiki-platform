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

/**
 * Describes an attachment that was just uploaded. Storages fill what they know about the uploaded attachment, so more
 * optional information can be added later without breaking the existing storages.
 *
 * @since 18.10.0RC1
 * @beta
 */
type UploadedAttachment = {
  /**
   * The URL of the uploaded attachment.
   */
  url: string;

  /**
   * The serialized reference of the uploaded attachment, possibly relative to the page it was uploaded to (e.g. only
   * the file name), when the storage knows it. It lets the editors refer to the attachment by reference rather than
   * by URL.
   */
  reference?: string;
};

export type { UploadedAttachment };
