/*
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

import { MarkType, NodeType } from "@tiptap/pm/model";
import { EditorState } from "@tiptap/pm/state";
import { findParentNode } from "@tiptap/vue-3";
import { Primitive } from "utility-types";

function isNodeActive(
  type: NodeType,
  attrs?: Record<string, Primitive>,
): (state: EditorState) => boolean {
  return (state) => {
    let isActive = false;
    if (type) {
      const nodeAfter = state.selection.$from.nodeAfter;
      let node = nodeAfter?.type == type ? nodeAfter : undefined;
      if (!node) {
        const parent = findParentNode((node) => node.type === type)(
          state.selection,
        );
        node = parent?.node;
      }

      if (node) {
        isActive = node?.hasMarkup(type, { ...node.attrs, ...attrs });
      }
    }
    return isActive;
  };
}

function isMarkActive(type: MarkType): (state: EditorState) => boolean {
  return (state) => {
    let isActive = false;
    if (type) {
      const selection = state.selection;
      const empty = selection.empty;
      if (empty) {
        isActive = !!type.isInSet(state.storedMarks || selection.$from.marks());
      } else {
        isActive = state.doc.rangeHasMark(selection.from, selection.to, type);
      }
    }
    return isActive;
  };
}

export { isNodeActive, isMarkActive };
