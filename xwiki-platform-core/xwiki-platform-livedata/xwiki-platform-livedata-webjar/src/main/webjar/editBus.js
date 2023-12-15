/*
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
 * Vue event bus for the edit action states.
 * Sent events:
 * - start-editing-entry({entryId, propertyId}): when an entry passes in edit mode
 * - cancel-editing-entry({entryId, propertyId}): when an entry goes out of edit mode without saving its edits
 * - save-editing-entry({entryId, propertyId, content}): when an entry goes out of edit model and wants to save its
 *   edits. The content is an object mapping the keys and values of the cell form attributes.
 */
define('edit-bus', ['vue'], (Vue) => {

  /**
   * Initialize the edit bus view and services.
   * @param logic the live data logic instance
   */
  function init(logic) {

    /**
     * Centralizes the edition event listeners and maintain a centralized state of the edition states and results for
     * the live data. This is useful to know when to save the table, and to know which cells can be edited according to
     * the current edit state
     */
    class EditBusService {

      /**
       * Default constructor.
       * @param editBus an edit bus Vue object
       * @param logic the live data logic instance
       */
      constructor(editBus, logic) {
        this.editBus = editBus;
        this.editStates = {};
        this.logic = logic;
      }

      /**
       * Initializes the Vue events listeners.
       */
      init() {
        this.editBus.$on('start-editing-entry', ({entryId, propertyId}) => {
          const entryState = this.editStates[entryId] || {};
          const propertyState = entryState[propertyId] || {};
          propertyState.editing = true;
          entryState[propertyId] = propertyState;
          this.editStates[entryId] = entryState;
        })

        this.editBus.$on('cancel-editing-entry', ({entryId, propertyId}) => {
          const entryState = this.editStates[entryId];
          const propertyState = entryState[propertyId];

          // The entry is not edited anymore.
          // The content is not edited, and should be `undefined` if the property was edited for the first time.
          // If the property was edited and saved a first time, then edited and cancelled, the content must stay to one
          // from the first edit.
          propertyState.editing = false;

        })

        this.editBus.$on('save-editing-entry', ({entryId, propertyId, content}) => {
          const entryState = this.editStates[entryId];
          const propertyState = entryState[propertyId];
          // The entry is not edited anymore but its content will need to be saved once the rest of the properties of
          // the  entry are not in edit mode. 
          propertyState.editing = false;
          propertyState.tosave = true;
          propertyState.content = content;
          this.save(entryId);
        })
      }

      /**
       * Save the changed values of an entry server side.
       * @param entryId the entry id of the entry to save
       */
      save(entryId) {
        const values = this.editStates[entryId];
        var canBeSaved = false;
        var keyEntry = undefined;


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

          this.logic.setValues({entryId, values: vals})
            .then(() => {
              this.editStates[entryId] = {};
            })
            .catch(() => {
              new XWiki.widgets.Notification(`The row save action failed.`, 'error');
            });
        }
      }

      isEditable() {
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

      onAnyEvent(callback) {
        this.editBus.$on(['save-editing-entry', 'start-editing-entry', 'cancel-editing-entry'], () => callback())
      }
    }

    /**
     * Notifies the start of a cell modification. After this event, the cell is considered as edited unless it is
     * canceled.
     * @param entry the entry of the edited row
     * @param propertyId the property id of the edited cell.
     */
    function start(entry, propertyId) {
      _editBus.$emit('start-editing-entry', {
        entryId: _logic.getEntryId(entry),
        propertyId
      });

    }

    /**
     * Notifies the cancellation of the edition of a cell. The cell rollback to a non modified state.
     * @param entry the entry of the edited row
     * @param propertyId the property id of the edited cell
     */
    function cancel(entry, propertyId) {
      _editBus.$emit('cancel-editing-entry', {
        entryId: _logic.getEntryId(entry),
        propertyId
      })

    }

    /**
     * Notifies the save of the a cell. With the current save strategy, the cell is directly save and the table is
     * reload after this notification.
     * @param entry the entry of the edit row
     * @param propertyId the property id of the edited cell
     * @param content the attributes of the edit cell form, in the form of an object
     `
     */
    function save(entry, propertyId, content) {
      _editBus.$emit('save-editing-entry', {
        entryId: _logic.getEntryId(entry),
        propertyId: propertyId,
        content: content
      });
    }

    /**
     * Indicated if cells are allowed to switch to edit mode. For instance, a cell is not allowed to be edited if
     * another cell is already in edit mode. In this case the currently edited cell must be saved or cancelled before
     * another one can be edited.
     * @returns {*} true if the cells are allowed to switch to edit mode, false otherwise
     */
    function isEditable() {
      return _editBusService.isEditable();
    }

    function onAnyEvent(callback) {
      _editBusService.onAnyEvent(callback);
    }

    const _editBus = new Vue();
    const _logic = logic
    const _editBusService = new EditBusService(_editBus, logic);
    _editBusService.init()
    return {start, cancel, save, isEditable, onAnyEvent};
  }

  return {init};
})
