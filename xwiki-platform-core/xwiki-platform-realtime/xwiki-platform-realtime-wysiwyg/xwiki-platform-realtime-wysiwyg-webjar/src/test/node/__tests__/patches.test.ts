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

import { mockRequireJS } from "@xwiki/platform-test-requirejs";
import ChainPad from "chainpad";
import * as DiffDOM from "diff-dom";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { readFileSync } from "node:fs";
import { resolve } from "node:path";
import type { RequireJSMock } from "@xwiki/platform-test-requirejs";

const PATCHES_SOURCE = readFileSync(
  resolve(import.meta.dirname, "../../../main/webjar/patches.js"),
  "utf-8",
);

// The filling character sequence that CKEditor inserts on WebKit in the text node where the user types.
const FILLING_CHAR_SEQUENCE = "\u200b".repeat(7);

// Headings have different structures, so DiffDOM moves them instead of modifying their text.
const HEADINGS =
  "<h1>one</h1><h2>two</h2><h3>three</h3><h4>four</h4><h5>five</h5>";
const MOVED_HEADINGS =
  "<h4>four</h4><h5>five</h5><h1>one</h1><h2>two</h2><h3>three</h3>";

type FakeRequire = (
  ids: string[],
  onLoad: (...modules: unknown[]) => void,
) => void;

type Patches = {
  setHTML(html: string, propagate: boolean): Promise<void>;
};

let requirejs: RequireJSMock;
let content: HTMLElement;
let selection: Range[];
let restoreSelection: ReturnType<typeof vi.fn>;
let patches: Patches;

function loadPatches(): new (editor: unknown) => Patches {
  requirejs = mockRequireJS({
    // Only used when merging the content, which is not tested here.
    "xwiki-realtime-transformers": {},
    // Only used to (de)serialize the content as HyperJSON, which is not tested here.
    "hyper-json": {},
    "diff-dom": DiffDOM,
    "json.sortify": JSON.stringify,
    chainpad: ChainPad,
  });
  // Evaluate the script in the global scope, where the RequireJS mock is installed.
  (0, eval)(PATCHES_SOURCE);
  let patchesClass: unknown;
  (globalThis as unknown as { require: FakeRequire }).require(
    ["xwiki-realtime-wysiwyg-patches"],
    (loaded) => {
      patchesClass = loaded;
    },
  );
  return patchesClass as new (editor: unknown) => Patches;
}

/**
 * @returns a fake editor, whose content is the content element
 */
function createEditor(): unknown {
  return {
    getContentWrapper: () => content,
    getFilters: () => [],
    isReadOnly: () => false,
    // The text-based selection is the fallback when the selection paths are invalidated.
    saveSelection: () => {},
    getSelection: () => selection,
    restoreSelection,
    parseInputHTML: (html: string) => {
      const newContent = document.createElement("div");
      newContent.innerHTML = html;
      return newContent;
    },
    updateContent: (updater: (content: HTMLElement) => unknown) =>
      updater(content),
  };
}

/**
 * Sets the local content of the editor.
 *
 * @param html - the local content
 */
function setContent(html: string): void {
  content.innerHTML = html;
}

/**
 * Places the caret in the local content.
 *
 * @param container - the node holding the caret
 * @param offset - the caret offset in that node
 */
function placeCaret(container: Node, offset: number): void {
  const range = document.createRange();
  range.setStart(container, offset);
  range.collapse(true);
  selection = [range];
}

/**
 * @param text - the text to look for
 * @returns the (first) text node, from the local content, whose data is the given text
 */
function findText(text: string): Text {
  const walker = document.createTreeWalker(content, NodeFilter.SHOW_TEXT);
  while (walker.nextNode()) {
    if ((walker.currentNode as Text).data === text) {
      return walker.currentNode as Text;
    }
  }
  throw new Error(`No text node with the data [${text}].`);
}

/**
 * Applies a remote change, as if it was received from another user.
 *
 * @param html - the content after the remote change
 */
async function applyRemoteContent(html: string): Promise<void> {
  await patches.setHTML(html, false);
  expect(content.innerHTML).toEqual(html);
  expect(restoreSelection).toHaveBeenCalledTimes(1);
}

/**
 * @returns the caret position restored after applying the remote change, or null if the selection paths were
 *   invalidated by the remote change (in which case the editor falls back on the less precise text-based selection)
 */
function getRestoredCaret(): [Node, number] | null {
  const ranges = restoreSelection.mock.calls[0][0] as Range[] | null;
  if (!ranges) {
    return null;
  }
  expect(ranges).toHaveLength(1);
  expect(ranges[0].collapsed).toBe(true);
  return [ranges[0].startContainer, ranges[0].startOffset];
}

