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
define('xwiki-realtime-history', [
  'xwiki-realtime-transformers',
  'chainpad',
], function (
  Transformers,
  ChainPad
) {
  'use strict';

  /**
   * Manages the history of changes made to some content, allowing undo/redo operations.
   */
  class History {
    /**
     * Creates a new instance based on the provided options.
     *
     * @param {Object} options the configuration options, including:
     * @param {string} [options.content=''] the initial content
     * @param {Object} [options.selectionStart=0] the initial selection start offset
     * @param {Object} [options.selectionEnd=0] the initial selection end offset
     * @param {function} [options.transform=Transformers.RebaseNaiveJSONTransformer] the transformer function used to
     *   rebase local changes on top of remote changes
     */
    constructor({
      content = '',
      selectionStart = 0,
      selectionEnd = 0,
      transform = Transformers.RebaseNaiveJSONTransformer
    } = {}) {
      this._content = content;
      this._transform = transform;
      // Add an entry matching the initial content, so that we can restore it.
      this._currentEntry = {
        redoOperations: [],
        redoSelection: {start: selectionStart, end: selectionEnd},
        undoOperations: [],
        undoSelection: {start: selectionStart, end: selectionEnd},
        isLocalChange: false,
        previousEntry: null,
        nextEntry: null
      };
    }

    /**
     * Adds a new history entry based on the provided content change.
     *
     * @param {string} newContent the new content
     * @param {number} selectionStart the new selection start offset
     * @param {number} selectionEnd the new selection end offset
     * @param {boolean} isLocalChange whether this is a local or remote change
     */
    changeContent(newContent = '', selectionStart = 0, selectionEnd = 0, isLocalChange = false) {
      if (newContent === this._content) {
        // No change.
        return;
      }
      const redoOperations = this._diff(this._content, newContent);
      const redoSelection = {start: selectionStart, end: selectionEnd};
      const undoOperations = this._invert(redoOperations, this._content);
      const undoSelection = this._rebaseSelection(redoSelection, undoOperations, newContent);
      const newEntry = {
        redoOperations,
        redoSelection,
        undoOperations,
        undoSelection,
        isLocalChange,
        previousEntry: this._currentEntry,
      };
      const redoEntry = this._currentEntry.nextEntry;
      this._currentEntry.nextEntry = newEntry;
      this._currentEntry = newEntry;
      const oldContent = this._content;
      this._content = newContent;
      if (!isLocalChange) {
        this._rebasePreviousLocalChanges();
        this._rebaseNextLocalChanges(redoEntry, {
          redoOperations,
          undoOperations
        }, oldContent);
      }
    }

    /**
     * We can undo only local changes.
     *
     * @returns {boolean} whether an undo operation can be performed
     */
    canUndo() {
      return this._currentEntry?.isLocalChange && !!this._currentEntry.previousEntry;
    }

    /**
     * Reverts the content to the state before the current history entry, if the current entry is a local change.
     *
     * @returns {Object} an object containing:
     *   - content: the new content after we undo the current history entry
     *   - selectionStart: the selection start offset within the new content
     *   - selectionEnd: the selection end offset within the new content
     */
    undo() {
      let selection = this._currentEntry?.redoSelection;
      if (this.canUndo()) {
        this._content = this._apply(this._currentEntry.undoOperations, this._content);
        selection = this._currentEntry.undoSelection;
        this._currentEntry = this._currentEntry.previousEntry;
      }
      return {
        content: this._content,
        selectionStart: selection?.start || 0,
        selectionEnd: selection?.end || 0,
      };
    }

    /**
     * If there is a next entry then it must be the result of an undo operation, so we can redo it.
     *
     * @returns {boolean} whether a redo operation can be performed
     */
    canRedo() {
      return !!this._currentEntry?.nextEntry;
    }

    /**
     * Reapplies the content change from the next history entry.
     *
     * @returns {Object} an object containing:
     *   - content: the new content after we redo the next history entry
     *   - selectionStart: the selection start offset within the new content
     *   - selectionEnd: the selection end offset within the new content
     */
    redo() {
      let selection = this._currentEntry?.redoSelection;
      if (this.canRedo()) {
        this._content = this._apply(this._currentEntry.nextEntry.redoOperations, this._content);
        selection = this._currentEntry.nextEntry.redoSelection;
        this._currentEntry = this._currentEntry.nextEntry;
      }
      return {
        content: this._content,
        selectionStart: selection?.start || 0,
        selectionEnd: selection?.end || 0,
      };
    }

    /**
     * Computes the difference between the old and new content and returns it as a list of operations.
     *
     * @param {string} oldContent the old content
     * @param {string} newContent the new content
     * @returns {Array} the list of operations representing the difference
     */
    _diff(oldContent, newContent) {
      return ChainPad.Diff.diff(oldContent, newContent);
    }

    /**
     * Rebase the next local changes (redo entries), starting from the provided local entry, on top of the given remote
     * change, linking the result to the current history entry. The use case we're trying to address here is when a
     * remote change arrives after the user has undone some of their local changes. We want to allow the user to redo
     * their local changes after the remote change is applied.
     *
     * @param {Object} localEntry the first of the next local changes to rebase
     * @param {*} remoteEntry the remote change to rebase upon
     * @param {*} content the content before both the remote change and the local change are applied
     */
    _rebaseNextLocalChanges(localEntry, remoteEntry, content) {
      let lastEntry = this._currentEntry;
      while (localEntry) {
        const localRedoOps = localEntry.redoOperations;
        const remoteRedoOps = remoteEntry.redoOperations;
        localEntry.redoOperations = this._rebase(localRedoOps, remoteRedoOps, content);
        remoteEntry.redoOperations = this._rebase(remoteRedoOps, localRedoOps, content);
        if (!localEntry.redoOperations || !remoteEntry.redoOperations) {
          // If we can't rebase this entry then we can't rebase any of the following ones.
          lastEntry.nextEntry = null;
          break;
        }

        const contentAfterRemoteChanges = this._apply(remoteRedoOps, content);
        localEntry.undoOperations = this._invert(localEntry.redoOperations, contentAfterRemoteChanges);
        localEntry.undoSelection = this._rebaseSelection(localEntry.undoSelection, remoteRedoOps, content);
        localEntry.redoSelection = this._rebaseSelection(
          localEntry.undoSelection,
          localEntry.redoOperations,
          contentAfterRemoteChanges
        );

        content = this._apply(localRedoOps, content);

        lastEntry.nextEntry = localEntry;
        localEntry.previousEntry = lastEntry;
        lastEntry = localEntry;
        localEntry = localEntry.nextEntry;
      }
    }

    /**
     * Attempts to rebase the most recent local changes on top of the last remote change, so that we can undo/redo them.
     */
    _rebasePreviousLocalChanges() {
      let content = this._content;
      let entry = this._currentEntry;
      while (entry && !entry.isLocalChange && entry.previousEntry?.isLocalChange) {
        entry = entry.previousEntry;
        content = this._rebaseLocalChange(entry, content);
      }
      if (entry) {
        // This is either a remote change or a local change that could not be rebased. In both cases we cannot undo
        // beyond this point so we can drop the previous history entries.
        entry.previousEntry = null;
      }
    }

    /**
     * Attempts to rebase the provided local change on top of the following remote change.
     *
     * @param {Object} localEntry the history entry representing the local change to rebase
     * @param {string} content the content that includes both the local and remote changes
     * @returns {string} the content before the rebased local change is applied (and after the rebased remote change is
     *   applied)
     */
    _rebaseLocalChange(localEntry, content) {
      const localRedoOps = localEntry.redoOperations,
        localUndoOps = localEntry.undoOperations,
        remoteRedoOps = localEntry.nextEntry.redoOperations,
        remoteUndoOps = localEntry.nextEntry.undoOperations,
        contentAfterLocalChange = this._apply(remoteUndoOps, content),
        rebasedRemoteRedoOps = this._rebase(remoteRedoOps, localUndoOps, contentAfterLocalChange);
      if (!rebasedRemoteRedoOps) {
        return contentAfterLocalChange;
      }
      const contentBeforeLocalChange = this._apply(localUndoOps, contentAfterLocalChange),
        rebasedLocalRedoOps = this._rebase(localRedoOps, rebasedRemoteRedoOps, contentBeforeLocalChange);
      if (!rebasedLocalRedoOps) {
        return contentAfterLocalChange;
      }

      // Swap local and remote operations.
      localEntry.redoOperations = rebasedRemoteRedoOps;
      localEntry.undoOperations = this._invert(rebasedRemoteRedoOps, contentBeforeLocalChange);
      localEntry.isLocalChange = false;

      const contentAfterRebasedRemoteChange = this._apply(rebasedRemoteRedoOps, contentBeforeLocalChange);

      localEntry.nextEntry.redoOperations = rebasedLocalRedoOps;
      localEntry.nextEntry.undoOperations = this._invert(rebasedLocalRedoOps, contentAfterRebasedRemoteChange);
      localEntry.nextEntry.isLocalChange = true;

      // The selection before and after both sets of operations are applied doesn't change because if we were able to
      // rebase then the outcome is the same after swapping the operations. We only need to update the intermediate
      // selection (after the rebased remote changes are applied).
      localEntry.redoSelection = this._rebaseSelection(
        localEntry.undoSelection,
        localEntry.redoOperations,
        contentBeforeLocalChange
      );
      localEntry.nextEntry.undoSelection = this._rebaseSelection(
        localEntry.nextEntry.redoSelection,
        localEntry.nextEntry.undoOperations,
        content
      );

      return contentAfterRebasedRemoteChange;
    }

    /**
     * Rebases the given selection on top of the provided list of operations.
     *
     * @param {Object} selection the selection to rebase
     * @param {Array} operations the operations to rebase upon
     * @param {string} content the content before the given operations were applied
     * @returns {Object} the selection after the given operations are applied
     */
    _rebaseSelection(selection, operations, content) {
      const selectionOps = [
        ChainPad.Operation.create(selection.start, 0, ''),
        ChainPad.Operation.create(selection.end, 0, '')
      ];
      // We have to allow empty operations here because the selection is represented using empty operations.
      const rebasedSelectionOps = this._rebase(selectionOps, operations, content, true);
      return {
        start: rebasedSelectionOps?.[0]?.offset || 0,
        end: rebasedSelectionOps?.[1]?.offset || 0
      };
    }

    /**
     * The same content is modified concurrently by two sets of operations (left and right). This method transforms the
     * left operations so that they can be applied on top of (i.e. after) the right operations.
     *
     * @param {Array} left the operations to rebase
     * @param {Array} right the operations to rebase upon
     * @param {string} content the content before both sets of operations were applied (i.e. the content that is
     *   modified concurrently by both sets of operations)
     * @param {boolean} [allowEmptyOperations=false] whether to allow empty operations in the result
     * @returns {Array} the rebased left operations
     */
    _rebase(left, right, content, allowEmptyOperations = false) {
      if (this._canRebaseAll(left, right)) {
        try {
          return this._transform(left, right, content, allowEmptyOperations);
        } catch (error) {
          // We were able to rebase but the outcome doesn't have the expected format (e.g. JSON).
          console.debug('Failed to rebase:', {error, left, right, content});
        }
      }
    }

    /**
     * Checks if two sets of operations can be rebased without merge conflicts.
     *
     * @param {Array} left the operations to rebase
     * @param {*} right the operations to rebase upon
     * @returns {boolean} whether the provided sets of operations can be rebased without merge conflicts
     */
    _canRebaseAll(left, right) {
      for (const leftOperation of left) {
        for (const rightOperation of right) {
          if (!this._canRebase(leftOperation, rightOperation)) {
            return false;
          }
        }
      }
      return true;
    }

    /**
     * Checks if a given operation can be safely rebased upon (applied after) another operation, without changing the
     * user intent. This is true if:
     * - either the deleted areas of both operations don't overlap and don't touch each other
     * - or the deleted areas touch each other but only one operation inserts content or both insert the same content
     *
     * @param {Operation} left the operation to rebase
     * @param {Operation} right the operation to rebase upon
     * @returns {boolean} whether the left operation can be rebased upon the right operation
     */
    _canRebase(left, right) {
      // Either no overlap and no touching,
      return (left.offset > (right.offset + right.toRemove) || right.offset > (left.offset + left.toRemove)) ||
        // or touching but the insertion order is not important.
        ((left.offset === (right.offset + right.toRemove) || right.offset === (left.offset + left.toRemove)) &&
          (left.toInsert.length === 0 || right.toInsert.length === 0 ||
            left.toInsert === right.toInsert));
    }

    /**
     * Applies the provided list of operations to the given content.
     *
     * @param {Array} operations the list of operations to apply
     * @param {string} content the content to apply the operations on
     * @returns {string} the content after applying the provided operations
     */
    _apply(operations, content) {
      return ChainPad.Operation.applyMulti(operations, content);
    }

    /**
     * Inverts the provided list of operations. The resulting operations can be applied to revert the effect of the
     * original operations.
     *
     * @param {Array} operations the list of operations to invert
     * @param {string} content the content after the given operations were applied (i.e. the content the given
     *   operations modify)
     * @returns {Array} the inverted list of operations
     */
    _invert(operations, content) {
      return ChainPad.Patch.invert({operations}, content).operations;
    }
  }

  return History;
});