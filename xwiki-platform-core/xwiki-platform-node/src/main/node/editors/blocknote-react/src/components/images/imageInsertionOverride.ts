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
 * Result of an image insertion process, from `ImageInsertionOverrideFn`.
 *
 * @since 18.10.0RC1
 * @beta
 */
type ImageInsertionResult =
  | {
      type: "insert";
      /**
       * The properties of the image block to insert.
       */
      props: Partial<BlockOfType<"image">["props"]>;
      /**
       * The resource reference of the image to insert, when known.
       */
      reference?: ResourceReference;
    }
  | { type: "aborted" };

/**
 * Interceptor for the image insertion mechanism, i.e. what happens when the user chooses to browse the images to
 * insert. The default image selector is shown when not specified.
 *
 * @param insert - called with the image to insert, or to notify that the image insertion was aborted
 * @since 18.10.0RC1
 * @beta
 */
type ImageInsertionOverrideFn = (
  insert: (insertionResult: ImageInsertionResult) => void,
) => void;

export type { ImageInsertionOverrideFn, ImageInsertionResult };
