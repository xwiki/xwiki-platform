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
 * What the other clients need to know about a save.
 *
 * @since 18.8.0RC1
 * @beta
 */
type SaveResult = {
  /**
   * The document version the save created, left unset when it created none, e.g. because the content had not
   * actually changed.
   */
  version?: string;
};

/**
 * Saves the edited content, and reports what the other clients need to know about the result. How the content is
 * saved is left to the implementations, e.g. by submitting an edit form or by calling a storage service.
 *
 * @typeParam C - the type of the save context, which the implementation defines and reads
 * @since 18.8.0RC1
 * @beta
 */
abstract class SaveTarget<C extends object = object> {
  /**
   * @param saver - the saver using this target, asked to perform the manual saves
   */
  public constructor(protected readonly saver: Saver<C>) {}

  /**
   * Called when the transport is ready, to install the listeners used to detect and intercept the save requests
   * (e.g. when the user clicks on the save button). This has to wait for the transport because a save accepted
   * before the states of the other clients are known would elect no client and thus save nothing.
   *
   * @returns a promise that resolves when the target is ready to intercept the save requests
   */
  public abstract initialize(): Promise<void>;

  /**
   * @param context - the save context, e.g. holding the save button in case of a manual save
   * @returns the priority of this save; the client with the highest priority wins the save election
   */
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  public getSavePriority(context: C): number {
    // By default all clients have the same priority when saving. Subclasses may override this method to give higher
    // priority to manual saves, for instance (i.e. when the user clicks on the save button).
    return 1;
  }

  /**
   * @returns the interval between two consecutive saves, in milliseconds, or undefined to let the saver use its
   *   default; called each time a save is scheduled, so the value can change during the editing session
   */
  public getSaveInterval(): number | undefined {
    // Subclasses may override this method to read the interval from the environment they save to.
    return undefined;
  }

  /**
   * Save the edited content.
   *
   * @param context - the save context, e.g. holding the save button in case of a manual save
   * @returns a promise that resolves with the save result, holding the created version, if any
   */
  public abstract submit(context: C): Promise<SaveResult>;

  /**
   * Called whenever the saver states change, so that the target can react (e.g. take into account the version
   * created by another client, in order to prevent a merge conflict on the next save).
   *
   * @param states - the saver state of each client, keyed by client identifier
   * @param localClientId - the identifier of the local client
   */
  public abstract onStatesChanged(
    states: Record<string, SaverState>,
    localClientId: string,
  ): void;

  /**
   * Revert the changes made to the environment.
   */
  public abstract dispose(): void;
}

export { SaveTarget };
export type { SaveResult };
