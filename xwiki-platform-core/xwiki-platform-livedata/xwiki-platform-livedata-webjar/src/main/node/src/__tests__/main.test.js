/*
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
import { beforeEach, describe, expect, it, vi } from "vitest";
import $ from "jquery";

const init = vi.fn();
const delayPageReady = vi.fn((promise) => promise);

vi.mock("../services/init.js", () => ({ init: (...args) => init(...args) }));
vi.mock("@xwiki/platform-livedata-ui", () => ({ populateStore: () => {} }));

// The plugin is registered as a RequireJS module, on the jQuery instance it is given.
globalThis.require = (dependencies, callback) => callback($, { delayPageReady });

await import("../main.js");

const liveDataMarkup = '<div class="liveData" data-config=\'{"id":"test"}\'></div>';

/**
 * Let the pending dynamic imports, promise callbacks and jQuery ready callbacks run.
 */
async function settle() {
  for (let i = 0; i < 5; i++) {
    await new Promise((resolve) => setTimeout(resolve, 0));
  }
}

/**
 * @returns the number of live data displays that were started, each display delaying the page ready
 */
function startedDisplays() {
  return delayPageReady.mock.calls.filter(([, reason]) => reason === "livedata:display").length;
}

describe("$.fn.liveData", () => {
  beforeEach(async () => {
    // The plugin displays the live data elements found on the page as soon as the document is ready. Let that first
    // pass run before clearing the counters, so that the tests below start from a known state.
    await settle();
    init.mockReset();
    init.mockResolvedValue({});
    delayPageReady.mockClear();
    document.body.innerHTML = "";
  });

  it("displays a live data element", async () => {
    document.body.innerHTML = liveDataMarkup;
    const element = document.querySelector(".liveData");

    $(element).liveData();
    await settle();

    expect(startedDisplays()).toBe(1);
    expect(init).toHaveBeenCalledTimes(1);
    expect(init.mock.calls[0][0]).toBe(element);
    expect(element.dataset.config).toBe('{"id":"test"}');
  });

  it("does not display the same element twice when asked again before the first display completes", async () => {
    document.body.innerHTML = liveDataMarkup;
    const element = document.querySelector(".liveData");

    $(element).liveData();
    $(element).liveData();
    await settle();

    expect(startedDisplays()).toBe(1);
  });

  it("does not display an already displayed element again on xwiki:dom:updated", async () => {
    document.body.innerHTML = liveDataMarkup;

    $(document).trigger("xwiki:dom:updated");
    await settle();
    expect(startedDisplays()).toBe(1);

    $(document).trigger("xwiki:dom:updated");
    await settle();
    expect(startedDisplays()).toBe(1);
  });

  it("does not display an element whose configuration has already been consumed", async () => {
    // The editor puts the content it edits back in the page, so the displayed live data comes back as a new element,
    // without the configuration that displaying it consumed and without the jQuery marker of the node it replaces.
    document.body.innerHTML = '<div class="liveData"><table><tbody><tr></tr></tbody></table></div>';
    const element = document.querySelector(".liveData");

    $(document).trigger("xwiki:dom:updated");
    await settle();

    expect(startedDisplays()).toBe(0);
    expect(init).not.toHaveBeenCalled();
    expect(element.querySelector("tr")).not.toBeNull();
    expect(element.hasAttribute("data-config")).toBe(false);
  });

  it("displays an element without configuration when one is passed to the plugin", async () => {
    document.body.innerHTML = '<div class="liveData"></div>';
    const element = document.querySelector(".liveData");

    $(element).liveData({ id: "passed" });
    await settle();

    expect(startedDisplays()).toBe(1);
    expect(element.dataset.config).toBe('{"id":"passed"}');
  });

  it("displays a live data element that replaced an already displayed one", async () => {
    document.body.innerHTML = liveDataMarkup;

    $(document).trigger("xwiki:dom:updated");
    await settle();
    expect(startedDisplays()).toBe(1);

    // The editor re-inserts the content it edited as HTML, so the live data element is a new, freshly rendered node
    // that has to be displayed again.
    document.body.innerHTML = liveDataMarkup;
    $(document).trigger("xwiki:dom:updated");
    await settle();

    expect(startedDisplays()).toBe(2);
    expect(init.mock.calls[1][0]).toBe(document.querySelector(".liveData"));
  });
});
