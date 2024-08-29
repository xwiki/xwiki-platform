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

  CKEDITOR.plugins.add('xwiki-maximize', {
    requires: 'maximize,xwiki-selection',

    init: function(editor) {
      // There is also the 'maximize' event but it is fired after the command has been executed.
      editor.on('beforeCommandExec', function(event) {
        var command = event.data.command;
        if (command.name === 'maximize') {
          onBeforeToggleMaximize(event.editor, command.state);
        }
      });

      // The default 'maximize' CKEditor plugin doesn't support the in-line (in-place) editor so we have to implement
      // our own 'maximize' command and tool bar button in this case.
      if (editor.elementMode === CKEDITOR.ELEMENT_MODE_INLINE) {
        addSupportToMaximizeTheInlineEditor(editor);
      }
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
    bottom = $(bottom && bottom.$);
    var form = $(editor.element.$).closest('form');
    var bottomButtons = form.find('.bottombuttons');
    // The hidden input used to restore the full-screen mode when coming back from preview.
    var maximizedSelector = form.find('input[name="x-maximized"]');
    var body = $(editor.element.$).closest('body');
    if (state === CKEDITOR.TRISTATE_OFF) {
      // Go full-screen.

      // The maximize command may remove the class names from the body element while on full-screen mode so it's safer
      // if we mark the full-screen mode using an attribute instead of a class name. This marker is used in order to
      // modify the styles of elements outside the CKEditor UI (e.g. the styles of the notification container).
      body.attr('data-maximized', 'true');

      // Move the form action buttons inside the CKEditor UI, at the bottom. Doesn't apply to the in-line editor.
      bottomButtons.find('.buttons').insertAfter(bottom);

      // Note that the 'x-maximized' input is also used by fullScreen.js (the default full-screen handler in XWiki)
      // which expects a CSS selector. Thus we construct a fake CSS selector that shouldn't match any element.
      maximizedSelector.val('ckeditor#' + editor.name);

    } else if (state === CKEDITOR.TRISTATE_ON) {
      // Exit full-screen.

      // Clear the hidden input that indicates the maximized text area.
      maximizedSelector.val('');

      // Move the form action buttons back to their original position. Doesn't apply to the in-line editor.
      bottom.next().appendTo(bottomButtons);

      // Remove the full-screen marker from the body element.
      body.removeAttr('data-maximized');
    }
  };

  var addSupportToMaximizeTheInlineEditor = function(editor) {
    if (editor.ui.addButton) {
      editor.ui.addButton('Maximize', {
        label: editor.lang.maximize.maximize,
        command: 'maximize',
        toolbar: 'tools,10'
      });
    }

    editor.addCommand('maximize', {
      modes: {
        wysiwyg: true,
        source: true
      },
      readOnly: true,
      editorFocus: false,
      canUndo: false,
      exec: function() {
        // We want to make sure the selection / caret is visible after this command is executed. In order to do this we
        // save the selection now and restore it at the end.
        CKEDITOR.plugins.xwikiSelection.saveSelection(editor);

        this.toggleState();
        var maximized = this.state === CKEDITOR.TRISTATE_ON;

        // Toggle the button label and hint.
        var button = this.uiItems[0];
        // Only try to change the button if it exists (https://dev.ckeditor.com/ticket/6166)
        if (button) {
          var label = maximized ? editor.lang.maximize.minimize : editor.lang.maximize.maximize;
          var buttonNode = CKEDITOR.document.getById(button._.id);
          buttonNode.getChild(1).setHtml(label);
          buttonNode.setAttribute('title', label);
        }

        if (maximized) {
          enterFullScreenMode(editor);
        } else {
          leaveFullScreenMode(editor);
        }

        // Mark the floating tool bar and the form action buttons that correspond to this editor so that they get to be
        // displayed at the top and at the bottom of the editor in full screen mode.
        $(document.getElementById('cke_' + editor.name)).toggleClass('cke_toolBar_active', maximized);
        getActionButtons(editor).toggleClass('cke_actionBar_active', maximized);

        // Update the position and height of the editing area, if maximized, otherwise remove the styles.
        updateEditingArea(editor);

        editor.fire('maximize', this.state);

        // Make sure the selection is visible.
        CKEDITOR.plugins.xwikiSelection.restoreSelection(editor);
      }
    });

    var beforeModeUnloadListener, modeListener, beforeDestroyListener;

    var enterFullScreenMode = function(editor) {
      // Remove the marker from the old editing area before switching modes (e.g. WYSIWYG to Source).
      beforeModeUnloadListener = editor.on('beforeModeUnload', updateEditingArea.bind(null, editor, true));

      // Mark the new editing area after switching modes (e.g. WYSIWYG to Source). We register this listener with a
      // higher priority in order to make sure it is executed before the listener that restores the selection after the
      // editing mode changed.
      modeListener = editor.on('mode', updateEditingArea.bind(null, editor), null, null, 5);

      // Leave the full-screen mode before destroying the editor in order to clean the full-screen markers.
      // Otherwise, the next editing session might start directly in full-screen mode.
      beforeDestroyListener = editor.once('beforeDestroy', editor.execCommand.bind(editor, 'maximize'));

      // Update the position and height of the editing area when the browser window is resized.
      $(window).on('resize.maximize', updateEditingArea.bind(null, editor));

      // Use a backdrop to hide the rest of the page content while the editor is maximized. This is needed especially
      // when switching editing modes while the editor is maximized because removing the current editable reveals the
      // content below it.
      $('<div class="cke_maximize_backdrop"></div>').appendTo(document.body);
    };

    var leaveFullScreenMode = function(editor) {
      [beforeModeUnloadListener, modeListener, beforeDestroyListener].forEach(function(listener) {
        listener.removeListener();
      });
      $(window).off('resize.maximize');

      // Remove the backdrop.
      $('body > div.cke_maximize_backdrop').remove();

      // Update the position of the floating toolbar.
      editor.fire('blur');
      editor.fire('focus');
    };

    var isMaximized = function(editor) {
      return editor.getCommand('maximize').state === CKEDITOR.TRISTATE_ON;
    };

    var getActionButtons = function(editor) {
      var iterator = $(editor.container.$);
      while (iterator.length) {
        var actionButtons = iterator.nextAll().find('.buttons');
        if (actionButtons.length) {
          return actionButtons;
        }
        iterator = iterator.parent();
      }
    };

    var updateEditingArea = function(editor, reset) {
      var $editable = $(editor.editable().$);
      if (reset !== true && isMaximized(editor)) {
        var toolBarHeight = $('.cke_toolBar_active').outerHeight();
        var actionBarHeight = $('.cke_actionBar_active').outerHeight();
        $editable.addClass('cke_editable_active').css({
          top: toolBarHeight,
          height: $(window).height() - toolBarHeight - actionBarHeight
        });
      } else {
        // Remove the full-screen styles.
        $editable.removeClass('cke_editable_active').css({
          top: '',
          height: ''
        });
      }
    };
  };
})();
