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

import { escapeHtml, produceHtmlEl } from "..";
import { describe, expect, it } from "vitest";

describe("HTML", () => {
  it("should produce basic HTML correctly", () => {
    expect(produceHtmlEl("span", {}, false)).toBe("<span></span>");
    expect(produceHtmlEl("div", {}, false)).toBe("<div></div>");
    expect(produceHtmlEl("img", {}, false)).toBe("<img>");
    expect(produceHtmlEl("script", {}, false)).toBe("<script></script>");
  });

  it("should produce HTML attributes correctly", () => {
    expect(produceHtmlEl("p", { style: "color: blue;" }, false)).toBe(
      '<p style="color: blue;"></p>',
    );

    expect(
      produceHtmlEl("input", { type: "checkbox", readonly: "true" }, false),
    ).toBe('<input type="checkbox" readonly="true">');
  });

  it("should produce HTML with content correctly", () => {
    expect(produceHtmlEl("p", {}, "Hello world!")).toBe("<p>Hello world!</p>");
    expect(produceHtmlEl("span", {}, "Hello world!")).toBe(
      "<span>Hello world!</span>",
    );
    expect(produceHtmlEl("span", {}, "<strong>Hello world!</strong>")).toBe(
      "<span><strong>Hello world!</strong></span>",
    );
  });

  it("should escape HTML correctly", () => {
    expect(escapeHtml("<script>maliciousScript()</script>")).toBe(
      "&lt;script&gt;maliciousScript()&lt;/script&gt;",
    );

    expect(escapeHtml('<iframe src="somewhere" />')).toBe(
      '&lt;iframe src="somewhere" /&gt;',
    );
  });
});
