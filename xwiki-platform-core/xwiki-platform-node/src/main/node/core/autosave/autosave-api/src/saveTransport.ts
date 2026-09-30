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

import type { Saver } from "./saver";
import type { SaverState } from "./saverState";

/**
 * Replicates the saver state of each client taking part in the editing session. How the states travel between the
 * clients is left to the implementations, e.g. over a ChainPad channel or a Yjs document.
 *
 * @typeParam C - the type of the save context the saver passes to its target
 * @since 18.8.0RC1
 * @beta
 */
abstract class SaveTransport<C extends object = object> {
  /**
   * The state of the local client, that gets propagated to the other clients. A transport replaces it when it
   * receives a remote snapshot that carries the local state, so it is not read only.
   */
  protected state: SaverState = {
    updateCount: 0,
    savedUpdateCount: {},
    dirty: false,
    saving: 0,
  };

  /**
   * @param saver - the saver using this transport, notified when the remote states change
   */
  public constructor(protected readonly saver: Saver<C>) {}

  /**
   * Connect to the channel and start receiving the states of the other clients. Called after both the transport and
   * the target have been created, so that the saver is ready to be notified.
   */
  public abstract initialize(): void;

  /**
   * @returns a promise that resolves when the transport is connected and the states of the other clients are
   *   available
   */
  public abstract toBeReady(): Promise<void>;

  /**
   * @returns the identifier of the local client, used as key in the map returned by {@link SaveTransport.getStates}
   */
  public abstract getClientId(): string;

  /**
   * @returns the saver state of each client taking part in the editing session, keyed by client identifier
   */
  public abstract getStates(): Record<string, SaverState>;

  /**
   * This is normally the entry that {@link SaveTransport.getStates} holds for
   * {@link SaveTransport.getClientId}, but not always: a transport can receive a remote snapshot that doesn't
   * include the local client yet, in which case it has to keep the local state aside until the next push re-inserts
   * it in the map. Reading the local state through this method, rather than from the map, is what makes the saver
   * immune to that window.
   *
   * @returns the saver state of the local client
   */
  public getLocalState(): SaverState {
    return this.state;
  }

  /**
   * Apply the given changes to the local saver state and optionally propagate them to the other clients.
   *
   * @param patch - the state properties to modify
   * @param options - the options of this update
   * @returns a promise that resolves when the update has been applied (and propagated, when asked to)
   */
  public abstract updateLocalState(
    patch: Partial<SaverState>,
    options?: {
      /**
       * Whether to propagate the new state to the other clients.
       */
      push?: boolean;

      /**
       * Whether to wait for the new state to reach the other clients.
       */
      immediate?: boolean;
    },
  ): Promise<void>;

  /**
   * @param state - the saver state of a client
   * @returns whether the client owning the given state is still taking part in the editing session
   */
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  public isConnected(state: SaverState): boolean {
    return true;
  }

  /**
   * @returns a promise that resolves when the local state has been received by the other clients
   */
  public whenSettled(): Promise<void> {
    return Promise.resolve();
  }

  /**
   * Disconnect from the channel and revert the changes made to the environment.
   */
  public abstract dispose(): Promise<void>;
}

export { SaveTransport };
