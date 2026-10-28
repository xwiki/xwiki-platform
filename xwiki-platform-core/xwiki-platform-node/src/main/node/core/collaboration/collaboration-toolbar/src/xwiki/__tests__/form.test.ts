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

import { findXWikiEditForm } from "../form";
import { afterEach, describe, expect, it } from "vitest";

describe("findXWikiEditForm", () => {
  afterEach(() => {
    document.body.innerHTML = "";
  });

  function query(selector: string): HTMLElement {
    return document.querySelector<HTMLElement>(selector)!;
  }

  it("finds the standalone edit form", () => {
    document.body.innerHTML = `
      <form id="other"><div class="buttons"></div></form>
      <form id="edit">
        <div class="bottombuttons sticky-buttons">
          <div class="buttons"></div>
        </div>
      </form>`;
    expect(findXWikiEditForm()).toBe(query("form#edit"));
  });

  it("finds the in-place edit form", () => {
    document.body.innerHTML = `
      <form id="inplace-editing">
        <fieldset class="xform inplace-editing-buttons sticky-buttons">
          <div class="buttons"></div>
        </fieldset>
      </form>`;
    expect(findXWikiEditForm()).toBe(query("form#inplace-editing"));
  });

  it("finds the edit form in fullscreen mode", () => {
    document.body.innerHTML = `
      <form id="edit">
        <div class="cke_maximized">
          <div class="buttons"></div>
        </div>
      </form>`;
    expect(findXWikiEditForm()).toBe(query("form#edit"));
  });

  it("falls back on the page body", () => {
    document.body.innerHTML = `
      <div class="bottombuttons sticky-buttons">
        <div class="buttons"></div>
      </div>`;
    expect(findXWikiEditForm()).toBe(document.body);

    document.body.innerHTML = "";
    expect(findXWikiEditForm()).toBe(document.body);
  });
});
