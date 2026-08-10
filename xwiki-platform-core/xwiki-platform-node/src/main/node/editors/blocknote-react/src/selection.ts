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
import type { EditorType } from "./blocknote";

/**
 * Whether the current selection targets editable inline content — i.e. at least one selected block
 * has content (inline text or a table), as opposed to a content-less block (e.g. a block-level
 * macro). Inline-only actions such as creating a link only make sense when this is true.
 *
 * @param editor - the BlockNote editor instance
 * @returns whether the selection targets editable inline content
 */
function selectionHasInlineContent(editor: EditorType): boolean {
  return (
    editor.getSelection()?.blocks ?? [editor.getTextCursorPosition().block]
  ).some((block) => block.content !== undefined);
}

export { selectionHasInlineContent };
