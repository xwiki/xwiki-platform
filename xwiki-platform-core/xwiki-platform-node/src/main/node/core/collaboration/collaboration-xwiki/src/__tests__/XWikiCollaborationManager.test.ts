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

// Test scenarios are naturally made of many statements.
/* eslint-disable max-statements */

import { XWikiCollaborationManager } from "../XWikiCollaborationManager";
import { ConnectionStatus } from "@xwiki/platform-collaboration-api";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import type {
  Collaboration,
  Collaborator,
} from "@xwiki/platform-collaboration-api";
import type { WebsocketProvider } from "y-websocket";

/**
 * A stand-in for the y-websocket provider, so that the tests can emit the events a real connection would emit.
 */
class FakeProvider {
  public readonly doc = { clientID: 42 };

  public readonly awareness = {
    on: vi.fn(),
    getStates: () => new Map(),
  };

  public wsconnected = false;

  public readonly destroy = vi.fn();

  private readonly listeners: Map<string, Set<(...args: unknown[]) => void>> =
    new Map();

  public on(event: string, listener: (...args: unknown[]) => void): void {
    if (!this.listeners.has(event)) {
      this.listeners.set(event, new Set());
    }
    this.listeners.get(event)!.add(listener);
  }

  /**
   * Registers a self-removing wrapper, like the Observable the real provider extends, which off() can't remove.
   */
  public once(event: string, listener: (...args: unknown[]) => void): void {
    const wrapper = (...args: unknown[]) => {
      this.off(event, wrapper);
      listener(...args);
    };
    this.on(event, wrapper);
  }

  public off(event: string, listener: (...args: unknown[]) => void): void {
    this.listeners.get(event)?.delete(listener);
  }

  public emit(event: string, ...args: unknown[]): void {
    this.listeners.get(event)?.forEach((listener) => listener(...args));
  }

  public listenerCount(event: string): number {
    return this.listeners.get(event)?.size ?? 0;
  }

  public connect(): void {
    this.wsconnected = true;
    this.emit("status", { status: "connected" });
  }

  public drop(): void {
    this.wsconnected = false;
    this.emit("status", { status: "disconnected" });
  }
}

/**
 * Exposes the protected method under test, and spares us the dependencies the manager doesn't use to create a
 * collaboration session.
 */
class TestCollaborationManager extends XWikiCollaborationManager {
  constructor() {
    super(undefined!, undefined!, undefined!, undefined!);
  }

  public createCollaborationForTest(
    provider: FakeProvider,
  ): Promise<Collaboration> {
    const collaborator: Collaborator = {
      user: { name: "Alice" },
      color: "#ff0000",
    };
    return this.createCollaboration(
      provider as unknown as WebsocketProvider,
      collaborator,
    );
  }
}

describe("XWikiCollaborationManager", () => {
  let provider: FakeProvider;
  let warn: ReturnType<typeof vi.spyOn>;

  beforeEach(() => {
    vi.useFakeTimers();
    vi.spyOn(console, "debug").mockImplementation(() => {});
    warn = vi.spyOn(console, "warn").mockImplementation(() => {});
    provider = new FakeProvider();
  });

  afterEach(() => {
    vi.useRealTimers();
    vi.restoreAllMocks();
  });

  it("joins the session once it is synchronized", async () => {
    const joined = new TestCollaborationManager().createCollaborationForTest(
      provider,
    );
    provider.connect();
    provider.emit("sync", true);

    const collaboration = await joined;
    expect(collaboration.provider).toBe(provider);
    expect(collaboration.collaborator.id).toBe("42");
    expect(collaboration.connectionStatus.value).toBe(
      ConnectionStatus.Connected,
    );
    // Stop waiting for the synchronization.
    expect(vi.getTimerCount()).toBe(0);
    expect(provider.listenerCount("sync")).toBe(0);
    expect(warn).not.toHaveBeenCalled();
  });

  it("joins the session disconnected when the connection can't be established", async () => {
    const joined = new TestCollaborationManager().createCollaborationForTest(
      provider,
    );
    // The provider keeps trying to connect.
    provider.emit("status", { status: "connecting" });
    provider.drop();
    provider.emit("status", { status: "connecting" });

    await vi.advanceTimersByTimeAsync(5000);

    const collaboration = await joined;
    expect(collaboration.connectionStatus.value).toBe(
      ConnectionStatus.Connecting,
    );
    // The provider is left trying to connect, and the session is synchronized when it does.
    expect(provider.destroy).not.toHaveBeenCalled();
    expect(vi.getTimerCount()).toBe(0);
    expect(provider.listenerCount("sync")).toBe(0);
    expect(warn).toHaveBeenCalledOnce();

    provider.connect();
    expect(collaboration.connectionStatus.value).toBe(
      ConnectionStatus.Connected,
    );
  });

  it("waits for the synchronization while the connection is established", async () => {
    const joined = new TestCollaborationManager().createCollaborationForTest(
      provider,
    );
    provider.connect();

    // The server is slow to send its history.
    await vi.advanceTimersByTimeAsync(12000);
    await expect(Promise.race([joined, "pending"])).resolves.toBe("pending");

    provider.emit("sync", true);
    await expect(joined).resolves.toBeDefined();
    expect(vi.getTimerCount()).toBe(0);
    expect(warn).not.toHaveBeenCalled();
  });

  it("gives up waiting when the connection is lost before the synchronization", async () => {
    const joined = new TestCollaborationManager().createCollaborationForTest(
      provider,
    );
    provider.connect();
    await vi.advanceTimersByTimeAsync(5000);
    // The provider emits a sync event when the connection is lost, but we're not synchronized.
    provider.drop();
    provider.emit("sync", false);

    await vi.advanceTimersByTimeAsync(5000);

    const collaboration = await joined;
    expect(collaboration.connectionStatus.value).toBe(
      ConnectionStatus.Disconnected,
    );
    expect(warn).toHaveBeenCalledOnce();
  });
});
