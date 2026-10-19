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
// eslint-disable-next-line import-x/no-named-as-default
import EventEmitter from "eventemitter3";
import { reactive } from "vue";
import type { Logic, Values } from "@xwiki/platform-livedata-api";
import type { Reactive } from "vue";

/**
 * Centralizes the edition event listeners and maintain a centralized state of the edition states
 * and results for the live data. This is useful to know when to save the table, and to know
 * which cells can be edited according to the current edit state
 * @since 18.2.0RC1
 * @beta
 */
export class EditBusService {
  private eventEmitter: EventEmitter;
  private readonly editStates: Reactive<{
    [key: string]: { [key: string]: { editing: boolean } };
  }>;
  // The last queued save.
  // Each save starts once the previous one is done, preventing entries from being created multiple times.
  private runningSave: Promise<void> = Promise.resolve();

  /**
   * Default constructor.
   * @param logic - the live data logic instance
   */
  constructor(private readonly logic: Logic) {
    this.eventEmitter = new EventEmitter();
    this.editStates = reactive({});
    this.init();
  }

  /**
   * Initializes the Vue events listeners.
   */
  private init() {
    this.eventEmitter.on("start-editing-entry", ({ entryId, propertyId }) => {
      const entryState = this.editStates[entryId] || {};
      const propertyState = entryState[propertyId] || {};
      propertyState.editing = true;
      entryState[propertyId] = propertyState;
      this.editStates[entryId] = entryState;
    });

    this.eventEmitter.on("cancel-editing-entry", ({ entryId, propertyId }) => {
      this.editStates[entryId][propertyId].editing = false;
    });

    this.eventEmitter.on("save-editing-entry", ({ entryId, propertyId }) => {
      // The cell is not edited anymore, its content is saved by save().
      this.editStates[entryId][propertyId].editing = false;
    });
  }

  /**
   * Save the value of a cell server side.
   * @param entryId - the entry id of the cell to save
   * @param content - the values of the cell to save
   */
  private async _save(entryId: string, content: unknown) {
    try {
      await this.logic.setValues({ entryId, values: content });
    } catch {
      // The logic already notified the user of failure, the next saves must still run.
    }
  }

  /**
   * Indicates if cells are allowed to switch to edit mode. For instance, a cell is not allowed to be edited if another
   * cell is already in edit mode. In this case the currently edited cell must be saved or canceled before another one
   * can be edited.
   * @returns true if the cells are allowed to switch to edit mode, false otherwise
   */
  public isEditable() {
    for (const editStatesKey in this.editStates) {
      const editStates = this.editStates[editStatesKey];
      for (const editStateKey in editStates) {
        const editState = editStates[editStateKey];
        if (editState.editing) {
          return false;
        }
      }
    }
    return true;
  }

  onAnyEvent(callback: () => unknown) {
    for (const event of [
      "save-editing-entry",
      "start-editing-entry",
      "cancel-editing-entry",
    ]) {
      this.eventEmitter.on(event, callback);
    }
  }

  private startEvent(entry: Values, propertyId: string) {
    this.eventEmitter.emit("start-editing-entry", {
      entryId: this.logic.getEntryId(entry),
      propertyId,
    });
  }

  private cancelEvent(entry: Values, propertyId: string) {
    this.eventEmitter.emit("cancel-editing-entry", {
      entryId: this.logic.getEntryId(entry),
      propertyId,
    });
  }

  private saveEvent(entry: Values, propertyId: string) {
    this.eventEmitter.emit("save-editing-entry", {
      entryId: this.logic.getEntryId(entry),
      propertyId,
    });
  }

  /**
   * Notifies the start of a cell modification. After this event, the cell is considered as edited unless it is
   * canceled.
   * @param entry - the entry of the edited row
   * @param propertyId - the property id of the edited cell.
   */
  start(entry: Values, propertyId: string) {
    this.startEvent(entry, propertyId);
  }

  /**
   * Notifies the cancellation of the edition of a cell. The cell rollback to a non modified state.
   * @param entry - the entry of the edited row
   * @param propertyId - the property id of the edited cell
   */
  cancel(entry: Values, propertyId: string) {
    this.cancelEvent(entry, propertyId);
  }

  /**
   * Notifies the save of a cell, and saves its content once the previous saves are done.
   * @param entry - the entry of the edit row
   * @param propertyId - the property id of the edited cell
   * @param content - the attributes of the edit cell form, in the form of an object
   * @returns a promise completing when the save is done
   */
  save(entry: Values, propertyId: string, content: unknown) {
    this.saveEvent(entry, propertyId);
    // A new row only gets an id once the save creating its entry is done, so the id is read here, not when queueing.
    const previousSave = this.runningSave;
    // Chain on the previous save so that saves run one at a time.
    this.runningSave = (async () => {
      await previousSave;
      await this._save(this.logic.getEntryId(entry) as string, content);
    })();
    return this.runningSave;
  }

  /**
   * Discard the edit states of an entry that is not displayed anymore.
   * @param entry - the entry whose edit states are discarded
   * @since 18.9.0RC1
   */
  public discard(entry: Values) {
    delete this.editStates[this.logic.getEntryId(entry) as string];
  }

  /**
   * Move the edit states of an entry to the identifier it just got.
   * @param previousEntryId - the identifier the edit states are registered on
   * @param entryId - the identifier to move them to
   * @since 18.9.0RC1
   */
  public reassign(previousEntryId: string | undefined, entryId: string) {
    const editStates = this.editStates[previousEntryId as string];
    if (editStates) {
      this.editStates[entryId] = editStates;
      delete this.editStates[previousEntryId as string];
    }
  }

  /**
   * Waits for the save started by the last {@link save} call.
   * @returns a promise that completes when the running save is done
   * @since 18.9.0RC1
   */
  public whenSaved() {
    return this.runningSave;
  }
}
