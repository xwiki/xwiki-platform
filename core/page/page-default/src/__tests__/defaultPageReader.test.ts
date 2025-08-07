/**
 * See the LICENSE file distributed with this work for additional
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
import { DefaultPageReader } from "../defaultPageReader";
import { describe, expect, it } from "vitest";

describe("defaultPageReader", () => {
  it("parseWhenNotFrontMatter", () => {
    const defaultPageReader = new DefaultPageReader();
    const metadata = defaultPageReader.readPage("PageContent");
    expect(metadata).toEqual({
      content: "PageContent",
    });
  });

  it("parseWhenMalformedFrontMatter", () => {
    const defaultPageReader = new DefaultPageReader();
    const pageContent = `---
    PageContent`;
    const metadata = defaultPageReader.readPage(pageContent);
    expect(metadata).toEqual({
      content: pageContent,
    });
  });

  it("parseWhenEmptyFrontMatter", () => {
    const defaultPageReader = new DefaultPageReader();
    const pageContent = `---
---
PageContent`;
    const metadata = defaultPageReader.readPage(pageContent);
    expect(metadata).toEqual({
      content: "PageContent",
    });
  });

  it("parseWhenWellformedFrontMatter", () => {
    const defaultPageReader = new DefaultPageReader();
    const pageContent = `---
a: 1
---
PageContent`;
    const metadata = defaultPageReader.readPage(pageContent);
    expect(metadata).toEqual({
      a: 1,
      content: "PageContent",
    });
  });
});
