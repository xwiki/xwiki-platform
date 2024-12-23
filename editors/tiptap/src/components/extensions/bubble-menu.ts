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
import { getImageMenuActions } from "./bubble-menu/image-actions";
import { getTextMenuActions } from "./bubble-menu/text-actions";
import type { BubbleMenuAction } from "./bubble-menu/BubbleMenuAction";
import type { Editor } from "@tiptap/core";

// TODO: also add condition, for instance some actions shouldn't be proposed on
// code.

/**
 * @since 0.13
 */
enum ElementType {
  TEXT,
  IMAGE,
}

export default function getMenuActions(
  editor: Editor,
  type: ElementType = ElementType.TEXT,
): BubbleMenuAction[] {
  let actions;
  switch (type) {
    case ElementType.TEXT:
      actions = getTextMenuActions(editor);
      break;
    case ElementType.IMAGE:
      actions = getImageMenuActions(editor);
      break;
  }

  // Keep only actions at can be activated in the current context:
  // Actions with marks and nodes that are not available should not be available
  // Some actions can also be deactivated when the current selection does not
  // allow it (e.g., not possible to define bold text inside a code block).
  return actions.filter((action) => {
    return action.isActive(editor.state);
  });
}

export { ElementType };
