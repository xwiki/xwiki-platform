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
define('xwiki-realtime-wysiwygEditor-patches', [
  'xwiki-realtime-wysiwygEditor-filters',
  'hyper-json',
  'diff-dom',
  'json.sortify'
], function(Filters, HyperJSON, DiffDOM, JSONSortify) {
    'use strict';

    // The caret / selection is normally either inside a text node or between elements. So it can't be affected when
    // attributes, comments or element properties (like value, checked, selected) are changed. The selection is also
    // not affected when DOM nodes are added because the browser is able to update the offsets automatically. We only
    // need to update the selection when the selection end points are inside (text or element) nodes that are removed,
    // moved or modified.
    const changesAffectingSelection = ['modifyTextElement', 'removeTextElement', 'replaceElement',
      'removeElement', 'relocateGroup'];

    class Patches {
      // We can't use private fields currently because neither JSHit nor Closure Compiler support them.
      // See https://github.com/jshint/jshint/issues/3361
      // See https://github.com/google/closure-compiler/issues/2731

      /**
       * @param {CKEditor} editor the CKEditor instance we want to patch
       */
      constructor(editor) {
        this._editor = editor;

        this.diffDOM = new DiffDOM.DiffDOM({
          preDiffApply: (change) => {
            // Reject the changes we don't want to apply.
            if (Filters.shouldRejectChange(change)) {
              return true;
            }

            // Determine whether the selection is affected by the change.
            if (!this._shouldRestoreSelection && changesAffectingSelection.includes(change.diff.action)) {
              const selection = this._editor.getSelection()?.getNative();
              const range = selection?.rangeCount && selection?.getRangeAt(0);
              this._shouldRestoreSelection = change.node?.contains(range?.startContainer) ||
                change.node?.contains(range?.endContainer);
            }
          }
        });
      }

      /**
       * @param {Node} node the DOM node to serialize as HyperJSON
       * @returns {string} the serialization of the given DOM node as HyperJSON
       */
      static _stringifyNode(node) {
        return JSONSortify(HyperJSON.fromDOM(node, Filters.shouldSerializeNode.bind(Filters),
          Filters.filterHyperJSON.bind(Filters)));
      }

      /**
       * @returns {string} the serialization of the editor content as HyperJSON
       */
      getHyperJSON() {
        return Patches._stringifyNode(this._getEditableContent());
      }

      /**
       * Update the editor content without affecting its caret / selection.
       * 
       * @param {string} shjson the new content, serialized as HyperJSON.
       */
      setHyperJSON(shjson) {
        let newContent;
        try {
          newContent = HyperJSON.toDOM(JSON.parse(shjson));
        } catch (e) {
          console.error('Failed to parse the given HyperJSON string: ' + shjson, e);
          return;
        }

        this._updateContent(newContent);
      }

      /**
       * Update the editor content without affecting its caret / selection.
       * 
       * @param {string} html the new HTML content
       */
      setHTML(html) {
        let doc;
        try {
          doc = new DOMParser().parseFromString(html, 'text/html');
        } catch (e) {
          console.error('Failed to parse the given HTML string: ' + html, e);
          return;
        }

        this._updateContent(doc.body);
      }

      /**
       * Update the editor content without affecting its caret / selection.
       * 
       * @param {Node} newContent the new content to set, as a DOM node
       */
      _updateContent(newContent) {
        // Remember where the selection is, to be able to restore it in case the content update affects it.
        CKEDITOR.plugins.xwikiSelection.saveSelection(this._editor);

        const oldContent = this._getEditableContent();

        // We have to call nodeToObj ourselves because the compared DOM elements are from different documents.
        const patch = this.diffDOM.diff(DiffDOM.nodeToObj(oldContent), DiffDOM.nodeToObj(newContent));
        this._shouldRestoreSelection = false;
        this.diffDOM.apply(oldContent, patch, {
          document: oldContent.ownerDocument
        });

        try {
          this._initializeWidgets();
        } catch (e) {
          console.log("Failed to (re)initialize the widgets.", e);
        }

        // Restore the selection if it was affected by the content update.
        if (this._shouldRestoreSelection) {
          CKEDITOR.plugins.xwikiSelection.restoreSelection(this._editor);
        }
      }

      /**
       * @returns {Element} the element that defines the editable area of the editor
       */
      _getEditableContent() {
        return this._editor.editable()?.$;
      }

      _initializeWidgets() {
        const dataValues = {};
        const widgetElements = this._getEditableContent().querySelectorAll('[data-cke-widget-data]');
        widgetElements.forEach((widgetElement, index) => {
          dataValues[index] = widgetElement.getAttribute('data-cke-widget-data');
        });
        this._editor.widgets.checkWidgets();
        widgetElements.forEach((widgetElement, index) => {
          widgetElement.setAttribute('data-cke-widget-data', dataValues[index]);
        });
      }
    }

    return Patches;
});