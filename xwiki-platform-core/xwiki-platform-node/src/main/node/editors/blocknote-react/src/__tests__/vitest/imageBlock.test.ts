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
// @vitest-environment jsdom

import { getLabelFromURL } from "../../blocknote/imageBlock";
import { describe, expect, test } from "vitest";

describe("getLabelFromURL", () => {
  test("takes the file name from an attachment URL", () => {
    expect(getLabelFromURL("/xwiki/bin/download/Some/Page/photo.png")).toBe(
      "photo.png",
    );
  });

  test("ignores the query string", () => {
    expect(
      getLabelFromURL("/xwiki/bin/download/Some/Page/photo.png?width=127"),
    ).toBe("photo.png");
  });

  test("takes the file name from an absolute URL", () => {
    expect(getLabelFromURL("https://example.org/images/photo.png")).toBe(
      "photo.png",
    );
  });

  test("decodes the file name", () => {
    expect(getLabelFromURL("/xwiki/bin/download/A/B/my%20photo.png")).toBe(
      "my photo.png",
    );
  });

  test("keeps a file name whose escape sequence is malformed", () => {
    expect(getLabelFromURL("/xwiki/bin/download/A/B/100%.png")).toBe(
      "100%.png",
    );
  });

  test("returns an empty label when there is nothing to take", () => {
    expect(getLabelFromURL("")).toBe("");
  });
});
