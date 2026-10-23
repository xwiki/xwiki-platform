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
import { SESSION_REPLACED_EVENT } from "./events";
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
 * How long to wait, in milliseconds, for the connection to the collaboration server to be established before giving up
 * on the initial synchronization. Joining a session blocks the editor until then, so it shouldn't take too long, but
 * the provider needs a few connection attempts (with exponential back-off) to get over a transient failure.
 */
const CONNECTION_TIMEOUT = 5000;

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
      warnings: ref([]),
      collaborator,
      provider,
      doc: provider.doc,
    };
    this.watchAwarenessChange(collaboration);
    this.watchConnectionStatus(collaboration);
    await this.sync(collaboration);
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
    // The provider stops synchronizing (so we are disconnected) when the session is replaced.
    // @ts-expect-error The provider doesn't declare our custom event.
    provider.on(SESSION_REPLACED_EVENT, () => {
      collaboration.warnings!.value = [
        ...collaboration.warnings!.value,
        "sessionReplaced",
      ];
    });
  }

  /**
   * Waits for the initial synchronization, or until we give up connecting to the collaboration server. In the latter
   * case the collaboration session starts disconnected, letting the user edit (and save) while the provider keeps
   * trying to connect. The changes are synchronized when the connection is established, unless someone else started
   * the session meanwhile (the provider can't merge two sessions so it stops the synchronization and warns the user).
   *
   * @returns a promise resolved once the session is synchronized, or once we give up waiting for it
   */
  private sync(collaboration: Collaboration): Promise<void> {
    const provider: WebsocketProvider = collaboration.provider;
    return new Promise((resolve) => {
      let timeout: ReturnType<typeof setTimeout>;
      const stopWaiting = () => {
        clearTimeout(timeout);
        provider.off("sync", onSync);
        resolve();
      };
      const onSync = (synced: boolean) => {
        if (synced) {
          console.debug("Collaboration synchronized.");
          stopWaiting();
        }
      };
      const onTimeout = () => {
        if (provider.wsconnected) {
          // The connection is established and the synchronization is in progress. Giving up now would make us
          // initialize the content locally, which can't be merged with the session we're about to synchronize with.
          // Note that the provider closes the connection if the server doesn't send anything for a while.
          timeout = setTimeout(onTimeout, CONNECTION_TIMEOUT);
        } else {
          console.warn(
            "Failed to connect to the collaboration server. Editing offline until the connection is established.",
          );
          stopWaiting();
        }
      };
      timeout = setTimeout(onTimeout, CONNECTION_TIMEOUT);
      // We can't use once() because it registers a wrapper that off() doesn't remove.
      provider.on("sync", onSync);
    });
  }

  protected override disconnect(collaboration: Collaboration): void {
    const provider: WebsocketProvider = collaboration.provider;
    provider.destroy();
  }
}
