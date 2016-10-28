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
  CKEDITOR.plugins.add('xwiki-maximize', {
    requires: 'maximize',

    init: function(editor) {
      // There is also the 'maximize' event but it is fired after the command has been executed.
      editor.on('beforeCommandExec', function(event) {
        var command = event.data.command;
        if (command.name === 'maximize') {
          onBeforeToggleMaximize(event.editor, command.state);
        }
      });
    },

    afterInit: function(editor) {
      var form = editor.element && editor.element.getAscendant('form');
      if (form) {
        var maximizedSelector = form.findOne('input[name="x-maximized"]');
        if (maximizedSelector && maximizedSelector.getValue().substr(0, 9) === 'ckeditor#' &&
            maximizedSelector.getValue().substr(9) === editor.name) {
          // We need to wait for the editor to be ready before maximizing it.
          editor.once('instanceReady', function(event) {
            editor.execCommand('maximize');
          });
        }
      }
    }
  });

  var onBeforeToggleMaximize = function(editor, state) {
    var bottom = editor.ui.space('bottom');
    var form = bottom.getAscendant('form');
    if (!form) {
      return;
    }
    var bottomButtons = form.findOne('.bottombuttons');
    if (!bottomButtons) {
      return;
    }
    // The hidden input used to restore the full-screen mode when coming back from preview.
    var maximizedSelector = form.findOne('input[name="x-maximized"]');
    var body = form.getAscendant('body');
    if (state === CKEDITOR.TRISTATE_OFF) {
      // Go full-screen.
      // The maximize command removes the class names from the body element while on full-screen mode so we have to mark
      // the full-screen mode using an attribute instead of a class name. This marker is used in order to modify the
      // styles of elements outside the CKEditor UI (e.g. the styles of the notification container).
      body.setAttribute('data-maximized', 'true');
      bottomButtons.findOne('.buttons').insertAfter(bottom);
      if (maximizedSelector) {
        // Note that the 'x-maximized' input is also used by fullScreen.js (the default full-screen handler in XWiki)
        // which expects a CSS selector. Thus we construct a fake CSS selector that shouldn't match any element.
        maximizedSelector.setValue('ckeditor#' + editor.name);
      }
    } else if (state === CKEDITOR.TRISTATE_ON) {
      // Restore from full-screen.
      body.removeAttribute('data-maximized');
      bottomButtons.append(bottom.getNext());
      if (maximizedSelector) {
        maximizedSelector.setValue('');
      }
    }
  };
})();
