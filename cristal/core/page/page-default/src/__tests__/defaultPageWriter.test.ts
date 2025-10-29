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
import { DefaultPageWriter } from "../defaultPageWriter";
import { describe, expect, it } from "vitest";

describe("defaultPageWriter", () => {
  it("writeEmptyObject", () => {
    const defaultPageWriter = new DefaultPageWriter();
    const page = defaultPageWriter.writePage({ content: "" });
    expect(page).toBe("");
  });

  it("writeObjectNoPageContent", () => {
    const defaultPageWriter = new DefaultPageWriter();
    const page = defaultPageWriter.writePage({ key: "value", content: "" });
    expect(page).toBe(`---
key: value
---
`);
  });

  it("writeObjectOnlyContent", () => {
    const defaultPageWriter = new DefaultPageWriter();
    const page = defaultPageWriter.writePage({ content: "value" });
    expect(page).toBe("value");
  });

  it("writePageWithMetadata", () => {
    const defaultPageWriter = new DefaultPageWriter();
    const page = defaultPageWriter.writePage({ content: "value", a: 42 });
    expect(page).toBe(`---
a: 42
---
value`);
  });
});
