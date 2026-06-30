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
define('xwiki-realtime-wysiwyg-editHistory', [
  'xwiki-realtime-history',
  'xwiki-realtime-wysiwyg-selection'
], function (
  History,
  SelectionUtils
) {
  'use strict';

  /**
   * Manages the edit history for given editor, allowing undo/redo of local changes.
   */
  class EditHistory extends History {
    /**
     * Creates a new edit history for the provided editor.
     *
     * @param {Patches} patchedEditor the editor to track the history for
     */
    constructor(patchedEditor) {
      super({...EditHistory._getHyperJSONWithSelection(patchedEditor)});

      this._patchedEditor = patchedEditor;
      this._editor = patchedEditor._editor;
      this._handler = this._editor.handleHistory(this);
    }

    /** @inheritdoc */
    canUndo() {
      return !this._editor.isReadOnly() && super.canUndo();
    }

    /** @inheritdoc */
    async undo() {
      if (this.canUndo()) {
        await this._setHyperJSONWithSelection(super.undo());
      }
    }

    /** @inheritdoc */
    canRedo() {
      return !this._editor.isReadOnly() && super.canRedo();
    }

    /** @inheritdoc */
    async redo() {
      if (this.canRedo()) {
        await this._setHyperJSONWithSelection(super.redo());
      }
    }

    /** @inheritdoc */
    changeContent(...args) {
      super.changeContent(...args);
      // Enable/disable the undo/redo buttons.
      this._handler.updateState();
    }

    saveState(isLocalChange = false) {
      const {content, selectionStart, selectionEnd} = EditHistory._getHyperJSONWithSelection(this._patchedEditor);
      this.changeContent(content, selectionStart, selectionEnd, isLocalChange);
      return content;
    }

    /**
     * Destroys this edit history instance.
     */
    destroy() {
      this._handler.destroy();
    }

    static _getHyperJSONWithSelection(patchedEditor) {
      SelectionUtils.markDOMSelection(patchedEditor._editor.getSelection());
      try {
        let hyperJSON = patchedEditor.getHyperJSON();
        // The attributes of the content wrapper (root) are ignored when getting the hyperJSON.
        hyperJSON = SelectionUtils.copyRootSelectionMarkers(patchedEditor._editor.getContentWrapper(), hyperJSON);
        return SelectionUtils.unmarkHyperJSONSelection(hyperJSON);
      } finally {
        SelectionUtils.unmarkDOMSelection(patchedEditor._editor.getContentWrapper());
      }
    }

    async _setHyperJSONWithSelection({content, selectionStart, selectionEnd}) {
      this._editor.saveSelection();
      const hyperJSON = SelectionUtils.markHyperJSONSelection(content, selectionStart, selectionEnd);

      // By putting the editor in read-only mode we:
      // * disable the undo/redo while we wait for the content to be updated
      // * prevent the selection from being saved and restored by the setHyperJSON call, because we want to set the
      // provided selection
      this._editor.setReadOnly(true);
      try {
        await this._patchedEditor.setHyperJSON(hyperJSON);
      } finally {
        this._editor.setReadOnly(false);

        // The attributes of the content wrapper (root) are ignored when setting the hyperJSON.
        SelectionUtils.copyRootSelectionMarkers(hyperJSON, this._editor.getContentWrapper());
        const ranges = SelectionUtils.unmarkDOMSelection(this._editor.getContentWrapper());
        if (ranges.length) {
          this._editor.restoreSelection(ranges);
        }
      }
    }
  }

  return EditHistory;
});