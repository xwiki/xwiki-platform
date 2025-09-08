/**
 * See the LICENSE file distributed with this work for additional
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
import { Status } from "@xwiki/cristal-collaboration-api";
import { name as documentServiceName } from "@xwiki/cristal-document-api";
import { inject, injectable } from "inversify";
import { ref } from "vue";
import type { CristalApp } from "@xwiki/cristal-api";
import type {
  CollaborationInitializer,
  CollaborationManager,
  User,
} from "@xwiki/cristal-collaboration-api";
import type { DocumentService } from "@xwiki/cristal-document-api";
import type { Ref } from "vue";

/**
 * A collaboration provider for the websocket endpoint provided by xwiki for realtime editing with yjs based editors.
 * Technically, this is a wrapper on top of y-websocket.
 * @since 0.20
 * @beta
 */
@injectable()
export class XWikiCollaborationProvider implements CollaborationManager {
  private readonly statusRef: Ref<Status> = ref(Status.Connected);
  private readonly usersRef: Ref<User[]> = ref([]);

  constructor(
    @inject("CristalApp") private readonly cristalApp: CristalApp,
    @inject(documentServiceName)
    private readonly documentService: DocumentService,
  ) {}

  status(): Ref<Status> {
    return this.statusRef;
  }

  async get(): Promise<() => CollaborationInitializer> {
    const { createXWikiWebSocketProvider } = await import(
      "./xwikiProviderWebSocket"
    );

    return () => {
      const provider = createXWikiWebSocketProvider(
        this.cristalApp.getWikiConfig().realtimeURL!,
        this.documentService.getCurrentDocumentReferenceString().value!,
      );

      provider.awareness.on("change", () => {
        const users: User[] = [];
        provider.awareness
          .getStates()
          .forEach(({ user: { name, color } = {} }, clientId) => {
            if (name && color) {
              users.push({
                clientId: `${clientId}`,
                user: { name, color },
              });
            }
          });
        this.usersRef.value = users;
      });

      const promise = new Promise((resolve) => {
        provider.on("status", ({ status }) => {
          if (status === "connected") {
            resolve(undefined);
          }
        });
      });

      return { provider, doc: provider.doc, initialized: promise };
    };
  }

  users(): Ref<User[]> {
    return this.usersRef;
  }
}
