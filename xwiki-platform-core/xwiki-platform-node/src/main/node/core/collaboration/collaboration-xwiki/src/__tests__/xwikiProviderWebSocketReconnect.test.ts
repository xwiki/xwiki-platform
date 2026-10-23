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

// Test suites and test scenarios are naturally made of many statements.
/* eslint-disable max-statements */

import { SESSION_REPLACED_EVENT } from "../events";
import { createXWikiWebSocketProvider } from "../xwikiProviderWebSocket";
import * as decoding from "lib0/decoding";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import type { WebsocketProvider } from "y-websocket";

/**
 * Mimics the XWiki Yjs WebSocket endpoint (see AbstractRoom): it stores the synchronization updates, replays them
 * followed by an empty sync step 2 when a client sends a sync step 1, and relays the other messages.
 */
const { FakeRoom, FakeWebSocket } = vi.hoisted(() => {
  /**
   * The messages in transit, delivered when the test flushes the network.
   */
  const queue: (() => void)[] = [];

  class FakeRoom {
    static current: FakeRoom;

    history: Uint8Array[] = [];

    /**
     * When true, the connection attempts fail, as if the server couldn't be reached.
     */
    unreachable = false;

    sockets = new Set<FakeWebSocket>();

    /**
     * The synchronization updates received from each socket.
     */
    receivedUpdates = new Map<FakeWebSocket, Uint8Array[]>();

    static flush() {
      while (queue.length) {
        queue.shift()!();
      }
    }

    receive(from: FakeWebSocket, data: Uint8Array) {
      const decoder = decoding.createDecoder(data);
      const messageType = decoding.readVarUint(decoder);
      if (messageType === 0) {
        const syncMessageType = decoding.readVarUint(decoder);
        if (syncMessageType === 0) {
          this.history.forEach((update) => from.deliver(update));
          from.deliver(new Uint8Array([0, 1, 2, 0, 0]));
          return;
        } else if (syncMessageType === 2) {
          this.history.push(data);
          this.receivedUpdates.set(from, [
            ...(this.receivedUpdates.get(from) ?? []),
            data,
          ]);
        }
      } else if (messageType !== 1) {
        // Ignore the join and awareness query messages.
        return;
      }
      this.sockets.forEach((socket) => {
        // Awareness updates are sent back to the sender too.
        if (socket !== from || messageType === 1) {
          socket.deliver(data);
        }
      });
    }

    /**
     * Discards the room history, as if all the clients left the room.
     */
    reset() {
      this.history = [];
    }
  }

  class FakeWebSocket {
    static readonly OPEN = 1;
    readonly OPEN = 1;
    readyState = 0;
    binaryType = "arraybuffer";
    onopen: (() => void) | null = null;
    onmessage: ((event: { data: ArrayBuffer }) => void) | null = null;
    onclose: ((event: { code: number }) => void) | null = null;
    onerror: (() => void) | null = null;

    /**
     * When true, the sent messages are lost, as if the connection was lost without being detected yet.
     */
    deaf = false;

    constructor() {
      queue.push(() => {
        if (FakeRoom.current.unreachable) {
          this.readyState = 3;
          this.onclose?.({ code: 1006 });
          return;
        }
        this.readyState = this.OPEN;
        FakeRoom.current.sockets.add(this);
        this.onopen?.();
      });
    }

    send(data: Uint8Array) {
      if (!this.deaf) {
        queue.push(() => FakeRoom.current.receive(this, data));
      }
    }

    deliver(data: Uint8Array) {
      queue.push(() => this.onmessage?.({ data: data.slice().buffer }));
    }

    close() {
      this.readyState = 3;
      FakeRoom.current.sockets.delete(this);
    }

    /**
     * Simulates a connection loss.
     */
    drop() {
      this.close();
      this.onclose?.({ code: 1006 });
    }
  }

  return { FakeRoom, FakeWebSocket };
});

vi.mock("y-websocket", async (importOriginal) => {
  const actual = await importOriginal<typeof import("y-websocket")>();
  return {
    ...actual,
    WebsocketProvider: class extends actual.WebsocketProvider {
      constructor(
        url: string,
        room: string,
        doc: ConstructorParameters<typeof actual.WebsocketProvider>[2],
        options: ConstructorParameters<typeof actual.WebsocketProvider>[3],
      ) {
        super(url, room, doc, {
          ...options,
          WebSocketPolyfill: FakeWebSocket as never,
          disableBc: true,
        });
      }
    },
  };
});

