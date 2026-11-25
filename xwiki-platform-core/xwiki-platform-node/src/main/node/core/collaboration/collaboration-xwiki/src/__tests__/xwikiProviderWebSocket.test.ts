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

import { createXWikiWebSocketProvider } from "../xwikiProviderWebSocket";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import type { WebsocketProvider } from "y-websocket";

vi.mock("y-websocket", () => ({
  WebsocketProvider: vi.fn(
    class {
      messageHandlers: unknown[] = [];
      once = vi.fn();
      disconnect = vi.fn();
      destroy = vi.fn();
    },
  ),
}));

describe("createXWikiWebSocketProvider", () => {
  let provider: WebsocketProvider & {
    disconnect: ReturnType<typeof vi.fn>;
  };
  const reload = vi.fn();

  beforeEach(() => {
    vi.stubGlobal("location", { reload });
    provider = createXWikiWebSocketProvider(
      "ws://localhost/xwiki/websocket/xwiki/yjs",
      "xwiki:Space.Page",
      "",
    ) as typeof provider;
  });

  afterEach(() => {
    provider.destroy();
    vi.unstubAllGlobals();
    reload.mockClear();
  });

  it("leaves the session while the page is kept in the back/forward cache, and reloads the page when it is restored", () => {
    // The page is destroyed (not kept in the cache): the WebSocket connection is closed with it.
    window.dispatchEvent(
      new PageTransitionEvent("pagehide", { persisted: false }),
    );
    expect(provider.disconnect).not.toHaveBeenCalled();

    // The page is kept in the cache.
    window.dispatchEvent(
      new PageTransitionEvent("pagehide", { persisted: true }),
    );
    expect(provider.disconnect).toHaveBeenCalledOnce();

    // A page shown for the first time doesn't need to be reloaded.
    window.dispatchEvent(
      new PageTransitionEvent("pageshow", { persisted: false }),
    );
    expect(reload).not.toHaveBeenCalled();

    // The page is restored from the cache.
    window.dispatchEvent(
      new PageTransitionEvent("pageshow", { persisted: true }),
    );
    expect(reload).toHaveBeenCalledOnce();
  });

  it("stops listening to the page transitions when destroyed", () => {
    provider.destroy();

    window.dispatchEvent(
      new PageTransitionEvent("pagehide", { persisted: true }),
    );
    window.dispatchEvent(
      new PageTransitionEvent("pageshow", { persisted: true }),
    );

    expect(provider.disconnect).not.toHaveBeenCalled();
    expect(reload).not.toHaveBeenCalled();
  });
});
