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
import {
  AbstractCollaborationManager,
  ConnectionStatus,
} from "@xwiki/platform-collaboration-api";
import { name as documentServiceName } from "@xwiki/platform-document-api";
import { inject, injectable, named } from "inversify";
import { ref } from "vue";
import type { CristalApp } from "@xwiki/platform-api";
import type { AuthenticationManagerProvider } from "@xwiki/platform-authentication-api";
import type {
  Collaboration,
  Collaborator,
} from "@xwiki/platform-collaboration-api";
import type { DocumentService } from "@xwiki/platform-document-api";
import type { DocumentReference } from "@xwiki/platform-model-api";
import type { ModelReferenceSerializer } from "@xwiki/platform-model-reference-api";
import type { WebsocketProvider } from "y-websocket";

/**
 * How long to wait for the initial synchronization before giving up, in milliseconds. Joining a session blocks the
 * caller until then, so a server that accepts the connection but never synchronizes must not block it forever.
 */
const SYNC_TIMEOUT = 15000;

/**
 * A collaboration provider for the websocket endpoint provided by xwiki for realtime editing with yjs based editors.
 * Technically, this is a wrapper on top of y-websocket.
 * @since 18.4.0RC1
 * @beta
 */
@injectable()
export class XWikiCollaborationManager extends AbstractCollaborationManager {
  constructor(
    @inject("AuthenticationManagerProvider")
    authenticationManagerProvider: AuthenticationManagerProvider,

    @inject(documentServiceName)
    documentService: DocumentService,

    @inject("ModelReferenceSerializer")
    @named("XWiki")
    modelReferenceSerializer: ModelReferenceSerializer,

    @inject("CristalApp") private readonly cristalApp: CristalApp,
  ) {
    super(
      authenticationManagerProvider,
      documentService,
      modelReferenceSerializer,
    );
  }

  protected override async createProvider(
    documentReference: DocumentReference,
  ): Promise<WebsocketProvider> {
    console.debug("Loading the collaboration provider.");
    const { createXWikiWebSocketProvider } = await import(
      "./xwikiProviderWebSocket"
    );

    const room = this.modelReferenceSerializer
      .serialize(documentReference)
      ?.substring("doc:".length);
    const locale = documentReference.locale ?? "";
    console.debug(
      `Connecting the the collaboration WebSocket endpoint for document [${room}(${locale})].`,
    );
    return createXWikiWebSocketProvider(
      this.cristalApp.getWikiConfig().realtimeURL!,
      room!,
      locale,
    );
  }

  protected override async createCollaboration(
    provider: WebsocketProvider,
    collaborator: Collaborator,
  ): Promise<Collaboration> {
    collaborator.id = `${provider.doc.clientID}`;
    const collaboration: Collaboration = {
      connectionStatus: ref(ConnectionStatus.Connecting),
      collaborators: ref([]),
      collaborator,
      provider,
      doc: provider.doc,
    };
    this.watchAwarenessChange(collaboration);
    this.watchConnectionStatus(collaboration);
    try {
      await this.sync(collaboration);
    } catch (error) {
      // Otherwise it keeps retrying the connection forever, for a session nobody joined.
      provider.destroy();
      throw error;
    }
    return collaboration;
  }

  private watchAwarenessChange(collaboration: Collaboration): void {
    const provider: WebsocketProvider = collaboration.provider;
    provider.awareness.on("change", () => {
      const collaborators: Collaborator[] = [];
      provider.awareness
        .getStates()
        .forEach(({ collaborator: { user, color } = {} }, clientId) => {
          if (user && color) {
            collaborators.push({
              id: `${clientId}`,
              user,
              color,
            });
          }
        });
      collaboration.collaborators.value = collaborators;
    });
  }

  private watchConnectionStatus(collaboration: Collaboration): void {
    const provider: WebsocketProvider = collaboration.provider;
    provider.on("status", ({ status }) => {
      console.debug("Collaboration connection status:", status);
      switch (status) {
        case "connecting":
          collaboration.connectionStatus.value = ConnectionStatus.Connecting;
          break;
        case "connected":
          collaboration.connectionStatus.value = ConnectionStatus.Connected;
          break;
        case "disconnected":
          collaboration.connectionStatus.value = ConnectionStatus.Disconnected;
          break;
      }
    });
  }

  /**
   * @returns a promise resolved once the session is synchronized, and rejected if the connection fails or the
   *   synchronization doesn't arrive in time
   */
  private sync(collaboration: Collaboration): Promise<void> {
    return new Promise((resolve, reject) => {
      const provider: WebsocketProvider = collaboration.provider;
      const onSync = () => {
        stopListening();
        console.debug("Collaboration synchronized.");
        resolve();
      };
      const onConnectionClose = () => {
        stopListening();
        reject(new Error("Failed to connect to the collaboration server."));
      };
      const timeout = setTimeout(() => {
        stopListening();
        reject(
          new Error("Timed out waiting for the collaboration server to sync."),
        );
      }, SYNC_TIMEOUT);
      // Stop listening once the outcome is known, so that a later disconnection isn't taken for a failed join.
      function stopListening() {
        clearTimeout(timeout);
        provider.off("sync", onSync);
        provider.off("connection-close", onConnectionClose);
      }
      // We can't use once() here: it registers a wrapper that off() doesn't remove.
      provider.on("sync", onSync);
      provider.on("connection-close", onConnectionClose);
    });
  }

  protected override disconnect(collaboration: Collaboration): void {
    const provider: WebsocketProvider = collaboration.provider;
    provider.destroy();
  }
}
