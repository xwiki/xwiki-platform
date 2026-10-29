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
import jquery from "jquery";
import { afterEach, beforeEach, describe, expect, it } from "vitest";
import { readFileSync } from "node:fs";
import { resolve } from "node:path";
import type { RequireJSMock } from "@xwiki/platform-test-requirejs";

const SIMPLE_TOOLBAR_SOURCE = readFileSync(
  resolve(
    import.meta.dirname,
    "../../../../../../../main/webapp/resources/js/xwiki/editors/simpletoolbar.js",
  ),
  "utf-8",
);

const TOOLBAR_CONFIGURATION = {
  toolbarElements: [
    {
      id: "bold",
      image: "text_bold.png",
      tagOpen: "**",
      tagClose: "**",
      sampleText: "Text in Bold",
      speedTip: "Bold",
    },
  ],
};

// The configuration element is rendered right before the textarea it applies to, as simpleedittoolbar.vm does.
const EDITOR_HTML = `
  <div class="hidden simpletoolbar-configuration"
    data-xwiki-simpletoolbar-configuration='${JSON.stringify(TOOLBAR_CONFIGURATION)}'></div>
  <textarea id="content" data-syntax="xwiki/2.1">one word</textarea>
`;

let requirejs: RequireJSMock;

function loadToolbar(): void {
  document.body.innerHTML = EDITOR_HTML;
  requirejs = mockRequireJS({ jquery, "xwiki-events-bridge": {} });
  // Evaluate the script in the global scope, where the RequireJS mock is installed.
  (0, eval)(SIMPLE_TOOLBAR_SOURCE);
  // Initialize the toolbar right away instead of waiting for the document ready event.
  jquery(document).trigger("xwiki:dom:updated", { elements: [document.body] });
}

function unloadToolbar(): void {
  requirejs.restore();
  jquery(document).off("xwiki:dom:updated");
  document.body.innerHTML = "";
}

function getBoldButton(): HTMLButtonElement {
  const button = document.querySelector<HTMLButtonElement>(
    ".wikitoolbar-button[title='Bold']",
  );
  expect(button).not.toBeNull();
  return button!;
}

describe("Simple Toolbar", () => {
  beforeEach(loadToolbar);

  afterEach(unloadToolbar);

  it("keeps the focus in the editor when a button is pressed with the mouse", () => {
    const mouseDown = new MouseEvent("mousedown", {
      bubbles: true,
      cancelable: true,
    });
    getBoldButton().dispatchEvent(mouseDown);
    expect(mouseDown.defaultPrevented).toBe(true);
  });

  it("inserts the markup around the selected text when a button is clicked", () => {
    const textarea = document.querySelector<HTMLTextAreaElement>("#content")!;
    textarea.setSelectionRange(4, 8);
    getBoldButton().click();
    expect(textarea.value).toEqual("one **word**");
  });
});
