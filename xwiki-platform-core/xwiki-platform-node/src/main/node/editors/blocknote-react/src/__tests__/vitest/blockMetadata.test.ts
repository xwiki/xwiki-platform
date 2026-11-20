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
import { createBlockMetadataStore } from "../../misc/blockMetadata";
import { describe, expect, test } from "vitest";
import type { BlockMetadataKey } from "../../misc/blockMetadata";

const CALL: BlockMetadataKey = { name: "macroCall" };
const OUTPUT: BlockMetadataKey = { name: "macroOutput" };

describe("createBlockMetadataStore", () => {
  test("keeps the metadata entries set for a block", () => {
    const metadata = createBlockMetadataStore();

    expect(metadata.get("alice")).toBeUndefined();

    metadata.set("alice", CALL, "{}");
    metadata.set("alice", OUTPUT, []);

    expect(metadata.get("alice")).toEqual({ macroCall: "{}", macroOutput: [] });
    expect(metadata.get("bob")).toBeUndefined();
  });

  test("removes an entry set to undefined, and the metadata of a block left without entries", () => {
    const metadata = createBlockMetadataStore();
    metadata.set("alice", CALL, "{}");
    metadata.set("alice", OUTPUT, []);

    metadata.set("alice", OUTPUT, undefined);
    expect(metadata.get("alice")).toEqual({ macroCall: "{}" });

    metadata.set("alice", CALL, undefined);
    expect(metadata.get("alice")).toBeUndefined();

    // Removing an entry of a block without metadata doesn't create it.
    metadata.set("bob", CALL, undefined);
    expect(metadata.get("bob")).toBeUndefined();
  });

  test("doesn't change the metadata already returned", () => {
    const metadata = createBlockMetadataStore();
    metadata.set("alice", CALL, "{}");
    const before = metadata.get("alice");

    metadata.set("alice", CALL, '{"id":"info"}');

    expect(before).toEqual({ macroCall: "{}" });
    expect(metadata.get("alice")).toEqual({ macroCall: '{"id":"info"}' });
  });
});
