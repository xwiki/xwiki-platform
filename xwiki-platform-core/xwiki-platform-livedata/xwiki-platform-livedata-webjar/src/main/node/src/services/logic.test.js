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

vi.mock("vue", async (importOriginal) => {
  // A minimal application, that is never mounted for real.
  const app = {};
  app.mixin = app.provide = app.use = app.mount = () => app;
  return { ...(await importOriginal()), createApp: () => app };
});
vi.mock("@/components/XWikiLivedata.vue", () => ({ default: {} }));
vi.mock("@/services/liveDataSource.js", () => ({ init: () => ({}) }));
vi.mock("@/services/editBus.js", () => ({ init: () => ({}) }));
vi.mock("@/services/i18nResolver.js", () => ({ i18nResolver: () => Promise.resolve() }));

const { Logic } = await import("./logic.js");

describe("Logic", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  /**
   * Create the logic of the given live data markup and return whether its content is trusted.
   */
  function isContentTrusted(content) {
    vi.spyOn(Logic.prototype, "changeLayout").mockImplementation(() => {});
    vi.spyOn(Logic.prototype, "setEditBus").mockImplementation(() => {});
    vi.spyOn(Logic.prototype, "registerPanel").mockImplementation(() => {});

    const config = JSON.stringify({ meta: {}, data: { entries: [{}] } });
    document.body.innerHTML = '<div class="liveData"></div>';
    const element = document.querySelector(".liveData");
    element.setAttribute("data-config", config);
    element.innerHTML = content;

    return new Logic(element, undefined).isContentTrusted();
  }

  it("trusts the content when the Live Data macro says so", () => {
    expect(isContentTrusted('<script type="application/json" data-config-content-trusted="true"></script>'))
      .toBe(true);
  });

  it("doesn't trust the content when the Live Data macro says so", () => {
    expect(isContentTrusted('<script type="application/json" data-config-content-trusted="false"></script>'))
      .toBe(false);
  });

  it("doesn't trust the content silently when the trust script is missing", () => {
    const consoleError = vi.spyOn(console, "error");

    // The WYSIWYG editor replaces the script elements from the macro output with comments.
    expect(isContentTrusted("<!--{cke_protected}%3Cscript%20type%3D%22application%2Fjson%22%20" +
      "data-config-content-trusted%3D%22true%22%3E%3C%2Fscript%3E-->")).toBe(false);
    expect(consoleError).not.toHaveBeenCalled();
  });
});
