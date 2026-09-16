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

import { YjsAwarenessSaveTransport } from "../yjsAwarenessSaveTransport";
import { SaveTarget, Saver } from "@xwiki/platform-autosave-api";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import {
  Awareness,
  applyAwarenessUpdate,
  encodeAwarenessUpdate,
  removeAwarenessStates,
} from "y-protocols/awareness";
import { Doc } from "yjs";
import type { Collaboration } from "../collaboration";
import type { SaveResult } from "@xwiki/platform-autosave-api";

/**
 * The bits of a Yjs provider the transport uses, backed by a plain object so that the tests don't need a server.
 */
class FakeProvider {
  public readonly awareness: Awareness;

  public wsconnected: boolean = true;

  public ws: {
    readyState: number;
    OPEN: number;
    bufferedAmount: number;
  } | null = { readyState: 1, OPEN: 1, bufferedAmount: 0 };

  private readonly statusHandlers: ((payload: { status: string }) => void)[] =
    [];

  public constructor(public readonly doc: Doc) {
    this.awareness = new Awareness(doc);
  }

  public on(
    event: string,
    handler: (payload: { status: string }) => void,
  ): void {
    if (event === "status") {
      this.statusHandlers.push(handler);
    }
  }

  public off(
    event: string,
    handler: (payload: { status: string }) => void,
  ): void {
    if (event === "status") {
      const index = this.statusHandlers.indexOf(handler);
      if (index >= 0) {
        this.statusHandlers.splice(index, 1);
      }
    }
  }

  public emitStatus(status: string): void {
    [...this.statusHandlers].forEach((handler) => handler({ status }));
  }
}

/**
 * A client taking part in the simulated collaboration session.
 */
class FakeClient {
  public readonly doc: Doc = new Doc();

  public readonly provider: FakeProvider = new FakeProvider(this.doc);

  public get collaboration(): Collaboration {
    return {
      doc: this.doc,
      provider: this.provider,
    } as unknown as Collaboration;
  }

  /**
   * Replicate the awareness state of this client to the given ones, the way the server broadcasts it.
   */
  public broadcastAwarenessTo(...others: FakeClient[]): void {
    const update = encodeAwarenessUpdate(this.provider.awareness, [
      this.provider.awareness.clientID,
    ]);
    others.forEach((other) => {
      applyAwarenessUpdate(other.provider.awareness, update, "remote");
    });
  }
}

/**
 * A saver that only records what the transport tells it, so that the tests can assert on the notifications.
 */
class FakeSaver {
  public readonly remoteStatesChanges: number[] = [];

  public onRemoteStatesChanged(): void {
    this.remoteStatesChanges.push(this.remoteStatesChanges.length + 1);
  }
}

let client: FakeClient;
let saver: FakeSaver;
let transport: YjsAwarenessSaveTransport;

function createTransport(
  target: FakeClient = client,
  // Only the transport of the client under test reports to the saver the assertions look at.
  owner: FakeSaver = target === client ? saver : new FakeSaver(),
): YjsAwarenessSaveTransport {
  const created = new YjsAwarenessSaveTransport(
    owner as unknown as Saver<object>,
    { collaboration: target.collaboration },
  );
  created.initialize();
  return created;
}

function getPublishedState(target: FakeClient = client): unknown {
  return target.provider.awareness.getLocalState()?.saver;
}

