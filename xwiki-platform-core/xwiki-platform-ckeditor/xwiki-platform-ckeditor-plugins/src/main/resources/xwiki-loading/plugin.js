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
    init: function(editor) {
      var loadingCounter = 0;
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
    }
  });
})();
