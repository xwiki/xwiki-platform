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
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { readFileSync } from "node:fs";
import { resolve } from "node:path";
import type { RequireJSMock } from "@xwiki/platform-test-requirejs";

const PLUGIN_SOURCE = readFileSync(
  resolve(
    import.meta.dirname,
    "../../../../main/webjar/xwiki-selection/plugin.js",
  ),
  "utf-8",
);

type Listener = (event?: unknown) => void;

type XWikiSelection = {
  TextSelection: new () => unknown;
  saveSelection(editor: unknown): void;
  restoreSelection(
    editor: unknown,
    options?: Record<string, unknown>,
  ): Promise<void>;
  getSelection(editor: unknown): Range[];
  setSelection(editor: unknown, ranges: Range[], focus: unknown): void;
};

type PluginDefinition = {
  beforeInit(editor: unknown): void;
};

let requirejs: RequireJSMock;
let xwikiSelection: XWikiSelection;
let plugin: PluginDefinition;
let listeners: Map<string, Listener[]>;
let editor: Record<string, unknown>;
let content: HTMLElement;
let ranges: Range[];

/**
 * Loads the plugin, with the minimum of CKEditor, jQuery and RequireJS needed to evaluate it.
 */
function loadPlugin(): void {
  const plugins: Record<string, unknown> = {
    add: (name: string, definition: PluginDefinition) => {
      if (name === "xwiki-selection") {
        plugin = definition;
      }
    },
    notification: class {
      show(): void {}
      hide(): void {}
    },
  };
  Object.assign(globalThis, {
    CKEDITOR: { plugins },
    jQuery: () => {},
    Worker: class {},
  });
  Object.defineProperty(document, "currentScript", {
    configurable: true,
    value: { src: "http://localhost/xwiki-selection/plugin.js" },
  });
  requirejs = mockRequireJS({
    jquery: () => {},
    "node-module!fast-diff": () => [],
    // The text selection module is defined before the scroll utilities module that it depends on.
    scrollUtils: {},
  });
  // Evaluate the script in the global scope, where the fake globals are installed.
  (0, eval)(PLUGIN_SOURCE);
  xwikiSelection = plugins.xwikiSelection as XWikiSelection;
}

/**
 * @returns a fake editor, with the minimum needed to save and restore the selection, whose editing area has the focus
 */
function createEditor(): Record<string, unknown> {
  listeners = new Map();
  const editable = { $: content };
  return {
    _: {},
    on: (eventName: string, listener: Listener) => {
      listeners.set(eventName, [...(listeners.get(eventName) ?? []), listener]);
    },
    editable: () => editable,
    unlockSelection: () => {},
    localization: { get: () => "" },
    ui: { contentsElement: { $: { contains: () => true } } },
  };
}

function createCaret(node: Node, offset: number): Range {
  const range = document.createRange();
  range.setStart(node, offset);
  return range;
}

function fire(eventName: string): void {
  listeners.get(eventName)?.forEach((listener) => listener());
}

describe("xwikiSelection", () => {
  beforeEach(() => {
    loadPlugin();

    content = document.createElement("div");
    content.innerHTML = "<p>one</p>";
    document.body.append(content);
    ranges = [createCaret(content.firstChild!.firstChild!, 1)];

    editor = createEditor();
    plugin.beforeInit(editor);

    xwikiSelection.TextSelection = class {
      update(): unknown {
        return this;
      }
    };
    vi.spyOn(xwikiSelection, "getSelection").mockReturnValue(ranges);
    vi.spyOn(xwikiSelection, "setSelection").mockImplementation(() => {});
  });

  afterEach(() => {
    vi.restoreAllMocks();
    requirejs.restore();
    content.remove();
    delete (document as unknown as Record<string, unknown>).currentScript;
  });

  describe("restoreSelection", () => {
    it("doesn't scroll the selection into view when the content is not overwritten", async () => {
      xwikiSelection.saveSelection(editor);

      await xwikiSelection.restoreSelection(editor, { ranges, async: false });

      expect(xwikiSelection.setSelection).toHaveBeenCalledWith(editor, ranges, {
        preventScroll: true,
      });
    });

    it("scrolls the selection into view when the content is overwritten after the selection is saved", async () => {
      xwikiSelection.saveSelection(editor);
      fire("setData");

      await xwikiSelection.restoreSelection(editor, { ranges, async: false });

      expect(xwikiSelection.setSelection).toHaveBeenCalledWith(editor, ranges, {
        preventScroll: false,
      });
    });

    it("doesn't scroll the selection into view when the content was overwritten before the selection is saved", async () => {
      xwikiSelection.saveSelection(editor);
      fire("setData");
      await xwikiSelection.restoreSelection(editor, { ranges, async: false });

      // Save the selection again, e.g. before applying a remote change that doesn't overwrite the content.
      xwikiSelection.saveSelection(editor);
      await xwikiSelection.restoreSelection(editor, { ranges, async: false });

      expect(xwikiSelection.setSelection).toHaveBeenLastCalledWith(
        editor,
        ranges,
        { preventScroll: true },
      );
    });
  });
});
