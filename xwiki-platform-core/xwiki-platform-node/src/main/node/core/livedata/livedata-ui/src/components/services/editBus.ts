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
    [key: string]: {
      [key: string]: { editing: boolean; tosave: boolean; content: unknown };
    };
  }>;
  // This variable stores an edit request made while an edition is in progress.
  private pendingEdit?: { entryId: string; propertyId: string };

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
      const entryState = this.editStates[entryId];
      const propertyState = entryState[propertyId];

      // The entry is not edited anymore.
      // The content is not edited, and should be `undefined` if the property was edited for the
      // first time. If the property was edited and saved a first time, then edited and
      // cancelled, the content must stay to one from the first edit.
      propertyState.editing = false;
    });

    this.eventEmitter.on(
      "save-editing-entry",
      ({ entryId, propertyId, content }) => {
        const entryState = this.editStates[entryId];
        const propertyState = entryState[propertyId];
        // The entry is not edited anymore but its content will need to be saved once the rest of
        // the properties of the  entry are not in edit mode.
        propertyState.editing = false;
        propertyState.tosave = true;
        propertyState.content = content;
        this._save(entryId);
      },
    );
  }

  /**
   * Save the changed values of an entry server side.
   * @param entryId - the entry id of the entry to save
   */
  // eslint-disable-next-line max-statements
  private _save(entryId: string) {
    const values = this.editStates[entryId];
    let canBeSaved = false;
    // This variable can't be initialized in the for loop since it's value is used after the loop.
    let keyEntry;

    // Look for the single cell to save.
    for (keyEntry in values) {
      const entryValue = values[keyEntry];

      const editing = entryValue.editing;
      const tosave = entryValue.tosave;
      canBeSaved = !editing && tosave;

      if (canBeSaved) {
        break;
      }
    }

    // If a cell to save is found, we get its content and save it.
    if (canBeSaved && keyEntry) {
      const vals = values[keyEntry].content;

      this.logic
        .setValues({ entryId, values: vals })
        // eslint-disable-next-line promise/always-return
        .then(() => {
          delete this.editStates[entryId][keyEntry];
        })
        .catch(() => {
          // @ts-expect-error leftover from initial javascript implementation
          new XWiki.widgets.Notification(
            `The row save action failed.`,
            "error",
          );
        });
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

  /**
   * Indicates whether a cell has been edited and is in the middle of the save
   * process. In that state, opening a new cell for edition should be deferred
   * (see {@link requestEdit}), since a refresh will happen and re-render the
   * cells.
   *
   * @returns true if a save is in progress, false otherwise
   */
  public hasPendingSave() {
    for (const propertyStates of Object.values(this.editStates)) {
      for (const propertyState of Object.values(propertyStates)) {
        if (propertyState.tosave) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Stores a request for edition, to perform it later (once all save processes
   * have completed). See {@link enablePendingEdit} to apply it.
   *
   * @param entryId - the entry id of the cell to edit
   * @param propertyId - the property id of the cell to edit
   */
  public requestEdit(entryId: string, propertyId: string) {
    this.pendingEdit = { entryId, propertyId };
  }

  /**
   * Enables a pending edit request if it targets the given cell.
   *
   * @param entryId - the entry id of the cell
   * @param propertyId - the property id of the cell
   * @returns true if the given cell had a pending edit request, false otherwise
   */
  public enablePendingEdit(entryId: string, propertyId: string) {
    if (
      this.pendingEdit?.entryId === entryId &&
      this.pendingEdit?.propertyId === propertyId
    ) {
      this.pendingEdit = undefined;
      return true;
    }
    return false;
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

  private saveEvent(entry: Values, propertyId: string, content: unknown) {
    this.eventEmitter.emit("save-editing-entry", {
      entryId: this.logic.getEntryId(entry),
      propertyId: propertyId,
      content: content,
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
   * Notifies the save of  a cell. With the current save strategy, the cell is directly saved and the table is reload
   * after this notification.
   * @param entry - the entry of the edit row
   * @param propertyId - the property id of the edited cell
   * @param content - the attributes of the edit cell form, in the form of an object
   */
  save(entry: Values, propertyId: string, content: unknown) {
    this.saveEvent(entry, propertyId, content);
  }
}
