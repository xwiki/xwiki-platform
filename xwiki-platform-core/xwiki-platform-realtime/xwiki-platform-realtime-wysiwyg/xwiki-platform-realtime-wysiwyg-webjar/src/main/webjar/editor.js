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
define('xwiki-realtime-wysiwyg-editor', [
  'deferred!ckeditor',
], function (ckeditorPromise) {
  'use strict';

  /**
   * The component used to interact with the WYSIWYG editor. The interface is generic but the implementation is
   * currently based on CKEditor.
   */
  class Editor {
    // We can't use private fields currently because neither JSHit nor Closure Compiler support them.
    // See https://github.com/jshint/jshint/issues/3361
    // See https://github.com/google/closure-compiler/issues/2731

    /**
     * @param {CKEDITOR.editor} ckeditor the CKEditor instance that is being synchronized in real-time
     * @param {CKEDITOR} CKEDITOR the CKEditor API entry point
     * @see https://ckeditor.com/docs/ckeditor4/latest/api/CKEDITOR.html
     * @see https://ckeditor.com/docs/ckeditor4/latest/api/CKEDITOR_editor.html
     */
    constructor(ckeditor, CKEDITOR) {
      this._ckeditor = ckeditor;
      this._CKEDITOR = CKEDITOR;

      // Disable temporary attachment upload for now.
      if (this._ckeditor.config['xwiki-upload']) {
        this._ckeditor.config['xwiki-upload'].isTemporaryAttachmentSupported = false;
      }

      // Register code to be executed each time the editor content is reloaded.
      this.onContentLoaded(this._onContentLoaded.bind(this));
      if (this._ckeditor.editable()) {
        // Initial content load.
        this._onContentLoaded();
      }
    }

    /**
     * Wait for the specified editor to be ready.
     *
     * @param {string} name the editor name, usually the key used to submit the editor content
     * @returns {Promise<Editor>} a promise that will be resolved with the editor instance when the editor is ready
     */
    static async waitForInstance(name) {
      name = name || 'content';
      const CKEDITOR = await ckeditorPromise;
      const ckeditor = CKEDITOR.instances[name];
      if (ckeditor) {
        const editor = new Editor(ckeditor, CKEDITOR);
        if (ckeditor.status === 'ready') {
          return editor;
        } else {
          return new Promise(resolve => ckeditor.on('instanceReady', resolve.bind(null, editor)));
        }
      } else {
        return new Promise(resolve => CKEDITOR.on('instanceReady', function (event) {
          if (event.editor.name === name) {
            resolve(new Editor(event.editor, CKEDITOR));
          }
        }));
      }
    }

    /**
     * @returns {Element} the DOM element containing the editor content (i.e. the element that defines the editable area
     *   of the editor)
     */
    getContent() {
      return this._ckeditor.editable()?.$;
    }

    /**
     * Notify the editor that its content has been updated as a result of a remote change.
     *
     * @param {Node[]} updatedNodes the DOM nodes that have been updated (added, modified directly or with removed
     *   descendants)
     */
    contentUpdated(updatedNodes) {
      try {
        this._initializeWidgets(updatedNodes);
      } catch (e) {
        console.log("Failed to (re)initialize the widgets.", e);
      }

      // Notify the content change (e.g. to update the empty line placeholders) without triggering our own change
      // handler (see #onChange()).
      this._ckeditor.fire('change', {remote: true});
    }

    /**
     * Adds a callback to be called whenever the editor content changes as a result of user interaction.
     *
     * @param {Function} callback the function to call when the editor content changes
     */
    onChange(callback) {
      this._ckeditor.on('change', (event) => {
        if (!event.data?.remote) {
          callback();
        }
      });
    }

    /**
     * Adds a callback to be called whenever the editor content is loaded. For in-place editors this is called only
     * once, when the editor is loaded. For iframe-based editors this is called each time the iframe is reloaded, which
     * happens for instance when a macro is inserted.
     *
     * @param {Function} callback the function to call when the editor content is loaded
     */
    onContentLoaded(callback) {
      // We use a very low priority because we want our listener to be executed after CKEditor's default listeners
      // (e.g. after the CKEditor widgets are initialized).
      const priority = 1000;
      this._ckeditor.on('contentDom', callback, null, null, priority);
    }

    /**
     * @returns {Selection} the current DOM selection in the editor
     * @see https://developer.mozilla.org/en-US/docs/Web/API/Selection
     */
    getSelection() {
      return this._ckeditor.getSelection()?.getNative();
    }

    /**
     * Save the current selection so that it can be restored later, usually after a DOM change.
     */
    saveSelection() {
      this._CKEDITOR.plugins.xwikiSelection.saveSelection(this._ckeditor);
    }

    /**
     * Restore the selection saved previously.
     */
    restoreSelection() {
      this._CKEDITOR.plugins.xwikiSelection.restoreSelection(this._ckeditor);
    }

    _initializeWidgets(updatedNodes) {
      // Save the focused and selected widgets, as well as the widget holding the focused editable, so that we can
      // restore them after (re)initializing the widgets (if possible).
      const focusedWidgetWrapper = this._ckeditor.widgets.focused?.wrapper;
      const selectedWidgetWrappers = this._ckeditor.widgets.selected.map(widget => widget.wrapper);
      const widgetHoldingFocusedEditableWrapper = this._ckeditor.widgets.widgetHoldingFocusedEditable?.wrapper;

      // Find the widgets that need to be reinitialized because some of their content was updated.
      const updatedWidgets = new Set();
      updatedNodes.forEach(updatedNode => {
        if (updatedNode.nodeType === Node.ATTRIBUTE_NODE) {
          // For attribute nodes we consider the owner element was updated.
          updatedNode = updatedNode.ownerElement;
        } else if (updatedNode.nodeType !== Node.ELEMENT_NODE) {
          // The updated node is a text or comment, most probably, so it doesn't affect the widget.
          return;
        }
        const updatedWidget = this._ckeditor.widgets.getByElement(new this._CKEDITOR.dom.element(updatedNode));
        if (updatedWidget) {
          updatedWidgets.add(updatedWidget);
          // We also have to reinitialize the nested widgets.
          updatedWidget.wrapper.find('.cke_widget_wrapper').toArray().forEach(nestedWidgetWrapper => {
            const nestedWidget = this._ckeditor.widgets.getByElement(nestedWidgetWrapper, true);
            updatedWidgets.add(nestedWidget);
          });
        }
      });

      // Delete the updated widgets so that we can reinitialize them.
      updatedWidgets.forEach(widget => {
        delete this._ckeditor.widgets.instances[widget.id];
      });

      // Remove the widgets whose element was removed from the DOM and add widgets to match the widget elements found in
      // the DOM.
      this._ckeditor.widgets.checkWidgets();

      // Update the focused and selected widgets, as well as the widget holding the focused editable.
      if (focusedWidgetWrapper) {
        const focusedWidget = this._ckeditor.widgets.getByElement(focusedWidgetWrapper, true);
        this._ckeditor.widgets.focused = focusedWidget;
      }
      this._ckeditor.widgets.selected = selectedWidgetWrappers.map(widgetWrapper =>
        this._ckeditor.widgets.getByElement(widgetWrapper, true)
      ).filter(widget => !!widget);
      if (widgetHoldingFocusedEditableWrapper) {
        const widgetHoldingFocusedEditable = this._ckeditor.widgets.getByElement(widgetHoldingFocusedEditableWrapper,
          true);
        this._ckeditor.widgets.widgetHoldingFocusedEditable = widgetHoldingFocusedEditable;
      }
    }

    _onContentLoaded() {
      this._fixMagicLine();
    }

    _fixMagicLine() {
      // Make sure the magic line is not synchronized between editors.
      const magicLine = this._ckeditor._.magiclineBackdoor?.that?.line?.$;
      if (magicLine) {
        [magicLine, magicLine.parentElement].forEach(function (element) {
          element.setAttribute('class', 'rt-non-realtime');
        });
      }
    }
  }

  return Editor;
});