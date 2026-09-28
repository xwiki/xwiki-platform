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
import type { Collaborator } from "./collaborator";
import type { ConnectionStatus } from "./connectionStatus";
import type { Saver } from "@xwiki/platform-autosave-api";
import type { Ref } from "vue";
import type { Doc } from "yjs";

/**
 * Holds information about a collaboration session.
 *
 * @since 18.4.0RC1
 * @beta
 */
export type Collaboration = {
  /**
   * Specifies whether the current user is connected, connecting or disconnected from this collaboration. The connection
   * status is reactive and will be updated when the connection status changes. Note that the collaboration provider
   * will automatically try to reconnect when the connection is lost.
   */
  connectionStatus: Ref<ConnectionStatus>;

  /**
   * The list of users that have joined this collaboration. The list is reactive and will be updated when users join or
   * leave the collaboration.
   */
  collaborators: Ref<Collaborator[]>;

  /**
   * Information on how the current user has presented itself to the other collaborators. This information doesn't
   * change during the collaboration session.
   */
  collaborator: Collaborator;

  /**
   * The collaboration provider used to synchronize the shared document between the collaborators (e.g. hocuspocus,
   * y-websocket).
   */
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  provider: any;

  /**
   * The shared Yjs document that is synchronized by the collaboration provider between the collaborators.
   */
  doc: Doc;

  /**
   * The auto-saver shared by every editor taking part in this collaboration session. Building one requires knowing
   * how the edited content is saved, which the collaboration manager has no way of telling, so the first editor
   * that joins creates it and the manager only stops it when the session is left.
   */
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  saver?: Saver<any>;
};
