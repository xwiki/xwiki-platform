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
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { readFileSync } from "node:fs";
import { resolve } from "node:path";
import type { RequireJSMock } from "@xwiki/platform-test-requirejs";

const ASYNC_SOURCE = readFileSync(
  resolve(
    import.meta.dirname,
    "../../../../../../main/webapp/resources/uicomponents/async/async.js",
  ),
  "utf-8",
);

let requirejs: RequireJSMock;
let updatedElements: Element[][];

function onDOMUpdated(_event: unknown, data: { elements: Element[] }): void {
  updatedElements.push(data.elements);
}

/**
 * Load the asynchronous rendering script on a page holding a placeholder, whose asynchronous result is the given HTML.
 *
 * @param result - the HTML returned by the asynchronous renderer
 */
function renderAsync(result: string): void {
  vi.spyOn(jquery, "get").mockImplementation(((settings: {
    complete: (xhr: unknown) => void;
  }) => {
    settings.complete({ status: 200, responseText: result });
  }) as unknown as typeof jquery.get);
  document.body.innerHTML = `
    <p id="before">Before</p>
    <div class="xwiki-async" data-xwiki-async-url="/xwiki/asyncrenderer/test"></div>
    <p id="after">After</p>
  `;
  // Evaluate the script in the global scope, where the RequireJS mock is installed.
  (0, eval)(ASYNC_SOURCE);
}

describe("Asynchronous rendering", () => {
  beforeEach(() => {
    requirejs = mockRequireJS({
      jquery,
      "xwiki-meta": { wiki: "xwiki" },
    });
    updatedElements = [];
    jquery(document).on("xwiki:dom:updated", onDOMUpdated);
  });

  afterEach(() => {
    jquery(document).off("xwiki:dom:updated", onDOMUpdated);
    requirejs.restore();
    vi.restoreAllMocks();
    document.body.innerHTML = "";
  });

  it("replaces the placeholder with the result", () => {
    renderAsync('<div class="chart">Chart</div>');

    expect(document.querySelector(".xwiki-async")).toBeNull();
    expect(
      Array.from(document.body.children).map((element) => element.outerHTML),
    ).toEqual([
      '<p id="before">Before</p>',
      '<div class="chart">Chart</div>',
      '<p id="after">After</p>',
    ]);
  });

  it("targets the inserted elements with the DOM update event", () => {
    renderAsync(
      'text <span class="first">First</span> <div class="second">Second</div>',
    );

    expect(updatedElements).toHaveLength(1);
    expect(updatedElements[0]).toEqual([
      document.querySelector(".first"),
      document.querySelector(".second"),
    ]);
  });

  it("doesn't execute the inline scripts of the result", () => {
    renderAsync(
      '<div class="chart">Chart</div><script>window.inlineScriptExecuted = true;</script>',
    );

    expect(document.querySelector("script")).toBeNull();
    expect("inlineScriptExecuted" in window).toBe(false);
  });
});
