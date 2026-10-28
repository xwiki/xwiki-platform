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
import * as DiffDOM from "diff-dom";
import { afterEach, beforeEach, describe, expect, it } from "vitest";
import { readFileSync } from "node:fs";
import { resolve } from "node:path";
import type { RequireJSMock } from "@xwiki/platform-test-requirejs";

const ADAPTER_SOURCE = readFileSync(
  resolve(
    import.meta.dirname,
    "../../../../main/webjar/xwiki-realtime/ckeditorRealtimeAdapter.js",
  ),
  "utf-8",
);

// The filling characters that CKEditor inserts on WebKit, e.g. when typing after an inline widget, start a new text
// node.
const FILLING_CHAR_SEQUENCE = "\u200b".repeat(7);
const IMAGE = '<img src="image.png">';

// The hidden selection container that CKEditor adds at the end of the editable area when a widget is selected.
const HIDDEN_SELECTION =
  '<div data-cke-hidden-sel="1" data-cke-temp="1">widget</div>';
// The filler that the widgetselection plugin adds at the start of the editable area when it starts with a widget.
const FILLER =
  '<div data-cke-temp="1" data-cke-filler-webkit="start">&nbsp;</div>';

type FakeRequire = (
  ids: string[],
  onLoad: (...modules: unknown[]) => void,
) => void;

type Updater = (content: HTMLElement) => unknown;

type CKEditorAdapter = {
  updateContent(updater: Updater, isLocalChange: boolean): Promise<void>;
};

let requirejs: RequireJSMock;
let content: HTMLElement;
let customData: Map<string, unknown>;
let adapter: CKEditorAdapter;

function loadAdapter(): new (
  ckeditor: unknown,
  CKEDITOR: unknown,
) => CKEditorAdapter {
  requirejs = mockRequireJS({
    // The base class only defines the editor "interface".
    "xwiki-realtime-wysiwyg-editor": class {},
    "json.sortify": JSON.stringify,
  });
  // Evaluate the script in the global scope, where the RequireJS mock is installed.
  (0, eval)(ADAPTER_SOURCE);
  let adapterClass: unknown;
  (globalThis as unknown as { require: FakeRequire }).require(
    ["xwiki-ckeditor-realtime-adapter"],
    (loaded) => {
      adapterClass = loaded;
    },
  );
  return adapterClass as new (
    ckeditor: unknown,
    CKEDITOR: unknown,
  ) => CKEditorAdapter;
}

/**
 * @param html - the new content
 * @returns an updater that patches the editor content to match the given HTML, the way the realtime editor applies
 *   remote changes
 */
function patchTo(html: string): Updater {
  return (oldContent) => {
    const newContent = document.createElement("div");
    newContent.innerHTML = html;
    const diffDOM = new DiffDOM.DiffDOM({
      maxChildCount: false,
      valueDiffing: false,
    });
    diffDOM.apply(
      oldContent,
      diffDOM.diff(
        DiffDOM.nodeToObj(oldContent, diffDOM.options),
        DiffDOM.nodeToObj(newContent, diffDOM.options),
      ),
    );
    return [];
  };
}

/**
 * Simulates CKEditor remembering the text node holding the filling characters, in order to remove them later.
 *
 * @param text - the text node holding the filling characters
 */
function setFillingChar(text: Text): void {
  customData.set("cke-fillingChar", { $: text, getText: () => text.data });
}

