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
import type { BlockOfType } from "../../blocknote";
import type { ResourceReference } from "@xwiki/platform-rendering-api";

/**
 * Hooks to intercept the image insertion / edition flow.
 *
 * @since 18.10.0RC1
 * @beta
 */
type ImageEditionHooks = {
  /**
   * Run right after the image selected by the user (e.g. from the image suggestions, the image selector or an upload)
   * is written into the content, either as a new image block or as the new source of an existing image block.
   *
   * @param block - the image block, as written into the content
   * @param reference - the resource reference of the selected image, when known (e.g. provided by the server with the
   *   image suggestions or the upload response); undefined when the image is only known by its URL (e.g. the user
   *   entered it)
   */
  afterSelect?: (
    block: BlockOfType<"image">,
    reference?: ResourceReference,
  ) => void;
};

export type { ImageEditionHooks };
