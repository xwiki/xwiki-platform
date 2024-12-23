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
import { BubbleMenuAction } from "./BubbleMenuAction";
import type { Editor } from "@tiptap/core";

function getReplaceAction(editor: Editor): BubbleMenuAction {
  return {
    title: "Replace",
    icon: "arrow-left-right",
    command() {
      editor
        .chain()
        .focus()
        .deleteCurrentNode()
        .insertContent({
          type: "imageInsert",
        })
        .run();
    },
    isActive: () => true,
  };
}

function getDeleteAction(editor: Editor): BubbleMenuAction {
  return {
    title: "Delete",
    icon: "trash",
    command() {
      editor.commands.deleteSelection();
    },
    isActive: () => true,
  };
}

function getImageMenuActions(editor: Editor): BubbleMenuAction[] {
  return [getReplaceAction(editor), getDeleteAction(editor)];
}

export { getImageMenuActions };
