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
import type {
  onAwarenessChangeParameters,
  onStatusParameters,
} from "@hocuspocus/provider";
import type { CristalApp } from "@xwiki/cristal-api";
import type {
  CollaborationInitializer,
  CollaborationManager,
  User,
} from "@xwiki/cristal-collaboration-api";
import type { DocumentService } from "@xwiki/cristal-document-api";
import type { Ref } from "vue";

/**
 * Collaboration provider for Hocus Pocus.
 * This is the default provider.
 * @since 0.20
 * @beta
 */
@injectable()
export class HocuspocusCollaborationProvider implements CollaborationManager {
  private readonly statusRef: Ref<Status> = ref(Status.Connecting);
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
    const { HocuspocusProvider, WebSocketStatus } = await import(
      "@hocuspocus/provider"
    );

    return () => {
      const provider = new HocuspocusProvider({
        url: this.cristalApp.getWikiConfig().realtimeURL!,
        // we distinguish from sessions from other editors by suffixing the session with the editor id
        name: `${this.documentService.getCurrentDocumentReferenceString().value}:${
          this.cristalApp.getWikiConfig().editor ?? "blocknote"
        }`,
      });
      // As soon as the provider's status changes, update it
      provider.on("status", (event: onStatusParameters) => {
        let status;
        switch (event.status) {
          case WebSocketStatus.Connecting:
            status = Status.Connecting;
            break;
          case WebSocketStatus.Connected:
            status = Status.Connected;
            break;
          case WebSocketStatus.Disconnected:
            status = Status.Disconnected;
            break;
        }

        this.statusRef.value = status;
      });

      provider.on("awarenessChange", (event: onAwarenessChangeParameters) => {
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        this.usersRef.value = Array.from(event.states.values() as any);
      });

      return {
        provider,
        doc: provider.document,
        initialized: new Promise((resolve) => {
          provider.on("synced", () => resolve(undefined));
        }),
      };
    };
  }

  users(): Ref<User[]> {
    return this.usersRef;
  }
}
