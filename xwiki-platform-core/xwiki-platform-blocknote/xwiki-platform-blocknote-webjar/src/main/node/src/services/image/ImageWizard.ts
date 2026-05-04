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
import type { BlockOfType } from "@xwiki/platform-editors-blocknote-react";

/**
 * The interface used by the image wizard to submit the image properties or to notify that the image insertion / edition
 * has been cancelled.
 *
 * @since 18.3.0RC1
 * @beta
 */
interface ImageWizardCallback {
  submit(image: Partial<BlockOfType<"image">["props"]>): void;
  cancel(): void;
}

/**
 * The component used to insert or edit image blocks in the editor.
 *
 * @since 18.3.0RC1
 * @beta
 */
interface ImageWizard {
  /**
   * Shows the UI to insert a new image (i.e. select an image and set its properties) and uses the provided callback to
   * insert the new image block with the provided properties or to cancel the image insertion.
   *
   * @param callback - the callback used to either insert the new image block or to cancel the insertion
   */
  insert(callback: ImageWizardCallback): void;

  /**
   * Shows the UI to edit the image properties and uses the provided callback to update the image block with the new
   * property values or to cancel the image edition.
   *
   * @param image - the image block properties to edit
   * @param callback - the callback used to either update the image block or to cancel the image edition
   */
  edit(
    image: BlockOfType<"image">["props"],
    callback: ImageWizardCallback,
  ): void;
}

export type { ImageWizard, ImageWizardCallback };
