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
import { Status } from "./status";
import type { CollaborationInitializer } from "./collaborationInitializer";
import type { User } from "./user";
import type { Ref } from "vue";

/**
 * The manager for a given collaboration provider (e.g., hocuspocus, or y-websocket).
 * @since 18.0.0RC1
 * @beta
 */
interface CollaborationManager {
  /**
   * A lazy initializer for a collaboration initializer. We proceed that way to allow the collaboration provider to
   * be resolved and assigned outside the editor, but to be effectively initialized inside the editor, where
   * access to the internal editor structure is available.
   */
  get(): Promise<() => CollaborationInitializer>;

  /**
   * The current status.
   */
  status(): Ref<Status>;

  /**
   * The list of currently connected users.
   */
  users(): Ref<User[]>;
}

export type { CollaborationManager };
