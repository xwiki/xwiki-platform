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

import { createXWikiWebSocketProvider } from "../xwikiProviderWebSocket";
import * as decoding from "lib0/decoding";
import * as encoding from "lib0/encoding";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { Awareness } from "y-protocols/awareness";
import { WebsocketProvider } from "y-websocket";
import type { Doc } from "yjs";

vi.mock("y-websocket", () => ({
  WebsocketProvider: vi.fn(
    class {
      messageHandlers: unknown[] = [];
      awareness: Awareness;
      on = vi.fn();
      ws = { send: vi.fn() };
      originalDestroy = vi.fn();
      destroy = this.originalDestroy;
      constructor(
        _url: string,
        _room: string,
        public readonly doc: Doc,
      ) {
        this.awareness = new Awareness(doc);
      }
    },
  ),
}));

type ProviderMock = WebsocketProvider & {
  messageHandlers: ((
    encoder: encoding.Encoder,
    decoder: decoding.Decoder,
    provider: WebsocketProvider,
  ) => void)[];
};

describe("createXWikiWebSocketProvider", () => {
  beforeEach(() => {
    vi.mocked(WebsocketProvider).mockClear();
  });

  it("connects to the room of the given document translation", () => {
    createXWikiWebSocketProvider(
      "ws://localhost/xwiki/websocket/xwiki/yjs",
      "xwiki:Space.Page",
      "fr",
    );

    const [url, roomName, , options] =
      vi.mocked(WebsocketProvider).mock.calls[0];
    expect(url).toBe("ws://localhost/xwiki/websocket/xwiki");
    expect(roomName).toBe("yjs");
    expect(options).toEqual({
      params: { room: "xwiki:Space.Page", locale: "fr" },
    });
  });

  it("removes the local awareness state when the page is hidden, until destroyed", () => {
    const provider = createXWikiWebSocketProvider(
      "ws://localhost/xwiki/websocket/xwiki/yjs",
      "xwiki:Space.Page",
      "",
    ) as ProviderMock & { originalDestroy: ReturnType<typeof vi.fn> };
    const awareness = provider.awareness;
    const clientID = provider.doc.clientID;
    awareness.setLocalState({ collaborator: { user: { name: "Alice" } } });
    const onChange = vi.fn();
    awareness.on("change", onChange);

    window.dispatchEvent(new Event("pagehide"));

    expect(awareness.getStates().has(clientID)).toBe(false);
    expect(onChange).toHaveBeenCalledWith(
      { added: [], updated: [], removed: [clientID] },
      "page hide",
    );

    // Join again (e.g. the page is restored from the back-forward cache), then destroy the provider.
    awareness.setLocalState({ collaborator: { user: { name: "Alice" } } });
    provider.destroy();
    expect(provider.originalDestroy).toHaveBeenCalledOnce();

    window.dispatchEvent(new Event("pagehide"));
    expect(awareness.getStates().has(clientID)).toBe(true);
  });

  it("identifies itself and asks who is there each time it connects", () => {
    const provider = createXWikiWebSocketProvider(
      "ws://localhost/xwiki/websocket/xwiki/yjs",
      "xwiki:Space.Page",
      "",
    ) as ProviderMock & { ws: { send: ReturnType<typeof vi.fn> } };
    const [eventName, onStatus] = vi.mocked(provider.on).mock.calls[0] as [
      string,
      (event: { status: string }) => void,
    ];
    expect(eventName).toBe("status");

    onStatus({ status: "connecting" });
    expect(provider.ws.send).not.toHaveBeenCalled();

    const join = encoding.createEncoder();
    encoding.writeVarUint(join, 2);
    encoding.writeVarUint(join, provider.doc.clientID);
    const query = new Uint8Array([3]);
    onStatus({ status: "connected" });
    expect(provider.ws.send.mock.calls).toEqual([
      [encoding.toUint8Array(join)],
      [query],
    ]);

    // Reconnect.
    onStatus({ status: "disconnected" });
    onStatus({ status: "connected" });
    expect(provider.ws.send).toHaveBeenCalledTimes(4);
  });

  it("removes the collaborators that left the room", () => {
    const provider = createXWikiWebSocketProvider(
      "ws://localhost/xwiki/websocket/xwiki/yjs",
      "xwiki:Space.Page",
      "",
    ) as ProviderMock;
    const awareness = provider.awareness;
    // Simulate a remote collaborator.
    awareness.states.set(42, { collaborator: { user: { name: "Bob" } } });
    awareness.meta.set(42, { clock: 1, lastUpdated: 0 });

    // The server notifies that the collaborator left the room.
    const encoder = encoding.createEncoder();
    encoding.writeVarUint(encoder, 4);
    encoding.writeVarUint(encoder, 42);
    const message = encoding.toUint8Array(encoder);
    const decoder = decoding.createDecoder(message);
    decoding.readVarUint(decoder);
    provider.messageHandlers[4](encoding.createEncoder(), decoder, provider);

    expect(awareness.getStates().has(42)).toBe(false);
  });
});
