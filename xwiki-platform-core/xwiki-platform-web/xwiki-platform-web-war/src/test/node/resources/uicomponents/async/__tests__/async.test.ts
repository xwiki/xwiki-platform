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

// The URLs of the asynchronous rendering requests, in the order in which they have been sent.
let requestedURLs: string[];

// The number of times the layout of the page has been read, each read forcing a layout of the page in a browser.
let layoutReads: number;

let observe: ReturnType<typeof vi.spyOn>;

/**
 * jsdom has no layout, so emulate it: an element is rendered unless it, or one of its ancestors, has the hidden
 * attribute. Count the reads, which are what makes jQuery's ":visible" expensive in a browser.
 */
function emulateLayout(): void {
  layoutReads = 0;
  const size = function (this: HTMLElement): number {
    layoutReads++;
    return this.closest("[hidden]") ? 0 : 10;
  };
  vi.spyOn(HTMLElement.prototype, "offsetWidth", "get").mockImplementation(
    size,
  );
  vi.spyOn(HTMLElement.prototype, "offsetHeight", "get").mockImplementation(
    size,
  );
  vi.spyOn(Element.prototype, "getClientRects").mockImplementation(function (
    this: Element,
  ) {
    layoutReads++;
    return (this.closest("[hidden]")
      ? []
      : [new DOMRect(0, 0, 10, 10)]) as unknown as DOMRectList;
  });
}

/**
 * Browsers reject the jQuery-only ":visible" pseudo-class whatever the element, so jQuery remembers that the selector
 * is not native and evaluates it itself, checking ":visible" before the class. jsdom rejects the selector only for the
 * elements that have the class, and answers false for the others without checking their visibility. Have jQuery
 * fail once on the native evaluation, so that it evaluates the selector itself as it does in a browser.
 */
function evaluateVisibleSelectorLikeBrowsers(): void {
  const element = document.createElement("div");
  element.className = "xwiki-async";
  jquery.find.matchesSelector(element, ".xwiki-async:visible");
}

function loadAsync(): void {
  requirejs = mockRequireJS({
    jquery,
    "xwiki-meta": { wiki: "xwiki" },
  });
  // Evaluate the script in the global scope, where the RequireJS mock is installed.
  (0, eval)(ASYNC_SOURCE);
}

/**
 * Add some HTML to the page and wait for the mutation observers to be notified.
 */
async function addToPage(html: string): Promise<void> {
  document.body.insertAdjacentHTML("beforeend", html);
  await new Promise((resolve) => setTimeout(resolve));
}

function placeholder(name: string, attributes = ""): string {
  return `<div class="xwiki-async" data-xwiki-async-url="/async/${name}" ${attributes}></div>`;
}

describe("Asynchronous rendering placeholders", () => {
  beforeEach(() => {
    requestedURLs = [];
    // Keep the requests pending: the tests only check which placeholders are activated.
    vi.spyOn(jquery, "get").mockImplementation((settings: { url: string }) => {
      requestedURLs.push(settings.url);
      return jquery.Deferred().promise();
    });
    // Collect the observers created by the script, to stop them after each test.
    observe = vi.spyOn(MutationObserver.prototype, "observe");
    evaluateVisibleSelectorLikeBrowsers();
    emulateLayout();
    loadAsync();
  });

  afterEach(() => {
    for (const observer of observe.mock.contexts as MutationObserver[]) {
      observer.disconnect();
    }
    requirejs.restore();
    vi.restoreAllMocks();
    document.body.innerHTML = "";
  });

  it("doesn't read the layout of the page when the added elements hold no placeholder", async () => {
    const rows = Array.from(
      { length: 100 },
      (_, index) => `<tr><td>${index}</td><td><span>cell</span></td></tr>`,
    ).join("");
    await addToPage(
      `<p>paragraph</p>text<table><tbody>${rows}</tbody></table>`,
    );

    expect(layoutReads).toBe(0);
    expect(requestedURLs).toEqual([]);
  });

  it("activates the visible placeholders added to the page", async () => {
    await addToPage(
      placeholder("added") +
        `<div><p>${placeholder("nested")}</p>${placeholder("sibling")}</div>`,
    );

    expect(requestedURLs).toEqual([
      "/async/added?timeout=500&wiki=xwiki",
      "/async/nested?timeout=500&wiki=xwiki",
      "/async/sibling?timeout=500&wiki=xwiki",
    ]);
  });

  it("doesn't activate the hidden placeholders added to the page", async () => {
    await addToPage(
      placeholder("hidden", "hidden") +
        `<div hidden>${placeholder("insideHidden")}</div>` +
        placeholder("visible"),
    );

    expect(requestedURLs).toEqual(["/async/visible?timeout=500&wiki=xwiki"]);
  });

  it("activates the placeholders that are already in the page", () => {
    // The script was loaded on an empty page: load it again on a page holding a hidden placeholder.
    document.body.innerHTML = placeholder("initial", "hidden");
    requirejs.restore();
    loadAsync();

    expect(requestedURLs).toEqual(["/async/initial?timeout=500&wiki=xwiki"]);
  });
});
