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
import { describe, expect, it, vi } from "vitest";
import { h, useId } from "vue";

// Displays an identifier generated with useId, to check that the identifiers are unique in the page.
vi.mock("@xwiki/platform-livedata-ui", () => ({
  XWikiLivedata: { setup: () => () => h("div", { class: "generated", id: useId() }) },
}));
vi.mock("@xwiki/platform-livedata-xwiki", () => ({
  XWikiLiveDataSource: class {},
  initTranslationsBuilder: () => () => () => {},
}));
vi.mock("xwiki-platform-localization-webjar", () => ({ resolver: {} }));

const { init } = await import("../init.js");

describe("init", () => {
  it("leaves the element untouched when it has no configuration", async () => {
    document.body.innerHTML = '<div class="liveData"><table id="displayed"></table></div>';
    const element = document.querySelector(".liveData");

    await expect(init(element, undefined)).rejects.toThrow(/Missing data-config attribute/);

    // An already displayed live data must survive a redundant initialization attempt.
    expect(element.querySelector("#displayed")).not.toBeNull();
  });

  it("generates distinct identifiers in each live data of the page", () => {
    const config = '<script type="application/json">{}</script>';
    document.body.innerHTML =
      `<div class="liveData" data-config="{}">${config}</div><div class="liveData" data-config="{}">${config}</div>`;

    for (const element of document.querySelectorAll(".liveData")) {
      init(element, undefined);
    }

    const ids = [...document.querySelectorAll(".generated")].map((generated) => generated.id);
    expect(ids.length).toBe(2);
    expect(ids[0]).not.toBe(ids[1]);
  });
});
