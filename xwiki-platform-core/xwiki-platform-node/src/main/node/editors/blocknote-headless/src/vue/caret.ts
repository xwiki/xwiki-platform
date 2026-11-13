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

// caretPositionFromPoint is the standard one, which WebKit and older Chrome lack.
function caretAt(left: number, top: number): [Node, number] | undefined {
  const position = document.caretPositionFromPoint?.(left, top);
  if (position) {
    return [position.offsetNode, position.offset];
  }
  const range = document.caretRangeFromPoint?.(left, top);
  return range ? [range.startContainer, range.startOffset] : undefined;
}

/**
 * Moves the caret under the pointer. A drop leaves the caret where it was, which is rarely where the file landed.
 * The selection is set in the DOM because BlockNote's setTextCursorPosition only takes a block and "start" or
 * "end", so it cannot put the caret inside the text the pointer is over. See
 * https://github.com/TypeCellOS/BlockNote/issues/3323#issuecomment-6800538598 for the API that would remove this.
 *
 * @param editor - the editor to move the caret in
 * @param coordinates - where the pointer is, in viewport coordinates
 * @returns false when there is no place for the caret under the pointer, e.g. because it is over the chrome around
 *   the content rather than over the content itself, which leaves the caret where it was
 * @since 18.10.0RC1
 * @beta
 */
function moveCaretTo(
  editor: EditorType,
  coordinates: { left: number; top: number },
): boolean {
  const caret = caretAt(coordinates.left, coordinates.top);
  if (!caret || !editor.prosemirrorView.dom.contains(caret[0])) {
    return false;
  }
  // ProseMirror only adopts the selection of a focused view, and focusing restores the old one, so focus first.
  editor.focus();
  document.getSelection()?.collapse(...caret);
  // ProseMirror reads a selection it didn't make only on a delayed flush, and would insert at the one it holds.
  (
    editor.prosemirrorView as unknown as { domObserver: { flush(): void } }
  ).domObserver.flush();
  return true;
}

export { moveCaretTo };
