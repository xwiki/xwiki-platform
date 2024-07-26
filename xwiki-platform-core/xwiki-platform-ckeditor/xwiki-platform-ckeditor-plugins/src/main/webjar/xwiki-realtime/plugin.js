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
    requires: 'notification',

    init : function(editor) {
      applyStyleSheets(editor);

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

      require([
        'xwiki-realtime-loader',
        'xwiki-ckeditor-realtime-adapter',
        'xwiki-realtime-interface'
      ], (Loader, Adapter, Interface) => {
        enableRealtimeEditing(editor, Loader, Adapter).then(() => {
          // The edited (HTML) content is normalized when the realtime editing is enabled (e.g. by adding some BR
          // elements to ensure the HTML is the same across different browsers) which makes the editor dirty, although
          // there aren't any real content changes (that would be noticed in the source wiki syntax). We reset the dirty
          // state in order to avoid getting the leave confirmation when leaving the editor just after it was loaded.
          editor.resetDirty();
        });
        editor._realtimeInterface = Interface;
        editor._realtimeSource = {
          // True if the editor was in the realtime session before switching to source.
          realtime: false,

          // The value of editor.checkDirty() before switching to source.
          dirty: false,

          // The result of editor.getSnapshot() right after switching to source.
          previousValue: null
        };
        editor.on('beforeSetMode', this.beforeSetMode.bind(this));
      });
    },

    beforeSetMode: function(event) {

      const newMode = event.data;
      const editor = event.editor;
      const realtimeCheckbox = editor._realtimeInterface.getAllowRealtimeCheckbox();

      // This handles the switching between wysiwyg and source mode.
      // The switch between wysiwyg and source modes marks the editor as dirty.
      // But we would like to rely on the dirty state of the editor to decide wether
      // or not to re-join the realtime session after editing the source.
      // To determine wether or not the editor is dirty, CKEditor compares the current
      // snapshot of the content to a snapshot saved during a editor.resetDirty() call.
      // We need to keep track of the dirty state ourselves by saving a snapshot.

      // When switching back to wysiwyg, even without editing the source, the content
      // can be marked as dirty, making switching back and forth between wysiwyg and source
      // leave the realtime session permanently even when no changes were made.
      // To prevent that, we reset the dirty state when the realtime-tracked dirty
      // state is clean.

      // We use the beforeSetMode event to capture the dirty state prior to the mode change,
      // and abort the realtime session before the iframe (when in framed wysiwyg) is destroyed.
      // We use the dataReady event to restore the dirty state once the mode change is done.
      // and re-join the realtime session if suitable.

      // We keep track of the realtime status before switching to source mode in the
      // editor._realtimeSource attribute.

      if (editor.mode === 'wysiwyg' && newMode === 'source') {
        // Switching from wysiwyg to source mode.

        // Store the realtime state before switching to source mode
        // in order to restore the state when switching back to wysiwyg mode.
        editor._realtimeSource.realtime = realtimeCheckbox.prop('checked');

        // When using the iframed editor, switching to source destroys the iframe,
        // preventing the realtime framework from applying new patches.
        // We need to leave the realtime session when switching to source mode
        // in order to avoid unexpected behaviour.
        if (editor._realtimeSource.realtime) {

          // Store the dirty state before switching to source mode.
          editor._realtimeSource.dirty = editor.checkDirty();

          // Abort the realtime session.
          editor._realtime._onAbort();

          // Show the user that we left the realtime session.
          realtimeCheckbox.prop('checked', false);


          // We listen for the `dataReady` event and not the `mode` event
          // because the xwiki-source plugin listens for `mode` to update
          // the content of the sourcearea.
          // Because of this, dataReady is fired only once after a switch
          // to source.
          const dataReady = function() {
            // After switching to source.

            // Bulletproofing, when switching to source, setData is called multiple times.
            if (editor.mode !== 'source') {
              editor.once('dataReady', dataReady);
              return;
            }

            // Once the mode switch is done, we store a snapshot of the editor
            // allowing to check if changes were made when switching back to wysiwyg.
            editor._realtimeSource.previousValue = editor.getSnapshot();
          };

          editor.once('dataReady', dataReady);

          // Show a notification explaining that we temporarily left the realtime session.
          editor.showNotification(
            editor.localization.get('xwiki-realtime.notification.sourcearea.temporarilyLeftSession'),
            'info',
            5000);
        }

      } else if (editor.mode === 'source' && newMode === 'wysiwyg') {
        // Swithing from source to wysiwyg mode.

        // We only need to change the behavior if we were in a realtime session before switching to source.
        if (editor._realtimeSource.realtime) {

          // Before switching to wysiwyg, check wether the source was edited.
          const sourceDirty = editor._realtimeSource.previousValue !== editor.getSnapshot();

          // There are unsaved changes if there were unsaved changes before switching to source
          // or if there were changes made while we were in source view.
          const dirty = editor._realtimeSource.dirty || sourceDirty;

          const dataReady = function () {
            // After switching to wysiwyg.

            // Bulletproofing, in iframe mode, when switching to wysiwyg, setData is called multiple times.
            if (editor.mode !== 'wysiwyg') {
              editor.once('dataReady', dataReady);
              return;
            }

            // Update the realtime channels to prepare joining the realtime session,
            // as well as knowing if there are users in the session.
            editor._realtime._updateChannels().then(() => {

              // When the editor is dirty, we can join only if we are alone.
              if (dirty) {
                /*jshint -W106 */
                if (editor._realtime._realtimeContext.channels.wysiwyg_users > 0) {
                  /*jshint +W106 */

                  // Bring the autosave checkbox back.
                  editor._realtimeInterface.realtimeAllowed(false);

                  // Show a notification explaining that we are not rejoining the realtime session.
                  editor.showNotification(
                    editor.localization.get('xwiki-realtime.notification.sourcearea.notRejoiningSession'),
                    'warning',
                    5000);
                } else {
                  // Join the realtime session.

                  // Show a notification explaining that we are rejoining the session because we are alone.
                  editor.showNotification(
                    editor.localization.get('xwiki-realtime.notification.sourcearea.rejoiningSession.alone'),
                    'success',
                    5000);
                  realtimeCheckbox.prop('checked', true);
                  editor._realtime._startRealtimeSync();
                }
              } else {
                // Join the realtime session.
                editor.showNotification(
                  editor.localization.get('xwiki-realtime.notification.sourcearea.rejoiningSession.noChanges'),
                  'success',
                  5000);
                realtimeCheckbox.prop('checked', true);

                const readOnly = function () {
                  // Bulletproofing, the readOnly event is triggered when making the editor read-write
                  // but also when making the editor read-only.
                  // We know that the realtime sync is ready when the editor is made read-write.
                  if (editor.readOnly) {
                    editor.once('readOnly', readOnly);
                    return;
                  }
                  // There are no unsaved changes.
                  // But the editor might consider itself dirty because of the mode change.
                  editor.resetDirty();
                };
                editor.once('readOnly', readOnly);

                editor._realtime._startRealtimeSync();

              }
            });
          };

          editor.once('dataReady', dataReady);
        }

      }
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
      const range = editor.getSelection()?.getRanges()[0];
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

  function enableRealtimeEditing(editor, Loader, Adapter) {
    const info = {
      type: 'wysiwyg',
      field: editor.name,
      // This is used to generate the WYSIWYG editor URL so we need to take into account if we are in view mode
      // (i.e. in-place editing) or in edit mode (standalone editing).
      href: window.XWiki?.contextaction === 'view' ? '&force=1' : '&editor=wysiwyg&force=1',
      name: 'WYSIWYG',
      compatible: ['wysiwyg', 'wiki']
    };

    return Loader.bootstrap(info).then(realtimeContext => {
      return new Promise((resolve, reject) => {
        require(['xwiki-realtime-wysiwyg'], RealtimeWysiwygEditor => {
          editor._realtime = new RealtimeWysiwygEditor(new Adapter(editor, CKEDITOR), realtimeContext);
  
          // When someone is offline, they may have left their tab open for a long time and the lock may have
          // disappeared. We're refreshing it when the editor is focused so that other users will know that someone is
          // editing the document.
          editor.on('focus', () => {
            editor._realtime.lockDocument();
          });

          resolve(editor._realtime);
        }, reject);
      });
    });
  }
})();
