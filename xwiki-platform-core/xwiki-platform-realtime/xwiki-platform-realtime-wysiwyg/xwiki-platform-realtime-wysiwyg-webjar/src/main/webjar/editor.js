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
     */
    contentUpdated() {
      try {
        this._initializeWidgets();
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

    _initializeWidgets() {
      const dataValues = {};
      const widgetElements = this.getContent().querySelectorAll('[data-cke-widget-data]');
      widgetElements.forEach((widgetElement, index) => {
        dataValues[index] = widgetElement.getAttribute('data-cke-widget-data');
      });
      this._ckeditor.widgets.checkWidgets();
      widgetElements.forEach((widgetElement, index) => {
        widgetElement.setAttribute('data-cke-widget-data', dataValues[index]);
      });
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