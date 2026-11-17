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
import { completeURL, hasURLScheme } from "../urlCompletion";
import { describe, expect, it } from "vitest";

describe("completeURL", () => {
  it.each([
    ["www.example.org", "https://www.example.org"],
    ["example.org/docs/guide.pdf", "https://example.org/docs/guide.pdf"],
    ["example.org?q=xwiki#results", "https://example.org?q=xwiki#results"],
    ["localhost:8080/xwiki", "https://localhost:8080/xwiki"],
    ["example.org:8443", "https://example.org:8443"],
    ["192.168.0.1/admin", "https://192.168.0.1/admin"],
    ["[::1]:8080", "https://[::1]:8080"],
    ["  www.example.org  ", "https://www.example.org"],
  ])(
    "adds the https scheme to a URL starting with a host name: %s",
    (url, expected) => {
      expect(completeURL(url)).toBe(expected);
    },
  );

  it.each([
    "https://www.example.org",
    "http://localhost:8080/xwiki",
    "ftp://ftp.example.org/file.txt",
    "mailto:alice@example.org",
    "tel:0123456789",
  ])("leaves an absolute URL as is: %s", (url) => {
    expect(completeURL(url)).toBe(url);
  });

  it.each([
    "/xwiki/bin/view/Main/",
    "./docs",
    "../docs",
    "?q=xwiki",
    "#anchor",
    "page",
  ])("leaves a relative URL as is: %s", (url) => {
    expect(completeURL(url)).toBe(url);
  });
});

describe("hasURLScheme", () => {
  it("tells absolute URLs apart from the relative ones and from the host names followed by a port", () => {
    expect(hasURLScheme("https://www.example.org")).toBe(true);
    expect(hasURLScheme("mailto:alice@example.org")).toBe(true);
    expect(hasURLScheme("tel:0123456789")).toBe(true);
    expect(hasURLScheme("localhost:8080")).toBe(false);
    expect(hasURLScheme("example.org:8443/docs")).toBe(false);
    expect(hasURLScheme("/xwiki/bin/view/Main/")).toBe(false);
  });
});
