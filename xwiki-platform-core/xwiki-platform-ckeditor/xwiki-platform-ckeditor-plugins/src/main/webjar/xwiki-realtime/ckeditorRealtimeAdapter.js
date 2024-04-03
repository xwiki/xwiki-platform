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

      // Realtime synchronization must be paused while the editor is locked.
      this._lockCallbacks = [];
      this._ckeditor.on('startLoading', () => {
        if (this._ckeditor.mode === 'wysiwyg') {
          this._lockCallbacks.forEach(callback => callback());
        }
      });
      this._unlockCallbacks = [];
      this._ckeditor.on('endLoading', () => {
        if (this._ckeditor.mode === 'wysiwyg' && this._ckeditor.editable()) {
          this._unlockCallbacks.forEach(callback => callback());
        }
      });
    }

    /** @inheritdoc */
    getFormFieldName() {
      return this._ckeditor.name;
    }

    /** @inheritdoc */
    getOutputHTML() {
      const fullPage = this._ckeditor.config.fullPage;
      try {
        // We're interested only in the edited content (what's inside the content wrapper). Moreover, the returned HTML
        // should be independent of the editor type (iframe-based on in-place).
        this._ckeditor.config.fullPage = false;
        return this._ckeditor.getData();
      } finally {
        // Restore the configuration value.
        this._ckeditor.config.fullPage = fullPage;
      }
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
    async contentUpdated(updatedNodes, propagate) {
      try {
        await this._updateWidgets(updatedNodes);
      } catch (e) {
        console.error("Failed to (re)initialize the widgets.", e);
      }

      // Notify the content change (e.g. to update the empty line placeholders) without triggering our own change
      // handler (see #onChange()).
      this._ckeditor.fire('change', {remote: !propagate});
    }

    /** @inheritdoc */
    onChange(callback) {
      this._ckeditor.on('change', (event) => {
        if (!event.data?.remote && !this._ckeditor.readOnly) {
          callback();
        }
      });
    }

    /** @inheritdoc */
    getSelection() {
      return this._ckeditor.getSelection(true)?.getNative();
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
    parseInputHTML(html) {
      const fixedHTML = this._ckeditor.dataProcessor.toHtml(html);

      let doc;
      try {
        doc = new DOMParser().parseFromString(fixedHTML, 'text/html');
      } catch (e) {
        console.error('Failed to parse the given HTML string: ' + html, e);
        return;
      }

      this._initializeWidgets(doc.body);

      return doc.body;
    }

    _initializeWidgets(contentWrapper) {
      const nextWidgetId = this._ckeditor.widgets._.nextId;
      const widgets = this._ckeditor.widgets.instances;
      try {
        this._ckeditor.widgets._.nextId = 0;
        this._ckeditor.widgets.instances = {};
        contentWrapper.querySelectorAll('.macro[data-macro]').forEach(macroElement => {
          macroElement.dataset.xwikiDomUpdated = 'true';
        });
        return this._ckeditor.widgets.initOnAll(new this._CKEDITOR.dom.element(contentWrapper));
      } finally {
        this._ckeditor.widgets._.nextId = nextWidgetId;
        this._ckeditor.widgets.instances = widgets;
      }
    }

    /** @inheritdoc */
    showNotification(message, type) {
      this._ckeditor.showNotification(message, type);
    }

    /** @inheritdoc */
    onBeforeDestroy(callback) {
      this._ckeditor.on('beforeDestroy', callback);
    }

    async _updateWidgets(updatedNodes) {
      // Reset the focused and selected widgets, as well as the widget holding the focused editable because they may
      // have been invalidated by the DOM changes.
      this._ckeditor.widgets.focused = null;
      this._ckeditor.widgets.selected = [];
      this._ckeditor.widgets.widgetHoldingFocusedEditable = null;

      // Find the widgets that need to be reinitialized because some of their content was updated.
      const updatedWidgets = new Set();
      // Also check if there where any macro parameters updated, which would require a full content refresh in order to
      // re-render the macros with the new parameter values (because macro output is not synchronized, for security
      // reasons).
      let shouldRefreshContent = false;
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
        // The macro parametes are kept on the macro wrapper, so if the macro wrapper was updated then it's very likely
        // that the macro parameters were also updated.
        shouldRefreshContent = shouldRefreshContent || this._isMacroWrapper(updatedNode);
      });

      // Delete the updated widgets so that we can reinitialize them.
      updatedWidgets.forEach(widget => {
        delete this._ckeditor.widgets.instances[widget.id];
      });

      // Remove the widgets whose element was removed from the DOM and add widgets to match the widget elements found in
      // the DOM.
      this._ckeditor.widgets.checkWidgets();

      if (shouldRefreshContent) {
        await this._refreshContent();
      }

      // Update the focused and selected widgets, as well as the widget holding the focused editable, after the
      // selection is restored.
      setTimeout(() => this._ckeditor.widgets.checkSelection(), 0);
    }

    _isMacroWrapper(element) {
      return element.matches('.macro[data-macro], .cke_widget_wrapper.cke_widget_xwiki-macro');
    }

    async _refreshContent() {
      // Refresh the content to re-render the macros. Don't preserve the selection because this is done by the realtime
      // framework when applying remove changes.
      this._ckeditor.execCommand('xwiki-refresh', {preserveSelection: false});

      return new Promise(resolve => {
        const macroPlugin = this._ckeditor.plugins['xwiki-macro'];
        if (macroPlugin) {
          macroPlugin.onceAfterRefresh(this._ckeditor, resolve);
        } else {
          resolve();
        }
      });
    }

    /** @inheritdoc */
    onLock(callback) {
      this._lockCallbacks.push(callback);
    }

    /** @inheritdoc */
    onUnlock(callback) {
      this._unlockCallbacks.push(callback);
    }

    /** @inheritdoc */
    setReadOnly(readOnly) {
      this._ckeditor.setReadOnly(readOnly);
    }
  }

  return CKEditorAdapter;
});