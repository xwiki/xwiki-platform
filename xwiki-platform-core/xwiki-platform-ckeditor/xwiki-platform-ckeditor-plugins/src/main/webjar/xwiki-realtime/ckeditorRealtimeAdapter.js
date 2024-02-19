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
define('xwiki-ckeditor-realtime-adapter', [
  'xwiki-realtime-wysiwyg-editor'
], function (Editor) {
  'use strict';

  /**
   * Implementation of the {@link Editor} "interface" for CKEditor.
   */
  class CKEditorAdapter extends Editor {
    /**
     * @param {CKEDITOR.editor} ckeditor the CKEditor instance that is being synchronized in real-time
     * @param {CKEDITOR} CKEDITOR the CKEditor API entry point
     * @see https://ckeditor.com/docs/ckeditor4/latest/api/CKEDITOR.html
     * @see https://ckeditor.com/docs/ckeditor4/latest/api/CKEDITOR_editor.html
     */
    constructor(ckeditor, CKEDITOR) {
      super();

      this._ckeditor = ckeditor;
      this._CKEDITOR = CKEDITOR;

      // Disable temporary attachment upload for now.
      if (this._ckeditor.config['xwiki-upload']) {
        this._ckeditor.config['xwiki-upload'].isTemporaryAttachmentSupported = false;
      }

      // Register code to be executed each time the editor content is reloaded.
      // We use a very low priority because we want our listener to be executed after CKEditor's default listeners
      // (e.g. after the CKEditor widgets are initialized).
      const priority = 1000;
      this._ckeditor.on('contentDom', this._onContentLoaded.bind(this), null, null, priority);
      if (this._ckeditor.editable()) {
        // Initial content load.
        this._onContentLoaded();
      }
    }

    /** @inheritdoc */
    getFormFieldName() {
      return this._ckeditor.name;
    }

    /** @inheritdoc */
    getOutputHTML() {
      return this._ckeditor.getData();
    }

    /** @inheritdoc */
    getContentWrapper() {
      return this._ckeditor.editable()?.$;
    }

    /** @inheritdoc */
    getToolBar() {
      return this._ckeditor.ui.space('top').$.querySelector('.cke_toolbox');
    }

    /** @inheritdoc */
    contentUpdated(updatedNodes, propagate) {
      try {
        this._initializeWidgets(updatedNodes);
      } catch (e) {
        console.log("Failed to (re)initialize the widgets.", e);
      }

      // Notify the content change (e.g. to update the empty line placeholders) without triggering our own change
      // handler (see #onChange()).
      this._ckeditor.fire('change', {remote: !propagate});
    }

    /** @inheritdoc */
    onChange(callback) {
      this._ckeditor.on('change', (event) => {
        if (!event.data?.remote) {
          callback();
        }
      });
    }

    /** @inheritdoc */
    getSelection() {
      return this._ckeditor.getSelection()?.getNative();
    }

    /** @inheritdoc */
    saveSelection() {
      this._CKEDITOR.plugins.xwikiSelection.saveSelection(this._ckeditor);
    }

    /** @inheritdoc */
    restoreSelection() {
      this._CKEDITOR.plugins.xwikiSelection.restoreSelection(this._ckeditor);
    }

    /** @inheritdoc */
    convertDataToHtml(data) {
      return this._ckeditor.dataProcessor.toHtml(data);
    }

    /** @inheritdoc */
    showNotification(message, type) {
      this._ckeditor.showNotification(message, type);
    }

    /** @inheritdoc */
    getCustomFilters() {
      // Widget attributes that may have different values for each user (so they can't really be synchronized).
      const ignoredWidgetAttributes = ['data-cke-widget-upcasted', 'data-cke-widget-id', 'data-cke-filter'];

      // Reject the CKEditor drag and resize handlers (for widgets and images).
      const ignoredWidgetHelpers = [
        'cke_widget_drag_handler_container', 'cke_widget_drag_handler', 'cke_image_resizer'
      ];

      return [
        //
        // Widget filter.
        //
        {
          shouldSerializeNode: (node) => !(
            // Reject the hidden (widget) selection and some widget helpers.
            node.nodeType === Node.ELEMENT_NODE &&
            (node.hasAttribute('data-cke-hidden-sel') ||
              ignoredWidgetHelpers.some(className => node.classList.contains(className)))
          ),
          filterHyperJSON: (hjson) => {
            ignoredWidgetAttributes.forEach(attributeName => {
              delete hjson[1][attributeName];
            });
            // Each user may have a different widget selected and/or focused, we don't want to synchronize that.
            if (hjson[1].class) {
              hjson[1].class = hjson[1].class.split(/\s+/).filter(className => ![
                'cke_widget_selected', 'cke_widget_focused', 'cke_widget_editable_focused'
              ].includes(className)).join(' ');
            }
            return hjson;
          }
        },

        //
        // Filling character sequence filter.
        // See https://ckeditor.com/docs/ckeditor4/latest/api/CKEDITOR_dom_selection.html#property-FILLING_CHAR_SEQUENCE
        // See https://bugs.webkit.org/show_bug.cgi?id=15256 (Impossible to place an editable selection inside empty
        // elements)
        //
        {
          // Both shouldSerializeNode and filterHyperJSON are currently called only for elements so in order to filter
          // text nodes we need to filter the direct text child nodes of the element passed to filterHyperJSON.
          filterHyperJSON: (hjson) => {
            const oldChildNodes = hjson[2];
            const newChildNodes = [];
            oldChildNodes.forEach(childNode => {
              if (typeof childNode === 'string') {
                // Remove the filling character sequence from text nodes.
                childNode = childNode.replace(CKEDITOR.dom.selection.FILLING_CHAR_SEQUENCE, '');
                // Ignore text nodes that are used only to allow the user to place the caret inside empty elements.
                if (childNode !== '') {
                  newChildNodes.push(childNode);
                }
              } else {
                newChildNodes.push(childNode);
              }
            });
            hjson[2] = newChildNodes;
            return hjson;
          }
        }
      ];
    }

    /** @inheritdoc */
    onBeforeDestroy(callback) {
      this._ckeditor.on('beforeDestroy', callback);
    }

    _initializeWidgets(updatedNodes) {
      // Reset the focused and selected widgets, as well as the widget holding the focused editable because they may
      // have been invalidated by the DOM changes.
      this._ckeditor.widgets.focused = null;
      this._ckeditor.widgets.selected = [];
      this._ckeditor.widgets.widgetHoldingFocusedEditable = null;

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
            if (nestedWidget) {
              updatedWidgets.add(nestedWidget);
            }
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

      // Update the focused and selected widgets, as well as the widget holding the focused editable, after the
      // selection is restored.
      setTimeout(() => this._ckeditor.widgets.checkSelection(), 0);
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

  return CKEditorAdapter;
});