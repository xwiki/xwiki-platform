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
     * Update the edited content as a result of a remote change.
     *
     * @param {Function} updater a function that takes the current content of the editor, modifies it and returns the
     *   updated nodes
     * @param {boolean} propagate true when the new content should be propagated to coeditors
     * @returns {Promise} a promise that resolves when the editor has finished handling the content update (some
     *   changes, like for instance if you modify some macro parameters, might require a full refresh of the edited
     *   content).
     */
    async updateContent(updater, propagate) {
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
     * Note that the editor might not be focused so the returned selection ranges don't always match the current window
     * selection. The returned ranges correspond to the selection (e.g. the caret position) that the editor remembers
     * even when it loses focus (e.g. when a dialog is opened).
     *
     * @returns {Array[Range]} the currently selected DOM ranges in the editor, {@code []} if there is no selection
     * @see https://developer.mozilla.org/en-US/docs/Web/API/Range
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
     * Restore the given DOM selection ranges, or the last saved text selection if no ranges are specified.
     *
     * @param {Range[]} ranges the DOM selection ranges to restore; if not specified, the last saved text selection is
     *   restored
     */
    restoreSelection(ranges) {
      throw new Error('Not implemented!');
    }

    /**
     * Simulates the loading of the given HTML in the editor without affecting the content that is currently being
     * edited. The given HTML is parsed into a DOM representation and filtered as if it were to be edited in the editor.
     * The returned element is similar to calling {@link #getContentWrapper()} after loading the given HTML in the
     * editor.
     *
     * @param {string} html the input HTML to be parsed; this should come either from {@link #getOutputHTML()} or from
     *   rendering wiki syntax to Annotated HTML
     * @returns {Element} the DOM representation of the given HTML, with some adjustments to match what you would get
     *   if you were to load the given HTML directly in the editor; see also {@link #getContentWrapper()}
     */
    parseInputHTML(html) {
      throw new Error('Not implemented!');
    }

    /**
     * Allows the editor to ignore some of the changes found when comparing the remote content with the local content.
     * Each filter function taks a DOM change as input and returns a boolean indicating whether that change should be
     * ignored or not.
     *
     * @returns {Array[Function]} an array of functions that must be used to filter the DOM changes before applying a
     *   patch (due to a remote change)
     */
    getFilters() {
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
