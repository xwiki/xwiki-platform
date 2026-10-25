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
import { afterEach, describe, expect, it, vi } from "vitest";

const createApp = vi.fn();
vi.mock("vue", async (importOriginal) => ({ ...(await importOriginal()), createApp }));
vi.mock("@xwiki/platform-livedata-ui", () => ({ XWikiLivedata: {} }));
vi.mock("@xwiki/platform-livedata-xwiki", () => ({
  XWikiLiveDataSource: class {},
  initTranslationsBuilder: () => () => () => {},
}));
vi.mock("xwiki-platform-localization-webjar", () => ({ resolver: {} }));

const { init } = await import("../init.js");

describe("init", () => {
  afterEach(() => {
    vi.restoreAllMocks();
    createApp.mockReset();
  });

  /**
   * Display the given live data markup and return the contentTrusted property passed to the live data component.
   */
  function getContentTrusted(markup) {
    // A minimal application, that is never mounted for real.
    const app = {};
    app.mixin = app.provide = app.use = app.mount = () => app;
    createApp.mockReturnValue(app);

    document.body.innerHTML = markup;
    init(document.querySelector(".liveData"), undefined);

    return createApp.mock.calls[0][1].contentTrusted;
  }

  it("trusts the content when the Live Data macro says so", () => {
    expect(getContentTrusted('<div class="liveData" data-config="{}">' +
      '<script type="application/json" data-config-content-trusted="true"></script></div>')).toBe(true);
  });

  it("doesn't trust the content when the Live Data macro says so", () => {
    expect(getContentTrusted('<div class="liveData" data-config="{}">' +
      '<script type="application/json" data-config-content-trusted="false"></script></div>')).toBe(false);
  });

  it("doesn't trust the content silently when the trust script is missing", () => {
    const consoleError = vi.spyOn(console, "error");

    // The WYSIWYG editor replaces the script elements from the macro output with comments.
    expect(getContentTrusted('<div class="liveData" data-config="{}"><!--{cke_protected}%3Cscript%20type%3D%22' +
      'application%2Fjson%22%20data-config-content-trusted%3D%22true%22%3E%3C%2Fscript%3E--></div>')).toBe(false);
    expect(consoleError).not.toHaveBeenCalled();
  });

  it("leaves the element untouched when it has no configuration", async () => {
    document.body.innerHTML = '<div class="liveData"><table id="displayed"></table></div>';
    const element = document.querySelector(".liveData");

    await expect(init(element, undefined)).rejects.toThrow(/Missing data-config attribute/);

    // An already displayed live data must survive a redundant initialization attempt.
    expect(element.querySelector("#displayed")).not.toBeNull();
  });
});
