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
import { transformTranslation } from "../transformTranslation";
import { describe, expect, it } from "vitest";

describe("transformTranslation", () => {
  it("string with no parameters", () => {
    expect(transformTranslation("Hello World")).toBe("Hello World");
  });

  it("replace parameters", () => {
    expect(transformTranslation("{0} + {1} = {2}", "1", "2", "3")).toBe(
      "1 + 2 = 3",
    );
  });

  it("replace all the parameters of same index", () => {
    expect(transformTranslation("{0} and {0}", "A")).toBe("A and A");
  });

  it("unescapes ''", () => {
    expect(transformTranslation("don''t won''t {0}", "x")).toBe(
      "don't won't x",
    );
  });
});
