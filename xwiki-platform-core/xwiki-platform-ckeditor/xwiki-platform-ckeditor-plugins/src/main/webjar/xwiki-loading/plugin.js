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

  CKEDITOR.plugins.add('xwiki-loading', {
    beforeInit: function(editor) {
      let readOnlyCounter = 0;
      let originalSetReadOnly = editor.setReadOnly.bind(editor);
      editor.setReadOnly = function(readOnly) {
        if (readOnly) {
          readOnlyCounter++;
          if (readOnlyCounter === 1) {
            originalSetReadOnly(true);
          }
        } else if (readOnlyCounter > 0) {
          readOnlyCounter--;
          if (readOnlyCounter === 0) {
            originalSetReadOnly(false);
          }
        }
      };
      // Initialize the read-only counter, in case the editor is created read-only or in case the editor is created
      // hidden. See CKEDITOR-390: The inline editor loads as read-only in Safari
      if (editor.readOnly) {
        editor.setReadOnly(true);
      }

      let loadingCounter = 0;
      editor.setLoading = function(loading) {
        if (loading) {
          loadingCounter++;
          if (loadingCounter === 1) {
            this.fire('startLoading');
          }
        } else if (loadingCounter > 0) {
          loadingCounter--;
          if (loadingCounter === 0) {
            this.fire('endLoading');
          }
        }
      };

      editor.on('startLoading', function(event) {
        if (this.editable()) {
          // Put the editor in read-only mode while loading.
          this.setReadOnly(true);
        }
        var contentsSpace = this.ui.space('contents');
        if (contentsSpace) {
          contentsSpace.setStyle('visibility', 'hidden');
        }
      });

      editor.on('endLoading', function(event) {
        var contentsSpace = this.ui.space('contents');
        if (contentsSpace) {
          contentsSpace.removeStyle('visibility');
        }
        var editable = this.editable();
        if (editable) {
          if (editable.hasFocus) {
            // Firefox can have problems if we turn contenteditable=true on an element that was previously focused. The
            // workaround is to blur the element before making it editable and then focus it back.
            // Bug 1587966: Cursor does not appear if element is focused before being made contenteditable
            // CKEDITOR-369: The caret is not visible anymore after inserting a macro, when using the in-place editor
            editable.$.blur();
            setTimeout(function() {
              editable.focus();
            }, 0);
          }
          // End the read-only mode.
          this.setReadOnly(false);
        }
      });

      editor.toBeReady = function() {
        return new Promise(resolve => {
          function resolveWhenReady() {
            // Announce that the editor is ready only after the event loop has finished, to allow the code that
            // triggered the event to finish its execution. This is important for instance in the case of the refresh
            // command which needs to end the editor loading state before it can restore the selection (the editing area
            // needs to be visible and have the focus).
            setTimeout(async () => {
              // Double check that the editor is ready, because other event listeners or some remote change might have
              // updated the editor status in the meantime.
              await editor.toBeReady();
              resolve();
            }, 0);
          }
          if (loadingCounter > 0) {
            this.once('endLoading', resolveWhenReady);
          } else if (this.status === 'ready') {
            resolve();
          } else if (this.status === 'recreating') {
            this.once('contentDom', resolveWhenReady);
          } else {
            this.on('instanceReady', resolveWhenReady);
          }
        });
      };

      // Allow CKEditor plugins that require asynchronous initialization to delay the instance ready event until they
      // are fully initialized (or the asynchronous initialization fails).
      editor._instanceReadyPromises = [];
      editor.delayInstanceReady = function(promise) {
        editor._instanceReadyPromises.push(promise);
      };

      const originalFireOnce = editor.fireOnce;
      editor.fireOnce = function(eventName, ...args) {
        if (eventName === 'instanceReady') {
          // Fire the instanceReady event only after all the instance ready promises have been settled. Note that we
          // don't want to prevent the instanceReady event if the asynchronous plugin initialization fails because we
          // consider such plugins as optional (we expect them to fail gracefully if they can't be initialized).
          return Promise.allSettled(this._instanceReadyPromises).then(() => {
            originalFireOnce.call(this, eventName, ...args);
          });
        } else {
          // Fire the event immediately.
          return originalFireOnce.call(this, eventName, ...args);
        }
      };
    }
  });
})();
