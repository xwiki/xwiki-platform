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

import { XWikiCollaborationManager } from "../XWikiCollaborationManager";
import { describe, expect, it, vi } from "vitest";
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

  public readonly destroy = vi.fn();

  private readonly listeners: Map<string, Set<(payload?: unknown) => void>> =
    new Map();

  public on(event: string, listener: (payload?: unknown) => void): void {
    if (!this.listeners.has(event)) {
      this.listeners.set(event, new Set());
    }
    this.listeners.get(event)!.add(listener);
  }

  /**
   * Registers a self-removing wrapper, exactly like the Observable the real provider extends. This is what off()
   * can't remove, and the reason the production code registers its listeners with on().
   */
  public once(event: string, listener: (payload?: unknown) => void): void {
    const wrapper = (payload?: unknown) => {
      this.off(event, wrapper);
      listener(payload);
    };
    this.on(event, wrapper);
  }

  public off(event: string, listener: (payload?: unknown) => void): void {
    this.listeners.get(event)?.delete(listener);
  }

  public emit(event: string, payload?: unknown): void {
    this.listeners.get(event)?.forEach((listener) => listener(payload));
  }

  public listenerCount(event: string): number {
    return this.listeners.get(event)?.size ?? 0;
  }
}

/**
 * Exposes the protected method under test, and spares us the dependencies the manager doesn't use to create a
 * collaboration session.
 */
class TestableCollaborationManager extends XWikiCollaborationManager {
  constructor() {
    super(undefined!, undefined!, undefined!, undefined!);
  }

  public createCollaborationForTest(
    provider: FakeProvider,
    collaborator: Collaborator,
  ): Promise<Collaboration> {
    return this.createCollaboration(
      provider as unknown as WebsocketProvider,
      collaborator,
    );
  }
}

function newCollaborator(): Collaborator {
  return { user: { name: "Alice" }, color: "#ff0000" };
}

describe("XWikiCollaborationManager", () => {
  it("resolves once the session is synchronized", async () => {
    const provider = new FakeProvider();
    const manager = new TestableCollaborationManager();

    const promise = manager.createCollaborationForTest(
      provider,
      newCollaborator(),
    );
    provider.emit("sync");

    const collaboration = await promise;
    expect(collaboration.provider).toBe(provider);
    expect(collaboration.collaborator.id).toBe("42");
    expect(provider.destroy).not.toHaveBeenCalled();
  });

  it("rejects and destroys the provider when the first connection attempt fails", async () => {
    const provider = new FakeProvider();
    const manager = new TestableCollaborationManager();

    const promise = manager.createCollaborationForTest(
      provider,
      newCollaborator(),
    );
    provider.emit("connection-close");

    await expect(promise).rejects.toThrow(
      "Failed to connect to the collaboration server.",
    );
    expect(provider.destroy).toHaveBeenCalled();
  });

  it("keeps the session when the connection drops after it was synchronized", async () => {
    const provider = new FakeProvider();
    const manager = new TestableCollaborationManager();

    const promise = manager.createCollaborationForTest(
      provider,
      newCollaborator(),
    );
    provider.emit("sync");
    await promise;

    // A disconnection that follows a successful synchronization is transient: the provider recovers from it on its
    // own, so it must neither tear down the session nor leave a listener behind.
    provider.emit("connection-close");
    expect(provider.destroy).not.toHaveBeenCalled();
    expect(provider.listenerCount("sync")).toBe(0);
    expect(provider.listenerCount("connection-close")).toBe(0);
  });

  it("rejects and destroys the provider when the synchronization doesn't happen in time", async () => {
    vi.useFakeTimers();
    try {
      const provider = new FakeProvider();
      const manager = new TestableCollaborationManager();

      const promise = manager.createCollaborationForTest(
        provider,
        newCollaborator(),
      );
      // Asserted before the timers run, so that the rejection is never momentarily unhandled.
      const rejection = expect(promise).rejects.toThrow(
        "Timed out waiting for the collaboration server to sync.",
      );
      // The server accepted the connection but never sent the initial state.
      await vi.advanceTimersByTimeAsync(15000);

      await rejection;
      expect(provider.destroy).toHaveBeenCalled();
    } finally {
      vi.useRealTimers();
    }
  });

  it("stops waiting for the synchronization once the session is synchronized", async () => {
    vi.useFakeTimers();
    try {
      const provider = new FakeProvider();
      const manager = new TestableCollaborationManager();

      const promise = manager.createCollaborationForTest(
        provider,
        newCollaborator(),
      );
      provider.emit("sync");
      await promise;

      expect(vi.getTimerCount()).toBe(0);
    } finally {
      vi.useRealTimers();
    }
  });
});
