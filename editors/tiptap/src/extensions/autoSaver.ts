/*
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

import { User } from "./collaboration";
import { HocuspocusProvider } from "@hocuspocus/provider";
import EventEmitter from "eventemitter3";

interface AutoSaverAwarenessState {
  // The number of local changes made so far.
  updateCount: number;

  // The update count for each user (clientID) when the document was last saved by this user (clientID). Undefined if
  // there was no save triggered by this user. The key is the clientID and the value is the update count.
  savedUpdateCount: { [key: number]: number };

  // Whether this user has unsaved local changes.
  dirty: boolean;

  // Whether this user is currently saving.
  saving: boolean;
}

enum AutoSaverStatus {
  UNSAVED = 1,
  SAVING,
  SAVED,
}

class AutoSaver extends EventEmitter {
  private static readonly AWARENESS_FIELD = "autoSaver";

  private hocuspocusProvider: HocuspocusProvider;

  /**
   * The function that performs the actual save. Receives the users that have made changes since the last save.
   */
  private callback: (authors: User[]) => Promise<void>;

  /**
   * The last known status of the auto saver, used to determine if the status has changed.
   */
  private previousStatus: AutoSaverStatus = AutoSaverStatus.SAVED;

  /**
   * Used to debounce saves.
   */
  private saveTimer: ReturnType<typeof setTimeout> | undefined;

  /**
   * The awareness state used by the auto saver.
   */
  private state: AutoSaverAwarenessState = {
    updateCount: 0,
    savedUpdateCount: {},
    dirty: false,
    saving: false,
  };

  constructor(
    hocuspocusProvider: HocuspocusProvider,
    callback: (authors: User[]) => Promise<void>,
  ) {
    super();

    this.hocuspocusProvider = hocuspocusProvider;
    this.callback = callback;
    // Set the initial awareness state.
    this.updateAwarenessState();
    hocuspocusProvider.on(
      "awarenessChange",
      this.updateAwarenessState.bind(this),
    );
    hocuspocusProvider.document.on("update", this.onUpdate.bind(this));
  }

  private updateAwarenessState(): void {
    this.state.dirty =
      // The content can't be dirty if there are no local changes.
      this.state.updateCount > 0 &&
      // Check if there is a user (client) that has saved all our local changes.
      !this.someState((state) => {
        const savedUpdateCount =
          state.savedUpdateCount[this.hocuspocusProvider.document.clientID];
        return (
          savedUpdateCount !== undefined &&
          savedUpdateCount >= this.state.updateCount
        );
      });

    this.hocuspocusProvider.awareness?.setLocalStateField(
      AutoSaver.AWARENESS_FIELD,
      this.state,
    );

    const status = this.getStatus();
    if (status !== this.previousStatus) {
      this.previousStatus = status;
      this.emit("statusChange", status);
    }
  }

  private onUpdate(update: Uint8Array, origin: unknown): void {
    // Cancel the previously scheduled save.
    clearTimeout(this.saveTimer);

    if (origin !== this.hocuspocusProvider) {
      this.state.updateCount++;
      this.state.dirty = true;
      this.updateAwarenessState();

      // This is a local update so we schedule a new save in 1 second.
      this.scheduleSave();
    }
  }

  private scheduleSave(): void {
    this.saveTimer = setTimeout(this.onSave.bind(this), 1000);
  }

  private onSave(): void {
    if (this.isSomeoneSaving()) {
      console.debug("Rescheduling our save because someone else is saving.");
      this.scheduleSave();
    } else if (this.isSomeoneDirty()) {
      this.save();
    }
  }

  async save(): Promise<void> {
    const authors = this.getAuthors();
    if (authors.length == 0) {
      console.debug("Nothing to save.");
      return;
    }

    this.state.saving = true;
    this.updateAwarenessState();

    const savedUpdateCount = this.getUpdateCounts();
    console.debug("Saving ", savedUpdateCount);

    try {
      // Trigger the actual save.
      await this.callback(authors);
      this.state.savedUpdateCount = savedUpdateCount;
    } catch (error) {
      console.error("Failed to save.", error);
    } finally {
      this.state.saving = false;
      this.updateAwarenessState();
    }
  }

  private isSomeoneSaving(): boolean {
    return this.someState((state) => state.saving);
  }

  private isSomeoneDirty(): boolean {
    return this.someState((state) => state.dirty);
  }

  private someState(
    callback: (state: AutoSaverAwarenessState) => boolean,
  ): boolean {
    const awarenessStates =
      this.hocuspocusProvider.awareness?.getStates().values() || [];
    for (const awarenessState of awarenessStates) {
      if (callback(awarenessState[AutoSaver.AWARENESS_FIELD])) {
        return true;
      }
    }
    return false;
  }

  private getUpdateCounts(): { [key: number]: number } {
    const updateCounts: { [key: number]: number } = {};
    this.hocuspocusProvider.awareness
      ?.getStates()
      .forEach((awarenessState, clientID) => {
        const state = awarenessState[AutoSaver.AWARENESS_FIELD];
        updateCounts[clientID] = state?.updateCount || 0;
      });
    return updateCounts;
  }

  /**
   * @returns the users that have made changes since the last save
   */
  private getAuthors(): User[] {
    const authors: User[] = [];
    const awarenessStates =
      this.hocuspocusProvider.awareness?.getStates().values() || [];
    for (const awarenessState of awarenessStates) {
      const state = awarenessState[AutoSaver.AWARENESS_FIELD];
      if (state.dirty) {
        authors.push({ ...awarenessState.user });
      }
    }
    return authors;
  }

  private getStatus(): AutoSaverStatus {
    if (this.isSomeoneSaving()) {
      return AutoSaverStatus.SAVING;
    } else if (this.isSomeoneDirty()) {
      return AutoSaverStatus.UNSAVED;
    } else {
      return AutoSaverStatus.SAVED;
    }
  }
}

export { AutoSaverStatus, AutoSaver };