describe("createXWikiWebSocketProvider (reconnect)", () => {
  const providers: WebsocketProvider[] = [];

  function connect(): WebsocketProvider {
    const provider = createXWikiWebSocketProvider(
      "ws://localhost/xwiki/websocket/xwiki/yjs",
      "xwiki:Space.Page",
      "",
    );
    providers.push(provider);
    FakeRoom.flush();
    return provider;
  }

  function socket(provider: WebsocketProvider) {
    return provider.ws as unknown as InstanceType<typeof FakeWebSocket>;
  }

  function disconnect(provider: WebsocketProvider) {
    socket(provider).drop();
    FakeRoom.flush();
  }

  function reconnect(provider: WebsocketProvider) {
    // Wait for the provider to reconnect (exponential back-off).
    vi.advanceTimersByTime(3000);
    FakeRoom.flush();
    expect(provider.wsconnected).toBe(true);
  }

  function text(provider: WebsocketProvider) {
    return provider.doc.getText("content");
  }

  beforeEach(() => {
    vi.useFakeTimers();
    FakeRoom.current = new FakeRoom();
  });

  afterEach(() => {
    providers.splice(0).forEach((provider) => provider.destroy());
    vi.useRealTimers();
  });

  it("sends the changes made while disconnected", () => {
    const alice = connect();
    text(alice).insert(0, "Hello");
    FakeRoom.flush();
    const bob = connect();
    expect(text(bob).toString()).toBe("Hello");

    disconnect(alice);
    text(alice).insert(5, " world");
    FakeRoom.flush();
    expect(text(bob).toString()).toBe("Hello");

    reconnect(alice);
    expect(text(bob).toString()).toBe("Hello world");
    // The changes are also stored by the server, for the clients that join later.
    expect(text(connect()).toString()).toBe("Hello world");
  });

  it("sends the deletions made while disconnected", () => {
    const alice = connect();
    text(alice).insert(0, "Hello world");
    FakeRoom.flush();
    const bob = connect();

    disconnect(alice);
    text(alice).delete(5, 6);

    reconnect(alice);
    expect(text(bob).toString()).toBe("Hello");
  });

  it("sends again the changes lost before the connection loss was detected", () => {
    const alice = connect();
    text(alice).insert(0, "Hello");
    FakeRoom.flush();
    const bob = connect();

    socket(alice).deaf = true;
    text(alice).insert(5, " world");
    FakeRoom.flush();
    expect(text(bob).toString()).toBe("Hello");
    disconnect(alice);

    reconnect(alice);
    expect(text(bob).toString()).toBe("Hello world");
  });

  it("doesn't send anything when there are no local changes", () => {
    const alice = connect();
    const bob = connect();
    text(bob).insert(0, "Hello world");
    FakeRoom.flush();
    expect(text(alice).toString()).toBe("Hello world");

    disconnect(alice);
    reconnect(alice);

    expect(FakeRoom.current.receivedUpdates.get(socket(alice))).toBeUndefined();
    expect(text(alice).toString()).toBe("Hello world");
  });

  it("resumes the session when the room was discarded while disconnected", () => {
    const alice = connect();
    text(alice).insert(0, "Hello");
    FakeRoom.flush();

    disconnect(alice);
    FakeRoom.current.reset();
    text(alice).insert(5, " world");

    reconnect(alice);
    expect(text(connect()).toString()).toBe("Hello world");
  });

  it("stops synchronizing when the room was replaced by a new session while disconnected", () => {
    const alice = connect();
    text(alice).insert(0, "Hello");
    FakeRoom.flush();

    disconnect(alice);
    FakeRoom.current.reset();
    const carol = connect();
    text(carol).insert(0, "Hello");
    FakeRoom.flush();
    text(alice).insert(5, " world");
    const onSessionReplaced = vi.fn();
    // @ts-expect-error The provider doesn't declare our custom event.
    alice.on(SESSION_REPLACED_EVENT, onSessionReplaced);
    const warn = vi.spyOn(console, "warn").mockImplementation(() => {});

    vi.advanceTimersByTime(3000);
    FakeRoom.flush();

    expect(onSessionReplaced).toHaveBeenCalledOnce();
    expect(warn).toHaveBeenCalledOnce();
    expect(alice.wsconnected).toBe(false);
    expect(alice.shouldConnect).toBe(false);
    // Neither the local document nor the new session are affected.
    expect(text(alice).toString()).toBe("Hello world");
    expect(text(carol).toString()).toBe("Hello");
    expect(text(connect()).toString()).toBe("Hello");
  });

  it("starts the session with the local content when the server becomes reachable", () => {
    FakeRoom.current.unreachable = true;
    const alice = connect();
    expect(alice.wsconnected).toBe(false);
    // The content is initialized locally, because we gave up waiting for the server.
    text(alice).insert(0, "Hello");

    FakeRoom.current.unreachable = false;
    reconnect(alice);

    expect(text(connect()).toString()).toBe("Hello");
  });

  it("joins the session started while the server was unreachable, if we didn't initialize the content yet", () => {
    FakeRoom.current.unreachable = true;
    const alice = connect();
    expect(alice.wsconnected).toBe(false);

    FakeRoom.current.unreachable = false;
    const bob = connect();
    text(bob).insert(0, "Hello");
    FakeRoom.flush();

    reconnect(alice);
    expect(text(alice).toString()).toBe("Hello");
  });

  it("stops synchronizing when the session was started by someone else while the server was unreachable", () => {
    FakeRoom.current.unreachable = true;
    const alice = connect();
    text(alice).insert(0, "Hello");

    FakeRoom.current.unreachable = false;
    const bob = connect();
    text(bob).insert(0, "Hi");
    FakeRoom.flush();
    const onSessionReplaced = vi.fn();
    // @ts-expect-error The provider doesn't declare our custom event.
    alice.on(SESSION_REPLACED_EVENT, onSessionReplaced);
    vi.spyOn(console, "warn").mockImplementation(() => {});

    vi.advanceTimersByTime(3000);
    FakeRoom.flush();

    expect(onSessionReplaced).toHaveBeenCalledOnce();
    expect(alice.shouldConnect).toBe(false);
    // Neither the local document nor the session are affected.
    expect(text(alice).toString()).toBe("Hello");
    expect(text(bob).toString()).toBe("Hi");
  });
});