describe("YjsAwarenessSaveTransport", () => {
  beforeEach(() => {
    client = new FakeClient();
    saver = new FakeSaver();
    transport = createTransport();
  });

  afterEach(async () => {
    await transport.dispose();
    vi.useRealTimers();
  });

  describe("client identity", () => {
    it("identifies the local client by its awareness client id", () => {
      expect(transport.getClientId()).toBe(
        `${client.provider.awareness.clientID}`,
      );
    });
  });

  describe("publishing the local state", () => {
    it("keeps the state local until it is pushed", async () => {
      await transport.updateLocalState({ updateCount: 3 });

      expect(getPublishedState()).toBeUndefined();
      expect(transport.getLocalState().updateCount).toBe(3);
      expect(transport.getStates()[transport.getClientId()].updateCount).toBe(
        3,
      );
    });

    it("publishes the state when asked to push it", async () => {
      await transport.updateLocalState(
        { updateCount: 3, dirty: true },
        {
          push: true,
        },
      );

      expect(getPublishedState()).toMatchObject({
        updateCount: 3,
        dirty: true,
      });
    });

    it("preserves the awareness fields published by the editor", async () => {
      client.provider.awareness.setLocalStateField("user", { name: "Alice" });

      await transport.updateLocalState({ dirty: true }, { push: true });

      expect(client.provider.awareness.getLocalState()?.user).toEqual({
        name: "Alice",
      });
      expect(getPublishedState()).toMatchObject({ dirty: true });
    });

    it("publishes a copy, so that later local changes are not seen before they are pushed", async () => {
      await transport.updateLocalState({ updateCount: 1 }, { push: true });

      await transport.updateLocalState({ updateCount: 2 });

      expect(getPublishedState()).toMatchObject({ updateCount: 1 });
    });

    it("keeps the state locally when the client is disconnected", async () => {
      client.provider.awareness.setLocalState(null);

      await transport.updateLocalState({ updateCount: 7 }, { push: true });

      expect(transport.getLocalState().updateCount).toBe(7);
      expect(client.provider.awareness.getLocalState()).toBeNull();
    });
  });

  describe("reading the states", () => {
    it("reports the saver state of the other clients", async () => {
      const other = new FakeClient();
      const otherTransport = createTransport(other);
      await otherTransport.updateLocalState({ dirty: true }, { push: true });
      other.broadcastAwarenessTo(client);

      const states = transport.getStates();

      expect(states[`${other.provider.awareness.clientID}`]).toMatchObject({
        dirty: true,
      });
      await otherTransport.dispose();
    });

    it("ignores the clients that only published editor fields", () => {
      const other = new FakeClient();
      other.provider.awareness.setLocalStateField("user", { name: "Bob" });
      other.broadcastAwarenessTo(client);

      expect(Object.keys(transport.getStates())).toEqual([
        transport.getClientId(),
      ]);
    });

    it("still reports the local state after the awareness entry was dropped", async () => {
      await transport.updateLocalState({ updateCount: 4 }, { push: true });

      // The XWiki provider drops the entry of a client that left the room.
      removeAwarenessStates(
        client.provider.awareness,
        [client.provider.awareness.clientID],
        "disconnected",
      );

      expect(transport.getStates()[transport.getClientId()].updateCount).toBe(
        4,
      );
    });
  });

  describe("notifying the saver", () => {
    it("reports a remote saver state change", async () => {
      const other = new FakeClient();
      const otherTransport = createTransport(other);

      await otherTransport.updateLocalState({ dirty: true }, { push: true });
      other.broadcastAwarenessTo(client);

      expect(saver.remoteStatesChanges).toHaveLength(1);
      await otherTransport.dispose();
    });

    it("ignores a remote cursor move", () => {
      const other = new FakeClient();
      other.provider.awareness.setLocalStateField("cursor", { anchor: 1 });
      other.broadcastAwarenessTo(client);

      expect(saver.remoteStatesChanges).toHaveLength(0);
    });

    it("ignores our own push", async () => {
      await transport.updateLocalState({ dirty: true }, { push: true });

      expect(saver.remoteStatesChanges).toHaveLength(0);
    });
  });

  describe("waiting for the local state to be sent", () => {
    it("resolves right away when nothing is buffered", async () => {
      await expect(transport.whenSettled()).resolves.toBeUndefined();
    });

    it("resolves when the send buffer drains", async () => {
      vi.useFakeTimers();
      client.provider.ws!.bufferedAmount = 10;
      const settled = transport.whenSettled();

      client.provider.ws!.bufferedAmount = 0;
      await vi.advanceTimersByTimeAsync(50);

      await expect(settled).resolves.toBeUndefined();
    });

    it("gives up when the send buffer never drains", async () => {
      vi.useFakeTimers();
      client.provider.ws!.bufferedAmount = 10;
      const settled = transport.whenSettled();

      await vi.advanceTimersByTimeAsync(3000);

      await expect(settled).resolves.toBeUndefined();
    });
  });

  describe("session lifecycle", () => {
    it("waits for the connection then publishes the initial state", async () => {
      client.provider.wsconnected = false;
      const ready = transport.toBeReady();

      expect(getPublishedState()).toBeUndefined();
      client.provider.wsconnected = true;
      client.provider.emitStatus("connected");
      await ready;

      expect(getPublishedState()).toBeDefined();
    });

    it("publishes the local state when the document becomes hidden", async () => {
      await transport.updateLocalState({ dirty: true });
      vi.spyOn(document, "visibilityState", "get").mockReturnValue("hidden");

      document.dispatchEvent(new Event("visibilitychange"));
      await Promise.resolve();

      expect(getPublishedState()).toMatchObject({ dirty: true });
    });

    it("stops listening to the session", async () => {
      await transport.dispose();

      const other = new FakeClient();
      const otherTransport = createTransport(other);
      await otherTransport.updateLocalState({ dirty: true }, { push: true });
      other.broadcastAwarenessTo(client);

      expect(saver.remoteStatesChanges).toHaveLength(0);
      await otherTransport.dispose();
    });
  });
});