describe("CKEditorAdapter", () => {
  beforeEach(() => {
    content = document.createElement("div");
    document.body.append(content);
    customData = new Map();
    const editable = {
      $: content,
      getCustomData: (key: string) => customData.get(key) ?? null,
      removeCustomData: (key: string) => customData.delete(key),
    };
    const ckeditor = {
      mode: "wysiwyg",
      on: () => {},
      fire: () => {},
      editable: () => editable,
      widgets: { instances: {}, checkWidgets: () => {} },
    };
    const CKEDITOR = { dom: { selection: { FILLING_CHAR_SEQUENCE } } };
    const CKEditorAdapter = loadAdapter();
    adapter = new CKEditorAdapter(ckeditor, CKEDITOR);
  });

  afterEach(() => {
    requirejs.restore();
    content.remove();
  });

  describe("updateContent", () => {
    it("hides the temporary elements from the updater", async () => {
      content.innerHTML = `${FILLER}<p>one</p>${HIDDEN_SELECTION}`;
      let updatedContent;

      await adapter.updateContent((newContent) => {
        updatedContent = newContent.innerHTML;
        return [];
      }, false);

      expect(updatedContent).toEqual("<p>one</p>");
      expect(content.innerHTML).toEqual(
        `${FILLER}<p>one</p>${HIDDEN_SELECTION}`,
      );
    });

    it("puts the temporary elements back at the start and at the end", async () => {
      content.innerHTML = `${FILLER}<p>one</p>${HIDDEN_SELECTION}`;

      await adapter.updateContent(
        patchTo("<h1>zero</h1><p>one</p><p>two</p>"),
        false,
      );

      expect(content.innerHTML).toEqual(
        `${FILLER}<h1>zero</h1><p>one</p><p>two</p>${HIDDEN_SELECTION}`,
      );
    });

    it("puts the temporary elements back at the end when there is no content", async () => {
      content.innerHTML = HIDDEN_SELECTION;

      await adapter.updateContent(patchTo("<p>one</p>"), false);

      expect(content.innerHTML).toEqual(`<p>one</p>${HIDDEN_SELECTION}`);
    });

    it("puts the temporary elements back when the update fails", async () => {
      content.innerHTML = `<p>one</p>${HIDDEN_SELECTION}`;

      await expect(
        adapter.updateContent(() => {
          throw new Error("Update failed.");
        }, false),
      ).rejects.toThrow("Update failed.");

      expect(content.innerHTML).toEqual(`<p>one</p>${HIDDEN_SELECTION}`);
    });

    it("keeps the paragraph holding the selected widget when the next paragraph changes", async () => {
      // A user uploads a file in the first paragraph, which selects the upload widget, while another user uploads a
      // file in the second paragraph. The hidden selection container must not make DiffDOM reuse the local first
      // paragraph for the remote second paragraph.
      const widget = '<span class="widget">file</span>';
      content.innerHTML = `<p>first ${widget}end</p><p>second&nbsp;</p>${HIDDEN_SELECTION}`;
      const firstParagraph = content.firstChild;

      await adapter.updateContent(
        patchTo(`<p>first ${widget}end</p><p>second ${widget} blue</p>`),
        false,
      );

      expect(content.innerHTML).toEqual(
        `<p>first ${widget}end</p><p>second ${widget} blue</p>${HIDDEN_SELECTION}`,
      );
      expect(content.firstChild).toBe(firstParagraph);
    });

    it("keeps the filling character node when the filling characters are still there", async () => {
      content.innerHTML = `<p>one${IMAGE}${FILLING_CHAR_SEQUENCE}two</p>`;
      const text = content.firstChild!.lastChild as Text;
      setFillingChar(text);

      await adapter.updateContent(
        patchTo(`<p>one${IMAGE}${FILLING_CHAR_SEQUENCE}two</p><p>three</p>`),
        false,
      );

      expect(customData.get("cke-fillingChar")).toMatchObject({ $: text });
    });

    it("forgets the filling character node when the filling characters are removed", async () => {
      content.innerHTML = `<p>one${IMAGE}${FILLING_CHAR_SEQUENCE}two</p>`;
      setFillingChar(content.firstChild!.lastChild as Text);

      await adapter.updateContent(patchTo(`<p>one${IMAGE}two</p>`), false);

      expect(customData.has("cke-fillingChar")).toBe(false);
    });

    it("forgets the filling character node when it is removed", async () => {
      content.innerHTML = `<p>one</p><p>${FILLING_CHAR_SEQUENCE}two</p>`;
      setFillingChar(content.lastChild!.firstChild as Text);

      await adapter.updateContent(patchTo("<p>one</p>"), false);

      expect(customData.has("cke-fillingChar")).toBe(false);
    });
  });
});
