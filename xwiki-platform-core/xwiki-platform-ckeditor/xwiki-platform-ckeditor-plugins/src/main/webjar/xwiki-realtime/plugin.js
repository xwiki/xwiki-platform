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
(function() {
  'use strict';
  var $ = jQuery;

  // Declare the configuration namespace.
  CKEDITOR.config['xwiki-realtime'] = CKEDITOR.config['xwiki-realtime'] || {
    __namespace: true
  };

  CKEDITOR.plugins.add('xwiki-realtime', {
    requires: 'notification,xwiki-loading',

    init: function(editor) {
      applyStyleSheets(editor);

      if (editor.elementMode === CKEDITOR.ELEMENT_MODE_INLINE) {
        // When editing in-place we need to maximize the parent of the editable area in order to have the user caret
        // indicators visible (they are injected after the editable area).
        this.fixFullScreenMode(editor);
      }

      // CKEditor's HTML parser doesn't preserve the space character typed at the end of a line of text. For instance,
      // the following:
      //   CKEDITOR.htmlParser.fragment.fromHtml('<p>x </p>')
      // is parsed as:
      //   <p>x</p>
      // Normally browsers should insert a non-breaking space when the user types a space at the end of a line. Chrome
      // behaves like this. Firefox inserts a normal space character instead. We need to fix this otherwise that space
      // character is lost when the content is saved or when the user switches to Source. This is especially critical
      // for the realtime editing where the content you type is merged with the content typed by other users and fed
      // back to your editor. So if you type at the end of a paragraph while remote changes are received you don't want
      // the space between the words you type to be lost.
      preserveSpaceCharAtTheEndOfLine(editor);

      editor.delayInstanceReady(new Promise((resolve, reject) => {
        const rejectWithNotification = (reason, ...args) => {
          if (reason) {
            const message = reason.message || reason;
            new XWiki.widgets.Notification(message, 'error');
          }
          reject(reason, ...args);
        };
        require([
          'xwiki-realtime-loader',
          'xwiki-ckeditor-realtime-adapter',
          'xwiki-realtime-interface',
          'xwiki-l10n!xwiki-realtime-messages'
        ], asyncRequireCallback(async (Loader, Adapter, Interface, Messages) => {
          editor._realtime = {
            adapter: new Adapter(editor, CKEDITOR),
            interface: Interface,
            // Rejoin the realtime collaboration when switching back to WYSIWYG mode.
            rejoin: true,
          };

          try {
            const realtimeSupported = await enableRealtimeEditing(editor, Loader);
            if (!realtimeSupported) {
              // "Fail" silently if realtime collaboration is not supported in this context.
              return;
            }
          } catch (error) {
            // The caught error may be too technical for the end user, so we wrap it in a more user-friendly message,
            // making sure we don't lose the original error that will be logged in the console for debugging.
            throw new Error(Messages['join.error'], {cause: error});
          }

          // The edited (HTML) content is normalized when the realtime editing is enabled (e.g. by adding some BR
          // elements to ensure the HTML is the same across different browsers) which makes the editor dirty, although
          // there aren't any real content changes (that would be noticed in the source wiki syntax). We reset the
          // dirty state in order to avoid getting the leave confirmation when leaving the editor just after it was
          // loaded.
          editor.resetDirty();

          // Leave / rejoin the realtime session when switching between WYSIWYG and Source modes.
          // We flush uncommitted work before leaving the WYSIWYG mode, in order to avoid losing local changes. We do
          // this very early, before the new mode is set, hopefully before the editor is locked (loading).
          editor.on('beforeSetMode', this.onBeforeSetMode.bind(this), null, null, 0);
          CKEDITOR.plugins.xwikiSource?.addModeChangeHandler(editor, this.onModeChanged.bind(this), 10);
          editor.on('modeReady', this.onModeReady.bind(this));
        }, resolve, rejectWithNotification), rejectWithNotification);
      }));
    },

    fixFullScreenMode: function(editor) {
      const prepareForFullScreenMode = (realtimeSupported) => {
        if (realtimeSupported === undefined) {
          realtimeSupported = editor.mode === 'wysiwyg';
        }
        // Maximize the parent of the editable area in order to include the user caret indicators. We do this only for
        // the WYSIWYG mode because it's the only mode that supports realtime editing.
        $(editor.element.getParent().$).toggleClass('cke_editable_fullscreen', realtimeSupported);
      };
      // Update the area that is maximized whenever the editing mode changes.
      CKEDITOR.plugins.xwikiSource?.addModeChangeHandler(editor, () => prepareForFullScreenMode());
      // Clean up when the editor is destroyed, but only after the full-screen mode is exited.
      editor.on('beforeDestroy', () => prepareForFullScreenMode(false), null, null, 100);
      // The mode change handler we added above is not called when the editor is initialized, so we need to trigger it.
      prepareForFullScreenMode();
    },

    onBeforeSetMode: function(event) {
      const editor = event.editor;
      if (editor.mode === 'wysiwyg') {
        // Flushing uncommitted work has no effect if we're disconnected from the realtime session or if the connection
        // is paused, which is why we do it here, very early, before the new mode is set, so before the editor is locked
        // (put into loading mode, which pauses the connection).
        //
        // Note that this operation is asynchronous but we can't block the event processing until it completes (because 
        // CKEditor doesn't support async event listeners). This is fine in practice because switching from WYSIWYG to
        // Source mode is asynchronous as well, so there is time for the uncommitted work to arrive to the server.
        editor._realtime.editor?._flushUncommittedWork();
      }
    },

    onModeChanged: async function(editor, {previousMode}) {
      if (previousMode === 'wysiwyg' && editor.mode === 'source') {
        // Switching from WYSIWYG to Source mode.

        // Store the realtime state before switching to Source mode in order to restore the state when switching back to
        // WYSIWYG mode.
        editor._realtime.rejoin = editor._realtime.context?.realtimeEnabled;

        // When using the iframed editor, switching to source destroys the iframe, preventing the realtime framework
        // from applying new patches. We need to leave the realtime session when switching to source mode in order to
        // avoid unexpected behaviour.
        if (editor._realtime.rejoin) {
          // Notify the user that we're leaving the realtime collaboration.
          editor.showNotification(editor.localization.get('xwiki-realtime.leavingCollaboration'));

          // Abort the realtime session. This operation is asynchronous because we want to push uncommitted work before
          // disconnecting.
          await editor._realtime.disconnect();
        }
      } else if (previousMode === 'source' && editor.mode === 'wysiwyg' && editor._realtime.rejoin) {
        // Swithing from Source back to WYSIWYG mode, which had realtime enabled before the switch.
        editor.showNotification(editor.localization.get('xwiki-realtime.joiningCollaboration'));
        editor._realtime.context.realtimeEnabled = true;
        await editor._realtime.connect();
      }
    },

    onModeReady: function(event) {
      // The user should not be able to join the realtime editing session while in source mode. We disable the allow
      // realtime checkbox while in source mode, and enable it when we go back to wysiwyg.
      const editor = event.editor;
      const realtimeCheckbox = editor._realtime.interface.getAllowRealtimeCheckbox();
      realtimeCheckbox.prop('disabled', editor.mode !== 'wysiwyg');
    }
  });

  function applyStyleSheets(editor) {
    const styleSheets = editor.config['xwiki-realtime'].stylesheets;

    // Add the stylesheet to the page where the editor is used (e.g. for the realtime toolbar).
    addStyleSheets(styleSheets, document);

    // Add the stylesheet also to the edited content, in case the edited content is inside an iframe, for the user
    // caret indicators. We have to do this each time the iframe is reloaded (e.g. after a macro is inserted).
    // Note that we can't simply use the editor#addContentsCss method because we have the fullPage configuration on.
    editor.on('contentDom', () => {
      addStyleSheets(styleSheets, editor.document.$);
    });
    if (editor.document && editor.document.$ !== document) {
      // Initial content load.
      addStyleSheets(styleSheets, editor.document.$);
    }
  }

  function addStyleSheets(urls, doc) {
    urls.forEach(url => {
      $('<link/>', doc).attr({
        type: 'text/css',
        rel: 'stylesheet',
        href: url
      }).appendTo(doc.head);
    });
  }

  function preserveSpaceCharAtTheEndOfLine(editor) {
    editor.on('beforeGetData', () => {
      // The selection is bound to the window so editor.getSelection() throws an exception if there's no window which is
      // the case when either the editor or its editable area is detached.
      const range = !editor.isDetached() && !editor.editable()?.isDetached() && editor.getSelection()?.getRanges()[0];
      const textNode = range?.startContainer;
      // Check if the caret is at the end of a text node that ends with a space and is followed by a line break.
      if (editor.mode === 'wysiwyg' && range?.collapsed && textNode.type === CKEDITOR.NODE_TEXT &&
          range.startOffset === textNode.getLength() && textNode.getText().endsWith(' ') &&
          isFollowedByLineBreak(textNode)) {
        fixTrailingSpace(textNode);
        // Make sure the caret remains at the end of the text node after we modify its data.
        range.setStart(textNode, textNode.getLength());
        range.select();
      }
    // Note that we use a high priority (0) to ensure that our listener is called before the HTML parser.
    }, null, null, 0);
  }

  function isFollowedByLineBreak(textNode) {
    // Is directly inside a block element...
    return CKEDITOR.dtd.$block[textNode.getParent().getName()] &&
      // ...and is either the last child or followed by a block element or a BR element.
      (!textNode.getNext() || CKEDITOR.dtd.$block[textNode.getNext().getName?.()] ||
        textNode.getNext().getName?.() === 'br');
  }

  function fixTrailingSpace(textNode) {
    // We replicate here the behavior of Chrome when multiple space characters are typed.
    const whitespaceLength = textNode.getLength() - textNode.getText().trimEnd().length;
    const whitespaceSuffix = '\u00A0 '.repeat((whitespaceLength - 1) / 2) +
      '\u00A0'.repeat((whitespaceLength - 1) % 2 + 1);
    textNode.$.replaceData(textNode.getLength() - whitespaceLength, whitespaceLength, whitespaceSuffix);
  }

  async function enableRealtimeEditing(editor, Loader) {
    const info = {
      type: 'wysiwyg',
      field: editor.name,
      // This is used to generate the WYSIWYG editor URL so we need to take into account if we are in view mode
      // (i.e. in-place editing) or in edit mode (standalone editing).
      href: window.XWiki?.contextaction === 'view' ? '&force=1' : '&editor=wysiwyg&force=1',
      name: 'WYSIWYG',
      compatible: ['wysiwyg', 'wiki']
    };

    editor._realtime.context = await Loader.bootstrap(info);
    if (editor._realtime.context) {
      return await new Promise((resolve, reject) => {
        require(['xwiki-realtime-wysiwyg'], asyncRequireCallback(RealtimeWysiwygEditor => {
          editor._realtime.connect = async () => {
            if (!editor._realtime.editor) {
              editor._realtime.editor = new RealtimeWysiwygEditor(editor._realtime.adapter, editor._realtime.context);
              await editor._realtime.editor.toBeConnected();
            } else {
              await editor._realtime.editor._updateChannels();
              await editor._realtime.editor._startRealtimeSync();
            }
            onAfterJoinCollaboration(editor);
            return editor._realtime.editor;
          };
          editor._realtime.disconnect = async () => {
            await editor._realtime.editor?._onAbort();
            onAfterLeaveCollaboration(editor);
          };
          return editor._realtime.connect();
        }, resolve, reject), reject);
      });
    }
  }

  function onAfterJoinCollaboration(editor) {
    // Disable the leave confirmation because we have automatic saving.
    const config = editor.config['xwiki-save'] = editor.config['xwiki-save'] || {};
    // Backup the current value in order to be able to restore it when leaving the collaboration.
    config._oldLeaveConfirmation = config.leaveConfirmation;
    config.leaveConfirmation = false;
  }

  function onAfterLeaveCollaboration(editor) {
    // Restore the leave confirmation.
    const config = editor.config['xwiki-save'];
    if (config?._oldLeaveConfirmation !== undefined) {
      config.leaveConfirmation = config._oldLeaveConfirmation;
      delete config._oldLeaveConfirmation;
    }
  }

  /**
   * This covers two problems:
   *   1. the error callback passed to require() is not called when there is an exception in the success callback
   *   2. the success callback is not called if marked as async
   */
  function asyncRequireCallback(asyncCallback, resolve, reject) {
    return (...args) => {
      Promise.resolve(asyncCallback(...args)).then(resolve).catch(reject);
    };
  }

  // Add support for synchronizing the upload widgets when realtime editing is enabled.
  const originalAddUploadWidget = CKEDITOR.fileTools.addUploadWidget;
  CKEDITOR.fileTools.addUploadWidget = function(editor, widgetName, ...args) {
    if (editor.plugins['xwiki-realtime']) {
      const handler = editor.on('widgetDefinition', function(event) {
        const widgetDefinition = event.data;
        if (widgetDefinition.name === widgetName) {
          handler.removeListener();

          const originalInit = widgetDefinition.init;
          Object.assign(widgetDefinition, {
            /**
             * Upcast upload widgets coming from other users editing at the same content in realtime. These widgets are
             * temporary placeholders that are going to be replaced with the actual content when the upload is complete.
             *
             * @param {CKEDITOR.htmlParser.element} element the element to check if it can be upcasted to this upload
             *   widget
             */
            upcast: function(element) {
              return element.hasClass?.('xwiki-widget-placeholder-' + this.name);
            },

            init: function(...args) {
              const widget = this;
              // Call the original init method only if this is a real upload widget and not a placeholder.
              if (widget.wrapper.findOne('[data-cke-upload-id]')) {
                originalInit.call(widget, ...args);
              }
            },

            /**
             * Upload widgets are by default downcasted to an empty text node because they are temporary placeholders
             * that are not supposed to be saved. They are replaced with the actual content when the upload is complete.
             *
             * The realtime editor synchronizes the output HTML of the editor which by default doesn't include the
             * upload widgets (because they are not supposed to be saved). This creates a problem when a remote change
             * is received while an upload is in progress: the remote content doesn't include the upload widget so the
             * upload widget gets removed when we compute the diff between the local content and the remote content and
             * then apply the patch.
             *
             * In order to overcome this we have to include the upload widgets in the output HTML of the editor, when
             * this HTML is used for realtime synchronization.
             *
             * @param {CKEDITOR.htmlParser.element} widgetElementClone a clone of the widget element to downcast
             */
            downcast: function(widgetElementClone) {
              const widget = this;
              if (widget.editor.config._includeUploadWidgetsInOutputHTML) {
                const placeholder = new CKEDITOR.htmlParser.element(widget.inline ? 'span' : 'div', {
                  'class': 'xwiki-widget-placeholder-' + widget.name
                });
                // Add a non-breaking space to ensure the placeholder is visible and thus not removed by CKEditor.
                placeholder.add(new CKEDITOR.htmlParser.text('\u00A0'));
                return placeholder;
              } else {
                return new CKEDITOR.htmlParser.text('');
              }
            },
          });

          // Make sure the upload widget placeholders are not saved.
          editor.dataProcessor?.htmlFilter?.addRules({
            '^': function(element) {
              if (element.hasClass?.('xwiki-widget-placeholder-' + widgetName)) {
                return false;
              }
            }
          }, {priority: 5, applyToAll: true});
        }
      });
    }
    return originalAddUploadWidget.call(this, editor, widgetName, ...args);
  };
})();