/**
 * A target that records the saves it was asked to perform, used to check the save election over the transport.
 */
class RecordingTarget extends SaveTarget {
  public submitted: number = 0;

  public override async initialize(): Promise<void> {}

  public override async submit(): Promise<SaveResult> {
    this.submitted++;
    return { version: "1.2" };
  }

  public override onStatesChanged(): void {}

  public override dispose(): void {}
}

/**
 * Two clients sharing a simulated session, each with its own saver, target and transport.
 */
function createElectionFixture(): {
  clients: FakeClient[];
  savers: Saver[];
  targets: RecordingTarget[];
  transports: YjsAwarenessSaveTransport[];
} {
  const clients = [new FakeClient(), new FakeClient()];
  const targets: RecordingTarget[] = [];
  const transports: YjsAwarenessSaveTransport[] = [];
  const savers = clients.map(
    (owner) =>
      new Saver(
        {},
        (saver) => {
          const created = new YjsAwarenessSaveTransport(saver, {
            collaboration: owner.collaboration,
          });
          transports.push(created);
          return created;
        },
        (saver) => {
          const target = new RecordingTarget(saver);
          targets.push(target);
          return target;
        },
      ),
  );
  return { clients, savers, targets, transports };
}

/**
 * Replicate the awareness state of each client to the other, the way the server broadcasts it.
 */
function exchangeAwareness(alice: FakeClient, bob: FakeClient): void {
  alice.broadcastAwarenessTo(bob);
  bob.broadcastAwarenessTo(alice);
}

describe("Saver over YjsAwarenessSaveTransport", () => {
  it("elects a single client to save when two of them have changes", async () => {
    const { clients, savers, targets, transports } = createElectionFixture();
    const [alice, bob] = clients;
    await Promise.all(savers.map((saver) => saver.toBeReady()));

    // Both clients make a change and tell each other about it.
    savers.forEach((saver) => saver.contentModifiedLocally());
    exchangeAwareness(alice, bob);

    // Both ask to save at the same time, then exchange their saving flags.
    const saves = savers.map((saver) => saver.save());
    exchangeAwareness(alice, bob);
    await Promise.all(saves);

    expect(targets.reduce((total, target) => total + target.submitted, 0)).toBe(
      1,
    );
    await Promise.all(transports.map((transport) => transport.dispose()));
  });
});
