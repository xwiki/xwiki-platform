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
import { moveCaretTo } from "../../vue/caret";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import type { EditorType } from "@xwiki/platform-editors-blocknote-react";

describe("moveCaretTo", () => {
  let editor: EditorType;
  let text: Node;

  // Only what moving the caret touches: the focus and the editor's own DOM element.
  function editorStub(): EditorType {
    const dom = document.createElement("div");
    dom.append("before");
    // The caret is set through the DOM selection, which needs the node to be in the document.
    document.body.append(dom);
    text = dom.firstChild!;
    return {
      focus: vi.fn(),
      prosemirrorView: { dom },
    } as unknown as EditorType;
  }

  beforeEach(() => {
    editor = editorStub();
  });

  afterEach(() => {
    document.body.innerHTML = "";
    // jsdom implements neither caret API, so each test defines the one it needs.
    delete (document as Partial<Document>).caretPositionFromPoint;
    delete (document as Partial<Document>).caretRangeFromPoint;
    vi.restoreAllMocks();
  });

  it("puts the caret under the pointer, at the exact offset", () => {
    document.caretPositionFromPoint = vi.fn(
      () => ({ offsetNode: text, offset: 2 }) as CaretPosition,
    );

    moveCaretTo(editor, { left: 5, top: 9 });

    expect(document.caretPositionFromPoint).toHaveBeenCalledWith(5, 9);
    // ProseMirror only adopts the selection of a focused view.
    expect(editor.focus).toHaveBeenCalled();
    expect(document.getSelection()?.anchorNode).toBe(text);
    expect(document.getSelection()?.anchorOffset).toBe(2);
  });

  it("falls back on caretRangeFromPoint, which is all WebKit has", () => {
    document.caretRangeFromPoint = vi.fn(
      () => ({ startContainer: text, startOffset: 3 }) as unknown as Range,
    );

    moveCaretTo(editor, { left: 5, top: 9 });

    expect(document.getSelection()?.anchorNode).toBe(text);
    expect(document.getSelection()?.anchorOffset).toBe(3);
  });

  it("leaves the caret alone when the pointer is outside the editor", () => {
    const selection = document.getSelection()!;
    vi.spyOn(selection, "collapse");
    document.caretPositionFromPoint = vi.fn(
      () => ({ offsetNode: document.body, offset: 0 }) as CaretPosition,
    );

    moveCaretTo(editor, { left: 5, top: 9 });

    expect(selection.collapse).not.toHaveBeenCalled();
  });

  it("leaves the caret alone when there is no position under the pointer", () => {
    const selection = document.getSelection()!;
    vi.spyOn(selection, "collapse");

    moveCaretTo(editor, { left: 5, top: 9 });

    expect(selection.collapse).not.toHaveBeenCalled();
  });
});
