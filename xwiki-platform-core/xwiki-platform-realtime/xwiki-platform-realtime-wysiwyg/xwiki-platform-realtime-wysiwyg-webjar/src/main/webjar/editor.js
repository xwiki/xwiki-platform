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
define('xwiki-realtime-wysiwyg-editor', [], function () {
  'use strict';

  /**
   * The component used to interact with the WYSIWYG editor.
   */
  class Editor {
    // We can't use private fields currently because neither JSHit nor Closure Compiler support them.
    // See https://github.com/jshint/jshint/issues/3361
    // See https://github.com/google/closure-compiler/issues/2731

    /**
     * @returns {string} the name of the form field that the editor is editing
     */
    getFormFieldName() {
      throw new Error('Not implemented!');
    }

    /**
     * @returns {string} the HTML content produced by the editor, that can be submitted to the server to be converted to
     *   wiki syntax
     */
    getOutputHTML() {
      throw new Error('Not implemented!');
    }

    /**
     * @returns {Element} the DOM element containing the editor content (i.e. the element that defines the editable area
     *   of the editor)
     */
    getContentWrapper() {
      throw new Error('Not implemented!');
    }

    /**
     * @returns {Element} the DOM element that represents the toolbar of the editor
     */
    getToolBar() {
      throw new Error('Not implemented!');
    }

    /**
     * Notify the editor that its content has been updated as a result of a remote change.
     *
     * @param {Node[]} updatedNodes the DOM nodes that have been updated (added, modified directly or with removed
     *   descendants)
     *
     * @param {boolean} propagate true when the new content should be propagated to coeditors
     * @returns {Promise} a promise that resolves when the editor has finished handling the content update (some
     *   changes, like for instance if you modify some macro parameters, might require a full refresh of the edited
     *   content).
     */
    async contentUpdated(updatedNodes, propagate) {
      throw new Error('Not implemented!');
    }

    /**
     * Adds a callback to be called whenever the editor content changes as a result of user interaction (local change).
     *
     * @param {Function} callback the function to call when the editor content changes
     */
    onChange(callback) {
      throw new Error('Not implemented!');
    }

    /**
     * @returns {Selection} the current DOM selection in the editor
     * @see https://developer.mozilla.org/en-US/docs/Web/API/Selection
     */
    getSelection() {
      throw new Error('Not implemented!');
    }

    /**
     * Save the current selection so that it can be restored later, usually after a DOM change.
     */
    saveSelection() {
      throw new Error('Not implemented!');
    }

    /**
     * Restore the selection saved previously.
     */
    restoreSelection() {
      throw new Error('Not implemented!');
    }

    /**
     * Converts input data accepted by the editor to html that can be directly inserted in the editor's DOM.
     *
     * @param {string} data the data to be converted
     * @returns {string} html representation of the input data that can be inserted in the editor's DOM.
     */
    convertDataToHTML(data) {
      throw new Error('Not implemented!');
    }

    /**
     * Shows a notification message inside the editor.
     * 
     * @param {string} message the notification message to show
     * @param {string} type the type of notification (e.g. 'info', 'warning', 'error')
     */
    showNotification(message, type) {
      throw new Error('Not implemented!');
    }

    /**
     * return {Array<Object>} an array of HyperJSON filters specific to this editor implementation
     */
    getCustomFilters() {
      return [];
    }

    /**
     * Adds a callback to be called before the editor is destroyed. This is useful for instance to disconnect from the
     * realtime session. It is especially important for the in-place editor where the user can enter and leave the edit
     * mode multiple times without reloading the web page, so resources and connections should be properly released when
     * the editor is destroyed.
     *
     * @param {Function} callback the function to call before the editor is destroyed
     */
    onBeforeDestroy(callback) {
      throw new Error('Not implemented!');
    }

    /**
     * Adds a callback to be called before the editor is locked.
     *
     * @param {Function} callback the function to call before the editor is locked
     */
    onLock(callback) {
      throw new Error('Not implemented!');
    }

    /**
     * Adds a callback to be called after the editor is unlocked.
     *
     * @param {Function} callback the function to call after the editor is unlocked
     */
    onUnlock(callback) {
      throw new Error('Not implemented!');
    }

    /**
     * @param {boolean} readOnly {@code true} if the editor should be set to read-only mode, false otherwise
     */
    setReadOnly(readOnly) {
      throw new Error('Not implemented!');
    }
  }

  return Editor;
});
