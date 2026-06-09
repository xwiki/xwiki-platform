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
import type { Collaboration } from "./collaboration";
import type { CollaborationManager } from "./collaborationManager";
import type { Collaborator } from "./collaborator";
import type { AuthenticationManagerProvider } from "@xwiki/platform-authentication-api";
import type { DocumentService } from "@xwiki/platform-document-api";
import type { DocumentReference } from "@xwiki/platform-model-api";
import type { ModelReferenceSerializer } from "@xwiki/platform-model-reference-api";

/**
 * Base class for collaboration managers.
 *
 * @since 18.4.0RC1
 * @beta
 */
export abstract class AbstractCollaborationManager
  implements CollaborationManager
{
  /**
   * The mapping between (string) document references and their associated collaboration sessions. We also counts the
   * number of times the current user has joined the collaboration session for a given document reference. This is used
   * to determine when to actually leave a collaboration session (i.e., when the count reaches 0).
   */
  protected readonly collaborations: Map<
    string,
    { promise: Promise<Collaboration>; usageCount: number }
  > = new Map();

  constructor(
    protected readonly authenticationManagerProvider: AuthenticationManagerProvider,
    protected readonly documentService: DocumentService,
    protected readonly modelReferenceSerializer: ModelReferenceSerializer,
  ) {}

  async join(documentReference?: DocumentReference): Promise<Collaboration> {
    documentReference =
      documentReference ??
      this.documentService.getCurrentDocumentReference().value;
    if (!documentReference) {
      throw new Error(
        "Cannot join collaboration session: " +
          "no document reference provided and the current document reference is undefined.",
      );
    }
    const key = this.modelReferenceSerializer.serialize(documentReference);
    let collaboration = this.collaborations.get(key!);
    if (collaboration) {
      collaboration.usageCount++;
    } else {
      collaboration = {
        promise: this.connect(documentReference),
        usageCount: 1,
      };
      this.collaborations.set(key!, collaboration);
    }
    return collaboration.promise;
  }

  leave(documentReference?: DocumentReference): void {
    documentReference =
      documentReference ??
      this.documentService.getCurrentDocumentReference().value;
    if (documentReference) {
      const key = this.modelReferenceSerializer.serialize(documentReference);
      const collaboration = this.collaborations.get(key!);
      if (collaboration) {
        if (collaboration.usageCount > 1) {
          collaboration.usageCount--;
        } else {
          this.disconnectAsync(key!, collaboration);
        }
      } else {
        console.warn(
          "Cannot leave a collaboration session the current user is not connected to.",
          documentReference,
        );
      }
    } else {
      console.warn(
        "Cannot leave collaboration session: " +
          "no document reference provided and the current document reference is undefined.",
      );
    }
  }

  private async connect(
    documentReference: DocumentReference,
  ): Promise<Collaboration> {
    console.debug("Joining realtime collaboration.");
    const collaborator = await this.getCollaborator();
    const provider = await this.createProvider(documentReference);
    return await this.createCollaboration(provider, collaborator);
  }

  private async getCollaborator(): Promise<Collaborator> {
    console.debug("Fetching current user details.");
    const authentication = this.authenticationManagerProvider.get();
    const user = await authentication!.getUserDetails();
    return {
      user,
      color: AbstractCollaborationManager.stringToColor(user.username!),
    };
  }

  /**
   * Convert a string to a color. Similar to a hash, changing the input string slightly will result in a totally
   * different color. Colors should be expected to be nice-looking and different enough from one another.
   *
   * @param str - An input string (e.g., username, document title, etc.)
   * @param prc - Optional lightness/darkness variation number
   *
   * @returns the generated color, in hexadecimal (e.g. `#789ABC`)
   */
  private static stringToColor(str: string, prc?: number): string {
    // Check for optional lightness/darkness
    prc = prc ?? -10;

    let hash = 0;

    for (let i = 0; i < str.length; i++) {
      hash = str.charCodeAt(i) + ((hash << 5) - hash);
    }

    const rgba =
      ((hash >> 24) & 0xff).toString(16) +
      ((hash >> 16) & 0xff).toString(16) +
      ((hash >> 8) & 0xff).toString(16) +
      (hash & 0xff).toString(16);

    const num = Number.parseInt(rgba, 16),
      amt = Math.round(2.55 * prc),
      R = (num >> 16) + amt,
      G = ((num >> 8) & 0x00ff) + amt,
      B = (num & 0x0000ff) + amt;

    const comp =
      0x1000000 +
      (R < 255 ? (R < 1 ? 0 : R) : 255) * 0x10000 +
      (G < 255 ? (G < 1 ? 0 : G) : 255) * 0x100 +
      (B < 255 ? (B < 1 ? 0 : B) : 255);

    return `#${comp.toString(16).slice(1)}`;
  }

  private async disconnectAsync(
    key: string,
    collaboration: {
      promise: Promise<Collaboration>;
      usageCount: number;
    },
  ): Promise<void> {
    this.collaborations.delete(key);
    console.debug("Leaving realtime collaboration.");
    this.disconnect(await collaboration.promise);
  }

  protected abstract createProvider(
    documentReference: DocumentReference, // eslint-disable-next-line @typescript-eslint/no-explicit-any
  ): Promise<any>;

  protected abstract createCollaboration(
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    provider: any,
    collaborator: Collaborator,
  ): Promise<Collaboration>;

  protected abstract disconnect(collaboration: Collaboration): void;
}
