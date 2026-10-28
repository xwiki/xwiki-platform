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

// Publish the entity reference API on the global XWiki object, as the corresponding bundle does on every page.
import "xwiki-platform-web-webjar";
import { JSDOM } from "jsdom";
import { beforeEach, describe, expect, it } from "vitest";
import { readFileSync } from "node:fs";
import { resolve } from "node:path";

// xwiki.js uses Prototype.js, which isn't published on npm, so the Maven build extracts it from its WebJar. See the
// unpack-prototype-for-javascript-tests execution in the pom.xml of this module.
const PROTOTYPE_SOURCE = readFileSync(
  resolve(
    import.meta.dirname,
    "../../../../../../../target/test-webjars/prototype.js",
  ),
  "utf-8",
);
const XWIKI_SOURCE = readFileSync(
  resolve(
    import.meta.dirname,
    "../../../../../../main/webapp/resources/js/xwiki/xwiki.js",
  ),
  "utf-8",
);

// The tabs at the bottom of a page in view mode, as docextra.vm renders them.
const DOC_EXTRA_HTML = `
  <div id="docextraanchors"><span id="Commentsanchor"></span><span id="Historyanchor"></span></div>
  <ul id="docExtrasTabsUl"><li id="Commentstab"></li><li id="Historytab"></li></ul>
  <div id="docextrapanes">
    <div id="Commentspane" class="hidden empty"></div>
    <div id="Historypane" class="hidden empty"></div>
  </div>
`;

/**
 * The options passed to Prototype.js' Ajax.Request, limited to what the code under test uses.
 */
type AjaxRequestOptions = {
  onSuccess: (response: { responseText: string }) => void;
  onComplete: (response: { status: number }) => void;
};

/**
 * The part of the window that the code under test publishes or that the test replaces.
 */
type XWikiWindow = Window & {
  XWiki: {
    displayDocExtra: (
      extraID: string,
      extraTemplate: string,
      scrollToAnchor: boolean,
    ) => void;
  };
  Ajax: { Request: (url: string, options: AjaxRequestOptions) => void };
  require: () => void;
  define: () => void;
  eval: (source: string) => void;
};

describe("XWiki.displayDocExtra", () => {
  let window: XWikiWindow;

  // The requests started to load the panes, in the order in which they have been started.
  let requests: AjaxRequestOptions[];

  beforeEach(() => {
    // xwiki.js keeps the state of the panes on the window, so each test gets a new one. The scripts are evaluated in
    // that window the way the browser evaluates them in a page, Prototype.js first.
    window = new JSDOM(`<html><body>${DOC_EXTRA_HTML}</body></html>`, {
      runScripts: "dangerously",
    }).window as unknown as XWikiWindow;
    window.eval(PROTOTYPE_SOURCE);
    // Copy the API, instead of sharing it, because xwiki.js adds its own API to the XWiki object it finds.
    window.XWiki = { ...(globalThis as unknown as XWikiWindow).XWiki };
    // xwiki.js also uses RequireJS for code that is not under test.
    window.require = () => {};
    window.define = () => {};
    window.eval(XWIKI_SOURCE);

    // Collect the requests instead of sending them, so that the test decides when each pane is loaded and in which
    // order, which is what these tests are about.
    requests = [];
    window.Ajax.Request = function (_url: string, options: AjaxRequestOptions) {
      requests.push(options);
    };
  });

  /**
   * Answers the request that loads a pane, the way Prototype.js' Ajax.Request does when the response arrives.
   *
   * @param index - the position of the request in the order in which the requests have been started
   * @param content - the HTML of the pane
   */
  function answer(index: number, content: string): void {
    requests[index].onSuccess({ responseText: content });
    requests[index].onComplete({ status: 200 });
  }

  function isDisplayed(extraID: string): boolean {
    return (
      window.document.getElementById(extraID + "tab")!.className === "active" &&
      window.document.getElementById(extraID + "pane")!.className === ""
    );
  }

  it("displays the pane requested last even when an earlier pane is loaded after it", () => {
    // The first pane is loaded when the page is loaded, and the user opens another pane before it has been loaded.
    window.XWiki.displayDocExtra("Comments", "commentsinline.vm", false);
    window.XWiki.displayDocExtra("History", "historyinline.vm", false);

    // The pane requested last is loaded first, then the slow one is loaded.
    answer(1, "<div>history</div>");
    answer(0, "<div>comments</div>");

    expect(isDisplayed("History")).toBe(true);
    expect(isDisplayed("Comments")).toBe(false);
  });

  it("doesn't load a pane again when it has been loaded but not displayed", () => {
    window.XWiki.displayDocExtra("Comments", "commentsinline.vm", false);
    window.XWiki.displayDocExtra("History", "historyinline.vm", false);
    answer(1, "<div>history</div>");
    answer(0, "<div>comments</div>");

    // The user now opens the pane that has been loaded but not displayed.
    window.XWiki.displayDocExtra("Comments", "commentsinline.vm", false);

    expect(requests).toHaveLength(2);
    expect(isDisplayed("Comments")).toBe(true);
    expect(isDisplayed("History")).toBe(false);
    // The content would be inserted a second time if the pane were loaded again.
    expect(window.document.getElementById("Commentspane")!.innerHTML).toBe(
      "<div>comments</div>",
    );
  });
});
