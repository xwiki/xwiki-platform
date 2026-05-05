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
import type { DocumentReference } from "@xwiki/platform-model-api";

/**
 * The role name used to retrieve the collaboration manager from the component manager.
 *
 * @since 18.4.0RC1
 * @beta
 */
const collaborationManagerName: string = "CollaborationManager";

/**
 * Used to connect to a realtime collaboration session using a specific collaboration provider (e.g. hocuspocus or
 * y-websocket).
 *
 * @since 18.4.0RC1
 * @beta
 */
interface CollaborationManager {
  /**
   * Attempts to connect to the realtime collaboration session for the specified document (or the current document), and
   * returns the joined collaboration once the connection is established and the initial synchronization is done.
   *
   * @param documentReference - the reference of the document for which to join the realtime collaboration session;
   *   falls back to the current document reference when not specified
   * @returns a promise that resolves with the joined collaboration, once the connection is established and the initial
   *   synchronization is done
   */
  join(documentReference?: DocumentReference): Promise<Collaboration>;

  /**
   * Leave the collaboration session for the specified document (or the current document), disconnecting the current
   * user and notifying the other collaborators.
   */
  leave(documentReference?: DocumentReference): void;
}

export { type CollaborationManager, collaborationManagerName };