describe("Patches", () => {
  beforeEach(() => {
    content = document.createElement("div");
    document.body.append(content);
    selection = [];
    restoreSelection = vi.fn();
    const Patches = loadPatches();
    patches = new Patches(createEditor());
    // The selection paths are logged for debugging.
    vi.spyOn(console, "debug").mockImplementation(() => {});
  });

  afterEach(() => {
    requirejs.restore();
    content.remove();
    vi.restoreAllMocks();
  });

  describe("setHTML", () => {
    describe("keeps the caret", () => {
      it("when a node is added before it", async () => {
        setContent("<p>one</p><p>two</p>");
        const text = findText("two");
        placeCaret(text, 1);

        await applyRemoteContent("<p>zero</p><p>one</p><p>two</p>");

        expect(getRestoredCaret()).toEqual([text, 1]);
      });

      it("when text is modified after it", async () => {
        setContent("<p>one two</p>");
        const text = findText("one two");
        placeCaret(text, 3);

        await applyRemoteContent("<p>one three</p>");

        expect(getRestoredCaret()).toEqual([text, 3]);
      });

      it("when an attribute is modified", async () => {
        setContent("<p>one</p><p>two</p>");
        const text = findText("two");
        placeCaret(text, 1);

        await applyRemoteContent('<p class="info">one</p><p>two</p>');

        expect(getRestoredCaret()).toEqual([text, 1]);
      });

      it("when a comment before it is modified", async () => {
        setContent("<p><!--one-->two</p>");
        const text = findText("two");
        placeCaret(text, 1);

        await applyRemoteContent("<p><!--three-->two</p>");

        expect(getRestoredCaret()).toEqual([text, 1]);
      });

      it("when the node right after it is removed", async () => {
        setContent("<p>one<br><br></p>");
        const paragraph = content.firstChild as Node;
        placeCaret(paragraph, 2);

        await applyRemoteContent("<p>one<br></p>");

        expect(getRestoredCaret()).toEqual([paragraph, 2]);
      });

      it("when the node holding it is moved", async () => {
        setContent(HEADINGS);
        const text = findText("two");
        placeCaret(text, 1);

        await applyRemoteContent(MOVED_HEADINGS);

        expect(getRestoredCaret()).toEqual([text, 1]);
      });

      it("when the node holding it is shifted by moving other nodes", async () => {
        setContent(HEADINGS);
        const text = findText("four");
        placeCaret(text, 1);

        await applyRemoteContent(MOVED_HEADINGS);

        expect(getRestoredCaret()).toEqual([text, 1]);
      });

      it("when a node is added in a sibling whose index is a prefix of the caret index", async () => {
        // The caret is in the 21st span, and the remote change adds a node in the 3rd span: the path of the 3rd span
        // (0/2) is a string prefix of the path of the 21st span (0/20), but the 3rd span isn't an ancestor of the caret.
        const spans = Array.from(
          { length: 21 },
          (_, index) => `<span>${index}</span>`,
        );
        setContent(`<p>${spans.join("")}</p>`);
        const text = findText("20");
        placeCaret(text, 1);

        spans[2] = "<span><em>new</em>2</span>";
        await applyRemoteContent(`<p>${spans.join("")}</p>`);

        expect(getRestoredCaret()).toEqual([text, 1]);
      });
    });

    describe("moves the caret to the left", () => {
      it("when the filling characters before it are removed", async () => {
        // CKEditor inserts the filling characters on WebKit, e.g. when typing after an inline widget. They're not part
        // of the synchronized content, so the remote content doesn't have them.
        setContent(
          `<p>one<img src="image.png">${FILLING_CHAR_SEQUENCE} two</p>`,
        );
        const text = findText(`${FILLING_CHAR_SEQUENCE} two`);
        placeCaret(text, text.length);

        await applyRemoteContent('<p>one<img src="image.png"> two</p>');

        expect(getRestoredCaret()).toEqual([text, 4]);
      });

      it("when text is removed before it", async () => {
        setContent("<p>one two three</p>");
        const text = findText("one two three");
        placeCaret(text, 9);

        await applyRemoteContent("<p>one three</p>");

        expect(getRestoredCaret()).toEqual([text, 5]);
      });
    });

    describe("invalidates the selection paths", () => {
      it("when text is inserted before the caret", async () => {
        // Inserting text before the caret can be the result of splitting the text node, e.g. to apply some formatting,
        // in which case we can't tell where the caret should go.
        setContent("<p>one two</p>");
        placeCaret(findText("one two"), 5);

        await applyRemoteContent("<p>one, two</p>");

        expect(getRestoredCaret()).toBeNull();
      });

      it("when the text around the caret is removed", async () => {
        setContent("<p>one two three</p>");
        placeCaret(findText("one two three"), 5);

        await applyRemoteContent("<p>three</p>");

        expect(getRestoredCaret()).toBeNull();
      });
    });
  });
});
