/**
 * See the LICENSE file distributed with this work for additional
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
import ImageInsertView from "./vue/ImageInsertView.vue";
import { Node, mergeAttributes } from "@tiptap/core";
import { VueNodeViewRenderer } from "@tiptap/vue-3";

/**
 * @since 0.13
 * @beta
 */
const ImageInsertNode = Node.create({
  name: "imageInsert",
  group: "block",
  parseHTML() {
    return [{ tag: "image-insert-components" }];
  },
  renderHTML({ HTMLAttributes }) {
    return ["image-insert-components", mergeAttributes(HTMLAttributes)];
  },
  addNodeView() {
    return VueNodeViewRenderer(ImageInsertView);
  },
  addStorage() {
    return {
      markdown: {
        serialize: () => {
          // This element is only a temporary placeholder to pick and image, it is not ment to part of the persisted
          // syntax.
        },
      },
    };
  },
});

export { ImageInsertNode };
