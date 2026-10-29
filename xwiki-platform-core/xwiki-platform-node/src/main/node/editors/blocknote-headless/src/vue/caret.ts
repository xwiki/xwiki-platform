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
import type { EditorType } from "@xwiki/platform-editors-blocknote-react";

// The DOM position under the pointer. Firefox and recent Chrome have caretPositionFromPoint, WebKit and older Chrome
// only the earlier caretRangeFromPoint.
function caretAt(left: number, top: number): [Node, number] | undefined {
  const position = document.caretPositionFromPoint?.(left, top);
  if (position) {
    return [position.offsetNode, position.offset];
  }
  const range = document.caretRangeFromPoint?.(left, top);
  return range ? [range.startContainer, range.startOffset] : undefined;
}

/**
 * Moves the caret under the pointer, for a drop, which leaves it where it was, rarely where the file landed. The
 * selection is set in the DOM because BlockNote only places the caret at a block boundary, which would put whatever
 * is inserted at the end of the block the pointer is over. The editor has to be focused for ProseMirror to adopt the
 * new selection, and it reads it on the next tick.
 *
 * @param editor - the editor to move the caret in
 * @param coordinates - where the pointer is, in viewport coordinates
 */
function moveCaretTo(
  editor: EditorType,
  coordinates: { left: number; top: number },
): void {
  editor.focus();
  const caret = caretAt(coordinates.left, coordinates.top);
  if (caret && editor.prosemirrorView.dom.contains(caret[0])) {
    document.getSelection()?.collapse(...caret);
  }
}

export { moveCaretTo };
