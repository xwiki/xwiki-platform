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

/**
 * The state a client taking part in an editing session publishes to the other clients, so that they can agree on who
 * saves and on whether anything is left to save.
 *
 * A transport is free to add its own fields, e.g. to tell whether the client owning a state is still connected.
 *
 * @since 18.8.0RC1
 * @beta
 */
type SaverState = {
  /**
   * The number of changes made by this client since its saver was created. Comparing it with the saved update count
   * of every client is what tells whether this client has unsaved changes.
   */
  updateCount?: number;

  /**
   * The number of changes of each client that were last saved by this client, keyed by client identifier.
   */
  savedUpdateCount?: Record<string, number>;

  /**
   * Whether this client has changes that no client has saved yet.
   */
  dirty?: boolean;

  /**
   * The priority with which this client is currently attempting to save, or 0 when it is not saving. The client with
   * the highest priority wins the save election, so that for instance a manual save can outrank an auto-save.
   */
  saving?: number;

  /**
   * The last document version this client created, used by the targets that need to know that another client saved.
   */
  version?: string;
};

export type { SaverState };
