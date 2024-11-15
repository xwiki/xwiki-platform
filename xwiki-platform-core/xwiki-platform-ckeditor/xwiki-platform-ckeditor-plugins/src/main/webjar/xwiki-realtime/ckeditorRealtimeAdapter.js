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
  'xwiki-realtime-wysiwyg-editor',
  'json.sortify'
], function (Editor, JSONSortify) {
  'use strict';

  // The attribute used to mark the empty elements that hold the caret of the current user or some remote user. We need
  // to mark these elements in order to prevent CKEditor from removing them from the output HTML that is synchronized
  // between editors.
  const EMPTY_CARET_HOLDER = 'data-xwiki-realtime-empty-caret';

  // The attribute used by CKEditor to mark elements that must not be removed when filtering the input and output HTML.
  const CKE_SURVIVE = 'data-cke-survive';

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
      this._widgetCache = {};

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

      this._protectEmptyElementsHoldingCaret();
    }

    /** @inheritdoc */
    getFormFieldName() {
      return this._ckeditor.name;
    }

    /** @inheritdoc */
    getOutputHTML() {
      const fullPage = this._ckeditor.config.fullPage;
      const includeUploadWidgetsInOutputHTML = this._ckeditor.config._includeUploadWidgetsInOutputHTML;
      const includeEmptyElementsHoldingCaret = this._ckeditor.config._includeEmptyElementsHoldingCaret;
      try {
        // We're interested only in the edited content (what's inside the content wrapper). Moreover, the returned HTML
        // should be independent of the editor type (iframe-based on in-place).
        this._ckeditor.config.fullPage = false;
        // We have to include the upload widgets in the output HTML (that gets synchronized with remote users),
        // otherwise they are lost when a remote change is applied. We can't simply ignore the diff changes that remove
        // the upload widgets because it leads to conflicts when applying remote patches (the DOM structure is
        // different: e.g. instead of <text><uploadWidget><text> we end up having only <text>, i.e. the adjacent text
        // nodes are merged together).
        this._ckeditor.config._includeUploadWidgetsInOutputHTML = true;
        // See _protectEmptyElementsHoldingCaret()
        this._ckeditor.config._includeEmptyElementsHoldingCaret = true;
        return this._ckeditor.getData();
      } finally {
        // Restore the configuration value.
        this._ckeditor.config.fullPage = fullPage;
        this._ckeditor.config._includeUploadWidgetsInOutputHTML = includeUploadWidgetsInOutputHTML;
        this._ckeditor.config._includeEmptyElementsHoldingCaret = includeEmptyElementsHoldingCaret;
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
    async updateContent(updater, isLocalChange) {
      // Don't push local changes when saving the snapshot. Wait until the content has been updated (see below),
      // otherwise we risk reverting the received remote changes.
      this._isRemoteChange = true;
      // Save the editor state before updating the content in order to be able to undo the remote changes.
      this._ckeditor.fire('saveSnapshot');
      // Prevent the editor from recording Undo/Redo history entries while the edited content is being refreshed:
      // * if a macro is inserted then we need to wait for the macro markers to be replaced by the actual macro output
      // * if a macro is updated then we need to wait for the macro output to be updated to match the new macro data
      this._ckeditor.fire('lockSnapshot', {dontUpdate: true});

      let updatedNodes = [];
      try {
        this._CKEDITOR.plugins.xwikiSelection.saveSelection(this._ckeditor);
        this._protectWidgets(this._ckeditor.widgets.instances);
        updatedNodes = updater.patchContent(this.getContentWrapper());
      } finally {
        await this._updateWidgetsAndRestoreSelection(updater, updatedNodes);

        // Push the updated content to remote users, when saving the snapshot, if this is a local change.
        // See #onChange() below.
        this._isRemoteChange = !isLocalChange;
        this._ckeditor.fire('unlockSnapshot');
        // Save the editor state after remote changes have been applied, in order to be able to revert it. This also
        // triggers the 'change' event allowing CKEditor plugins to react to the content change (e.g. to update the
        // empty line placeholders).
        this._ckeditor.fire('saveSnapshot');
        delete this._isRemoteChange;
      }
    }

    async _updateWidgetsAndRestoreSelection(updater, updatedNodes) {
      const shouldRefreshContent = this._updateWidgets(updatedNodes);

      const ranges = updater.restoreSelection(this.getContentWrapper());
      if (ranges.length) {
        // Restore the selection that was transformed using the received remote patches.
        console.debug('Restore transformed selection');
        this._CKEDITOR.plugins.xwikiSelection.restoreSelection(this._ckeditor, ranges);
      }

      if (shouldRefreshContent) {
        // Preserve the selection if it was restored above, after the content was updated. Otherwise we'll take care
        // of restoring the selection after the content is refreshed (see below).
        await this._refreshContent(ranges.length > 0);
      }

      if (!ranges.length) {
        // Restore the selection using the saved text-based selection because there were no ranges transformed by the
        // remote patches. This can happen for instance when the selection starts or ends inside the read-only part of
        // a widget (e.g. the macro output) which is not synchronized between editors.
        console.debug('Restore saved text selection');
        this._CKEDITOR.plugins.xwikiSelection.restoreSelection(this._ckeditor);
      }
    }

    /** @inheritdoc */
    onChange(callback) {
      this._ckeditor.on('change', () => {
        if (!this._isRemoteChange && !this._ckeditor.readOnly) {
          callback();
        }
      });
    }

    /** @inheritdoc */
    getSelection() {
      return this._CKEDITOR.plugins.xwikiSelection.getSelection(this._ckeditor);
    }

    /** @inheritdoc */
    parseInputHTML(html) {
      const fixedHTML = this._ckeditor.dataProcessor.toHtml(html);
      const doc = new DOMParser().parseFromString(fixedHTML, 'text/html');
      const widgets = this._initializeWidgets(doc.body);
      this._protectWidgets(widgets);
      return this._ensureSameContentWrapper(doc.body);
    }

    /** @inheritdoc */
    getFilters() {
      return [
        // Ignore widget id changes.
        change => change.diff.action === 'modifyAttribute' && change.diff.name === 'data-widget-id' &&
          change.node.classList?.contains('xwiki-widget')
      ];
    }

    /** @inheritdoc */
    filterInputContent(contentWrapper) {
      // Make sure the empty elements holding the caret for remote users are not removed when the content is parsed (and
      // filtered) by CKEditor. They are removed by default because they can't be edited by the current user, but in our
      // case it's not the current user that is editing them but the remote user that has the caret in that position.
      // See _protectEmptyElementsHoldingCaret()
      contentWrapper.querySelectorAll(`[${EMPTY_CARET_HOLDER}]:empty`).forEach(emptyCaretHolder => {
        emptyCaretHolder.setAttribute(CKE_SURVIVE, '1');
      });
    }

    /** @inheritdoc */
    onBeforeDestroy(callback) {
      this._ckeditor.on('beforeDestroy', callback);
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

    _ensureSameContentWrapper(root) {
      const contentWrapperClone = this.getContentWrapper().cloneNode();
      contentWrapperClone.append(...root.childNodes);
      return contentWrapperClone;
    }

    _initializeWidgets(contentWrapper) {
      const widgets = this._ckeditor.widgets.instances;
      try {
        this._ckeditor.widgets.instances = {};

        // Optimization: the macro output is ignored when comparing the remote content with the local content, so it
        // doesn't have to be fully initialized (enhanced) by dedicated JavaScript code.
        contentWrapper.querySelectorAll('.macro[data-macro]').forEach(macroElement => {
          macroElement.dataset.xwikiDomUpdated = 'true';
        });

        this._ckeditor.widgets.initOnAll(new this._CKEDITOR.dom.element(contentWrapper));

        // Remove the useless SPAN wrapper around the image widgets, that is normally not present when the content is
        // attached to the editor, but we have it here for the remote content, which is not attached.
        Object.values(this._ckeditor.widgets.instances).forEach(widget => {
          if (widget.name === 'image') {
            const widgetWrapper = widget.wrapper.$;
            if (!widgetWrapper.previousSibling && !widgetWrapper.nextSibling &&
                widgetWrapper.parentNode.tagName.toLowerCase() === 'span' &&
                !widgetWrapper.parentNode.attributes.length) {
              widgetWrapper.parentNode.replaceWith(widgetWrapper);
            }
          }
        });

        return this._ckeditor.widgets.instances;
      } finally {
        this._ckeditor.widgets.instances = widgets;
      }
    }

    _protectWidgets(widgets) {
      // Cache the widgets so that we can restore them after the content is updated.
      Object.assign(this._widgetCache, widgets);
      Object.values(widgets).forEach(widget => {
        const widgetWrapper = widget.wrapper.$;
        // We put the widget name in the placeholder tag name in order to avoid "merging" widgets of different types.
        // Each widget type has its own HTML structure that can't be recreated in a generic way using only the widget
        // name and data. We can only update an existing widget in a generic way if its type doesn't change.
        const widgetPlaceholder = widgetWrapper.ownerDocument.createElement('xwiki-widget-' + widget.name);
        widgetPlaceholder.classList.add('xwiki-widget');
        widgetPlaceholder.setAttribute('value', this._serializeWidgetData(widget));
        widgetPlaceholder.dataset.widgetId = widget.id;
        widgetWrapper.replaceWith(widgetPlaceholder);

        Object.entries(widget.editables).forEach(([name, editable]) => {
          const editablePlaceholder = widgetPlaceholder.ownerDocument.createElement('xwiki-editable');
          editablePlaceholder.setAttribute('name', name);
          if (editable.$.dataset.xwikiNonGeneratedContent) {
            editablePlaceholder.dataset.contentType = editable.$.dataset.xwikiNonGeneratedContent;
          }
          widgetPlaceholder.append(editablePlaceholder);
          editablePlaceholder.append(...editable.$.childNodes);
        });
      });
    }

    _restoreWidgets() {
      this.getContentWrapper().querySelectorAll('.xwiki-widget').forEach(widgetPlaceholder => {
        const widget = this._widgetCache[widgetPlaceholder.dataset.widgetId];
        if (widget) {
          // Restore the widget wrapper.
          widgetPlaceholder.replaceWith(widget.wrapper.$);
          // Update the widget data (which may have been modified as a result of a remote change). Note that this may
          // add or remove widget editables (e.g. when the image widget data is modified to enable the image caption the
          // caption editable is added).
          let widgetData = widgetPlaceholder.getAttribute('value');
          // Widget#setData() checks if the data has changed before firing the data event, but unfortunately the code
          // expects the data values to be primitives, which is not the case for macro widget data where the parameters
          // data is an object and this makes the check always return false. The problem with this is that when the data
          // event is fired the scroll postion is updated, making it hard to scroll the content while remote changes are
          // applied (the scroll bar jumps). So we have to check ourselves if the data has changed.
          if (widgetData !== this._serializeWidgetData(widget)) {
            widgetData = JSON.parse(widgetData);
            widget.setData(widgetData);
          }

          // Restore the content of the widget editables.
          widgetPlaceholder.querySelectorAll(':scope > xwiki-editable').forEach(editablePlaceholder => {
            const editableName = editablePlaceholder.getAttribute('name');
            let editable = widget.editables[editableName]?.$;
            if (!editable && widget.name === 'xwiki-macro') {
              // Macro widgets are rendered server-side so setting the widget data above doesn't create the editables.
              // We need create the editables ourselves under the widget element so that they get submitted to the
              // server when the content is refreshed (in order to be taken into account when rendering the macros).
              editable = widgetPlaceholder.ownerDocument.createElement('div');
              editable.classList.add('xwiki-metadata-container');
              editable.dataset.xwikiNonGeneratedContent = editablePlaceholder.dataset.contentType;
              editable.dataset.xwikiParameterName = editableName;
              widget.element.$.append(editable);
              widget.initEditable(editableName, {
                selector: `[data-xwiki-parameter-name="${CSS.escape(editableName)}"]`
              });
            }
            if (editable) {
              // The editable should be empty, unless it was just initialized above in which case it may contain an
              // empty paragraph that we want to get rid of (we want only the content from the editable placeholder).
              editable.innerHTML = '';
              editable.append(...editablePlaceholder.childNodes);
            }
          });
        }
      });

      // Update the widget instances to include the restored widgets, and destroy the detached widgets.
      Object.values(this._widgetCache).forEach(widget => {
        if (widget.wrapper.isDetached()) {
          // We no longer need this widget.
          delete this._ckeditor.widgets.instances[widget.id];
          widget.destroy(true);
        } else {
          // This widget was restored.
          this._ckeditor.widgets.instances[widget.id] = widget;
        }
      });

      // We only need to cache the widgets until they are restored.
      this._widgetCache = {};
    }

    _serializeWidgetData(widget) {
      // Clone the data so that we can modify it without affecting the widget.
      const data = JSON.parse((JSON.stringify(widget.data)));
      if (widget.name === 'xwiki-macro') {
        // Whether a macro widget is inline or block depends on the macro output, which is not synchronized and thus is
        // missing from the new content that we want to apply to the editor. When the macro widgets from the new content
        // are initialized the inline flag is set (in the absence of the macro outut) based on the siblings and the
        // parent of the macro widget. But there are cases where the macro can be both inline and block in the same
        // context (parent and siblings) so we don't know for sure if the original macro widget was inline or block
        // (e.g. an info box can technically be both inline and block inside a table cell because a table cell accepts
        // both inline and block level content). We had two options:
        // * include information about whether a macro widget is inline or block in the content that we synchronize (so
        //   that the inline flag is properly set when the macro widgets from the new content are initialized)
        // * ignore the inline flag when computing the changes between the old and the new content
        // We chose the second option because it's simpler and because the user can't change only the inline flag
        // without also changing either the macro parameters and content or the context where the macro is called.
        delete data.inline;
      }
      return JSONSortify(data);
    }

    _updateWidgets(updatedNodes) {
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
        if (updatedNode.nodeType !== Node.ELEMENT_NODE || !updatedNode.isConnected) {
          // The updated node is a text or comment, most probably, so it doesn't affect the widget. Note that for
          // attribute changes the updated node is the owner element. We also ignore nodes that have been removed.
          return;
        }
        const widgetPlaceholder = updatedNode.closest('.xwiki-widget');
        const widget = this._widgetCache[widgetPlaceholder?.dataset.widgetId];
        if (widget) {
          updatedWidgets.add(widget);
          // We also have to reinitialize the nested widgets.
          widgetPlaceholder.querySelectorAll('.xwiki-widget').forEach(nestedWidgetPlaceholder => {
            const nestedWidget = this._widgetCache[nestedWidgetPlaceholder.dataset.widgetId];
            if (nestedWidget) {
              updatedWidgets.add(nestedWidget);
            }
          });
        }
        // Most of the macro parametes are kept on the macro wrapper, so if the macro wrapper was updated then it's very
        // likely that the macro parameters were also updated. Some macro parameters are edited in-place using nested
        // editables. We need to re-render the macro if a new nested editable is added.
        shouldRefreshContent = shouldRefreshContent ||
          updatedNode.tagName.toLowerCase() === 'xwiki-widget-xwiki-macro' ||
          (updatedNode.tagName.toLowerCase() === 'xwiki-editable' && widget?.name === 'xwiki-macro' &&
            !widget.editables[updatedNode.getAttribute('name')]);
      });

      // Delete the updated widgets so that we can reinitialize them.
      updatedWidgets.forEach(widget => {
        delete this._ckeditor.widgets.instances[widget.id];
      });

      // Replace the widget placeholders with the actual widgets and restore the content of the widget editables.
      this._restoreWidgets();

      // Remove the widgets whose element was removed from the DOM and add widgets to match the widget elements found in
      // the DOM.
      this._ckeditor.widgets.checkWidgets();

      return shouldRefreshContent;
    }

    async _refreshContent(preserveSelection) {
      console.debug('Refreshing the content to re-render the macros.');
      this._ckeditor.execCommand('xwiki-refresh', {preserveSelection});

      return new Promise(resolve => {
        const macroPlugin = this._ckeditor.plugins['xwiki-macro'];
        if (macroPlugin) {
          macroPlugin.onceAfterRefresh(this._ckeditor, resolve);
        } else {
          resolve();
        }
      });
    }

    /**
     * CKEditor removes by default from the input / output HTML all the empty elements because they can't be edited.
     * This is fine when the content is saved or loaded into the editor, but it's not good when the content is
     * synchronized between editors because it prevents the user from inserting inline styles without selecting some
     * text first, if other users are making changes at the same time.
     */
    _protectEmptyElementsHoldingCaret() {
      // Used to find the previous empty element holding the caret. We don't keep the reference to the actual DOM
      // element because the DOM nodes can change when remote changes are applied.
      const userId = String(Math.random()).substring(2);
      this._ckeditor.on('change', () => {
        // Remove the marker from the previous empty element holding the caret.
        this._ckeditor.editable().findOne(`[${EMPTY_CARET_HOLDER}="${userId}"]`)
          ?.removeAttributes([EMPTY_CARET_HOLDER, CKE_SURVIVE]);
        // Check if the selection is collapsed in an empty element.
        const ranges = this._ckeditor.getSelection()?.getRanges();
        if (ranges?.length === 1 && ranges[0].collapsed &&
            ranges[0].startContainer.type === this._CKEDITOR.NODE_ELEMENT &&
            !ranges[0].startContainer.getChildCount()) {
          ranges[0].startContainer.setAttributes({
            [EMPTY_CARET_HOLDER]: userId,
            // Force CKEditor to keep this element in the HTML output so that it gets included in the content that is
            // synchronized between editors.
            [CKE_SURVIVE]: 1
          });
        }
      // If we don't use a lower priority for our listener then the CKE_SURVIVE attribute gets removed.
      }, null, null, 1000);

      // Make sure the EMPTY_CARET_HOLDER marker is not saved. We want it to be included in the output HTML only when
      // that HTML is used for realtime synchronization.
      this._ckeditor.dataProcessor?.htmlFilter?.addRules({
        elements: {
          '^': element => {
            if (!this._ckeditor.config._includeEmptyElementsHoldingCaret && element.attributes[EMPTY_CARET_HOLDER]) {
              delete element.attributes[EMPTY_CARET_HOLDER];
              if (!element.children.length) {
                // Remove the empty element (replicate the default CKEditor behavior).
                return false;
              }
            }
          }
        }
      }, {priority: 5, applyToAll: true});
    }
  }

  return CKEditorAdapter;
});