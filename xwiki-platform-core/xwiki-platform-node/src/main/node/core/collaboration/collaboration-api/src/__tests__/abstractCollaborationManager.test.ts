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

import { AbstractCollaborationManager } from "../abstractCollaborationManager";
import { ConnectionStatus } from "../connectionStatus";
import { afterEach, describe, expect, it, vi } from "vitest";
import { ref } from "vue";
import type { Collaboration } from "../collaboration";
import type { Collaborator } from "../collaborator";
import type { AuthenticationManagerProvider } from "@xwiki/platform-authentication-api";
import type { DocumentService } from "@xwiki/platform-document-api";
import type { DocumentReference } from "@xwiki/platform-model-api";
import type { ModelReferenceSerializer } from "@xwiki/platform-model-reference-api";
import type { Doc } from "yjs";

/**
 * A manager whose connection is a plain object, so that the tests can count the connections, make them fail and see
 * which sessions are disconnected.
 */
class TestCollaborationManager extends AbstractCollaborationManager {
  public connections: number = 0;

  public readonly disconnected: Collaboration[] = [];

  public failNextConnection: boolean = false;

  public readonly saverStop = vi.fn();

  constructor(currentDocumentReference?: DocumentReference) {
    super(
      {
        get: () => ({
          getUserDetails: async () => ({ name: "Alice", username: "alice" }),
        }),
      } as unknown as AuthenticationManagerProvider,
      {
        getCurrentDocumentReference: () => ref(currentDocumentReference),
      } as unknown as DocumentService,
      {
        serialize: (reference: DocumentReference) => reference.name,
      } as unknown as ModelReferenceSerializer,
    );
  }

  protected override async createProvider(): Promise<unknown> {
    this.connections++;
    if (this.failNextConnection) {
      throw new Error("Failed to connect.");
    }
    return { name: `provider ${this.connections}` };
  }

  protected override async createCollaboration(
    provider: unknown,
    collaborator: Collaborator,
  ): Promise<Collaboration> {
    return {
      connectionStatus: ref(ConnectionStatus.Connected),
      collaborators: ref([]),
      collaborator,
      provider,
      doc: {} as Doc,
      saver: { stop: this.saverStop } as unknown as Collaboration["saver"],
    };
  }

  protected override disconnect(collaboration: Collaboration): void {
    this.disconnected.push(collaboration);
  }
}

function documentReference(name: string): DocumentReference {
  return { name } as unknown as DocumentReference;
}

/**
 * leave() disconnects asynchronously, without returning the promise it starts.
 */
function flush(): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, 0));
}

describe("AbstractCollaborationManager", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("shares one connection between the editors of the same document", async () => {
    const manager = new TestCollaborationManager();

    const first = await manager.join(documentReference("Main.WebHome"));
    const second = await manager.join(documentReference("Main.WebHome"));

    // Equal references, but two distinct objects: the session is found by the serialized reference, not by identity.
    expect(second).toBe(first);
    expect(manager.connections).toBe(1);
  });

  it("keeps the session while another editor is still in it", async () => {
    const manager = new TestCollaborationManager();
    const reference = documentReference("Main.WebHome");
    await manager.join(reference);
    await manager.join(reference);

    manager.leave(reference);
    await flush();
    expect(manager.disconnected).toEqual([]);
  });

  it("disconnects when the last editor leaves the session", async () => {
    const manager = new TestCollaborationManager();
    const reference = documentReference("Main.WebHome");
    const collaboration = await manager.join(reference);

    manager.leave(reference);
    await flush();
    expect(manager.disconnected).toEqual([collaboration]);
    // Stopped before disconnecting, so that it can tell the other collaborators what it leaves unsaved.
    expect(manager.saverStop).toHaveBeenCalled();
  });

  it("starts a new connection when the previous one failed", async () => {
    const manager = new TestCollaborationManager();
    const reference = documentReference("Main.WebHome");

    manager.failNextConnection = true;
    await expect(manager.join(reference)).rejects.toThrow("Failed to connect.");

    manager.failNextConnection = false;
    await expect(manager.join(reference)).resolves.toBeDefined();
    expect(manager.connections).toBe(2);
  });

  it("forgets a session that failed to connect", async () => {
    const manager = new TestCollaborationManager();
    const reference = documentReference("Main.WebHome");
    const warn = vi.spyOn(console, "warn").mockImplementation(() => {});

    manager.failNextConnection = true;
    await expect(manager.join(reference)).rejects.toThrow();

    // Leaving reports a session the user is not connected to, rather than awaiting the failed connection.
    manager.leave(reference);
    await flush();
    expect(warn).toHaveBeenCalledWith(
      expect.stringContaining("not connected to"),
      reference,
    );
    expect(manager.disconnected).toEqual([]);
  });

  it("falls back to the current document when no reference is given", async () => {
    const manager = new TestCollaborationManager(
      documentReference("Main.WebHome"),
    );
    const collaboration = await manager.join();

    manager.leave();
    await flush();
    expect(manager.disconnected).toEqual([collaboration]);
  });
});
