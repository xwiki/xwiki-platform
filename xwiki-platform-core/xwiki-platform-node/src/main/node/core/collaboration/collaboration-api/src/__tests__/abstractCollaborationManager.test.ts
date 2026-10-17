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
import { describe, expect, it } from "vitest";
import { ref } from "vue";
import { Awareness } from "y-protocols/awareness";
import { Doc } from "yjs";
import type { Collaboration } from "../collaboration";
import type { Collaborator } from "../collaborator";
import type { AuthenticationManagerProvider } from "@xwiki/platform-authentication-api";
import type { DocumentService } from "@xwiki/platform-document-api";
import type { DocumentReference } from "@xwiki/platform-model-api";
import type { ModelReferenceSerializer } from "@xwiki/platform-model-reference-api";

const ALICE = {
  name: "Alice",
  username: "alice",
  profile: "/profile/alice",
  avatar: "/avatar/alice.png",
};

type FakeProvider = { doc: Doc; awareness: Awareness };

/**
 * A collaboration manager that connects to nothing, and lists the collaborators the way the actual collaboration
 * managers do, from the awareness states.
 */
class TestCollaborationManager extends AbstractCollaborationManager {
  constructor() {
    super(
      {
        get: () => ({ getUserDetails: async () => ALICE }),
      } as unknown as AuthenticationManagerProvider,
      {} as DocumentService,
      {
        serialize: () => "doc:Space.Page",
      } as unknown as ModelReferenceSerializer,
    );
  }

  protected override async createProvider(): Promise<FakeProvider> {
    const doc = new Doc();
    return { doc, awareness: new Awareness(doc) };
  }

  protected override async createCollaboration(
    provider: FakeProvider,
    collaborator: Collaborator,
  ): Promise<Collaboration> {
    const collaboration: Collaboration = {
      connectionStatus: ref(ConnectionStatus.Connected),
      collaborators: ref([]),
      collaborator,
      provider,
      doc: provider.doc,
    };
    provider.awareness.on("change", () => {
      collaboration.collaborators.value = [
        ...provider.awareness.getStates().entries(),
      ]
        .filter(([, state]) => state.collaborator)
        .map(
          ([
            clientId,
            {
              collaborator: { user, color },
            },
          ]) => ({
            id: `${clientId}`,
            user,
            color,
          }),
        );
    });
    return collaboration;
  }

  protected override disconnect(): void {}
}

describe("AbstractCollaborationManager", () => {
  it("publishes the current user to the other collaborators", async () => {
    const collaboration = await new TestCollaborationManager().join(
      {} as DocumentReference,
    );

    const awareness: Awareness = collaboration.provider.awareness;
    expect(awareness.getLocalState()?.collaborator).toEqual({
      user: ALICE,
      color: collaboration.collaborator.color,
    });
    expect(collaboration.collaborator.color).toMatch(/^#[0-9a-f]{6}$/);
    // The current user is listed among the collaborators.
    expect(collaboration.collaborators.value).toEqual([
      {
        id: `${awareness.clientID}`,
        user: ALICE,
        color: collaboration.collaborator.color,
      },
    ]);
  });
});
