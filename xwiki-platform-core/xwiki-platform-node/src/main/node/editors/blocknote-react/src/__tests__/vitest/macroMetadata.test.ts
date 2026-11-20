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
import {
  getMacroOutput,
  setMacroOutput,
} from "../../features/macros/macroMetadata";
import { createBlockMetadataStore } from "../../misc/blockMetadata";
import { describe, expect, test } from "vitest";

describe("getMacroOutput", () => {
  const call = JSON.stringify({ name: "info", parameters: {} });
  const output = [{ type: "paragraph", content: "Information" }];

  test("returns the output of an unchanged macro call", () => {
    const metadata = createBlockMetadataStore();
    setMacroOutput(metadata, "m1", call, output);

    expect(getMacroOutput(metadata, "m1", call)).toBe(output);
  });

  test("returns nothing once the macro call is modified, or for an unknown macro call", () => {
    const metadata = createBlockMetadataStore();
    setMacroOutput(metadata, "m1", call, output);

    expect(getMacroOutput(metadata, "m1", "{}")).toBeUndefined();
    expect(getMacroOutput(metadata, "m2", call)).toBeUndefined();
    expect(getMacroOutput(metadata, "", call)).toBeUndefined();
    expect(getMacroOutput(undefined, "m1", call)).toBeUndefined();
  });

  test("keeps the other metadata of the macro block", () => {
    const metadata = createBlockMetadataStore();
    metadata.set("m1", { name: "xwikiParameters" }, { class: "box" });
    setMacroOutput(metadata, "m1", call, output);

    expect(metadata.get("m1")).toEqual({
      xwikiParameters: { class: "box" },
      macroCall: call,
      macroOutput: output,
    });
  });
});
