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
import { Doc, applyUpdate, encodeStateAsUpdate } from "yjs";
import type {
  BlockMetadataKey,
  BlockMetadataStore,
  SharedBlockMetadata,
} from "../../misc/blockMetadata";

const REFERENCE: BlockMetadataKey = { name: "xwikiReference", shared: true };
const FREESTANDING: BlockMetadataKey = {
  name: "xwikiFreestanding",
  shared: true,
};
const OUTPUT: BlockMetadataKey = { name: "macroOutput" };

/**
 * @returns two shared documents kept in sync, as if edited by two users in the same realtime collaboration session
 */
function createSession(): [Doc, Doc] {
  const alice = new Doc();
  const bob = new Doc();
  alice.on("update", (update: Uint8Array) => applyUpdate(bob, update));
  bob.on("update", (update: Uint8Array) => applyUpdate(alice, update));
  return [alice, bob];
}

function getSharedMetadata(doc: Doc): SharedBlockMetadata {
  return doc.getMap("content:metadata") as SharedBlockMetadata;
}

/**
 * @param doc - the shared document of the realtime collaboration session
 * @param initialize - whether the created store initializes the session
 * @param entries - the metadata entries set before joining the session, as if extracted from the loaded content
 * @returns a metadata store connected to the given realtime collaboration session
 */
function joinSession(
  doc: Doc,
  initialize: boolean,
  entries: [string, BlockMetadataKey, unknown][] = [],
): BlockMetadataStore {
  const metadata = createBlockMetadataStore();
  entries.forEach(([id, key, value]) => metadata.set(id, key, value));
  metadata.connect(getSharedMetadata(doc), { initialize });
  return metadata;
}

describe("createBlockMetadataStore", () => {
  test("keeps the metadata entries set for a block", () => {
    const metadata = createBlockMetadataStore();

    expect(metadata.get("alice")).toBeUndefined();

    metadata.set("alice", REFERENCE, "Some.Page");
    metadata.set("alice", FREESTANDING, true);

    expect(metadata.get("alice")).toEqual({
      xwikiReference: "Some.Page",
      xwikiFreestanding: true,
    });
    expect(metadata.get("bob")).toBeUndefined();
  });

  test("removes an entry set to undefined, and the metadata of a block left without entries", () => {
    const metadata = createBlockMetadataStore();
    metadata.set("alice", REFERENCE, "Some.Page");
    metadata.set("alice", FREESTANDING, true);

    metadata.set("alice", FREESTANDING, undefined);
    expect(metadata.get("alice")).toEqual({ xwikiReference: "Some.Page" });

    metadata.set("alice", REFERENCE, undefined);
    expect(metadata.get("alice")).toBeUndefined();

    // Removing an entry of a block without metadata doesn't create it.
    metadata.set("bob", REFERENCE, undefined);
    expect(metadata.get("bob")).toBeUndefined();
  });

  test("doesn't change the metadata already returned", () => {
    const metadata = createBlockMetadataStore();
    metadata.set("alice", REFERENCE, "Some.Page");
    const before = metadata.get("alice");

    metadata.set("alice", REFERENCE, "Other.Page");

    expect(before).toEqual({ xwikiReference: "Some.Page" });
    expect(metadata.get("alice")).toEqual({ xwikiReference: "Other.Page" });
  });

  test("returns the shared and the private entries of a block together", () => {
    const metadata = createBlockMetadataStore();
    metadata.set("m1", REFERENCE, "Some.Page");
    metadata.set("m1", OUTPUT, ["output"]);

    expect(metadata.get("m1")).toEqual({
      xwikiReference: "Some.Page",
      macroOutput: ["output"],
    });

    metadata.set("m1", OUTPUT, undefined);
    expect(metadata.get("m1")).toEqual({ xwikiReference: "Some.Page" });
  });

  test("shares only the shared entries with the participants that join the session", () => {
    const [aliceDoc, bobDoc] = createSession();
    const alice = joinSession(aliceDoc, true, [
      ["p1", REFERENCE, "Some.Page"],
      ["m1", OUTPUT, ["Alice's output"]],
    ]);
    // Bob's shared entries are about the content he loaded, which is replaced by the content of the session.
    const bob = joinSession(bobDoc, false, [
      ["p2", REFERENCE, "Other.Page"],
      ["m1", OUTPUT, ["Bob's output"]],
    ]);

    expect(bob.get("p1")).toEqual({ xwikiReference: "Some.Page" });
    expect(bob.get("p2")).toBeUndefined();
    expect(bob.get("m1")).toEqual({ macroOutput: ["Bob's output"] });
    expect(alice.get("m1")).toEqual({ macroOutput: ["Alice's output"] });
  });

  test("shares the changes of the shared entries", () => {
    const [aliceDoc, bobDoc] = createSession();
    const alice = joinSession(aliceDoc, true);
    const bob = joinSession(bobDoc, false);

    alice.set("p1", REFERENCE, { type: "doc", reference: "Some.Page" });
    alice.set("p1", OUTPUT, ["output"]);
    expect(bob.get("p1")).toEqual({
      xwikiReference: { type: "doc", reference: "Some.Page" },
    });

    alice.set("p1", REFERENCE, undefined);
    expect(bob.get("p1")).toBeUndefined();
  });

  test("shares the changes made by the participants that joined the session", () => {
    const [aliceDoc, bobDoc] = createSession();
    const alice = joinSession(aliceDoc, true, [
      ["p1", REFERENCE, "Some.Page"],
      ["p1", OUTPUT, ["output"]],
    ]);
    const bob = joinSession(bobDoc, false);

    bob.set("p1", FREESTANDING, true);
    bob.set("p1", REFERENCE, undefined);

    expect(alice.get("p1")).toEqual({
      xwikiFreestanding: true,
      macroOutput: ["output"],
    });
  });

  test("shares the metadata of a session joined before it was initialized", () => {
    const aliceDoc = new Doc();
    const alice = createBlockMetadataStore();
    alice.set("p1", REFERENCE, "Some.Page");
    alice.connect(getSharedMetadata(aliceDoc), { initialize: true });

    const bobDoc = new Doc();
    applyUpdate(bobDoc, encodeStateAsUpdate(aliceDoc));
    const bob = createBlockMetadataStore();
    bob.connect(getSharedMetadata(bobDoc), { initialize: false });

    expect(bob.get("p1")).toEqual({ xwikiReference: "Some.Page" });
  });

  test("connecting again to the same shared metadata does nothing", () => {
    const doc = new Doc();
    const sharedMetadata = getSharedMetadata(doc);
    const metadata = createBlockMetadataStore();
    metadata.set("p1", REFERENCE, "Some.Page");
    metadata.connect(sharedMetadata, { initialize: true });

    metadata.connect(sharedMetadata, { initialize: false });

    expect(metadata.get("p1")).toEqual({ xwikiReference: "Some.Page" });
  });

  test("keeps the shared entries after disconnecting", () => {
    const [aliceDoc, bobDoc] = createSession();
    const alice = createBlockMetadataStore();
    alice.set("p1", REFERENCE, "Some.Page");
    const disconnect = alice.connect(getSharedMetadata(aliceDoc), {
      initialize: true,
    });
    const bob = createBlockMetadataStore();
    bob.connect(getSharedMetadata(bobDoc), { initialize: false });

    disconnect();
    alice.set("p1", FREESTANDING, true);

    expect(alice.get("p1")).toEqual({
      xwikiReference: "Some.Page",
      xwikiFreestanding: true,
    });
    expect(bob.get("p1")).toEqual({ xwikiReference: "Some.Page" });
  });
});
