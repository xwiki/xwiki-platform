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

  // The following code is partially taken (and adapted) from CKEditor's default sourcearea plugin.
  CKEDITOR.plugins.add('xwiki-sourcearea', {
    requires: 'xwiki-selection',

    init: function(editor) {
      editor.addMode('source', function(callback) {
        var contentsSpace = editor.ui.space('contents');

        var textArea = contentsSpace.getDocument().createElement('textarea');
        textArea.setStyles(CKEDITOR.tools.extend({
          width: '100%',
          height: '100%',
          resize: 'none',
          outline: 'none',
          'text-align': 'left'
        }, CKEDITOR.tools.cssVendorPrefix('tab-size', editor.config.sourceAreaTabSize || 2)));
        // Make sure that source code is always displayed LTR, regardless of editor language.
        // See https://dev.ckeditor.com/ticket/10105
        textArea.setAttribute('dir', 'ltr');
        textArea.addClass('cke_source').addClass('cke_enable_context_menu');

        if (editor.elementMode === CKEDITOR.ELEMENT_MODE_INLINE) {
          // Make the text area auto-resize to fit the content. Initialize with the height of the content edited in-line
          // in order to prevent UI flickering.
          var maximizeCommand = editor.getCommand('maximize') || {};
          initAutoHeight(textArea.$, contentsSpace.getSize('height', true), function() {
            // Don't auto resize when the editor is maximized (in full-screen mode).
            return maximizeCommand.state !== CKEDITOR.TRISTATE_ON;
          });
        } else {
          textArea.addClass('cke_reset');
        }

        contentsSpace.append(textArea);

        var editable = editor.editable(new SourceEditable(editor, textArea));
        // Fill the text area with the current editor data.
        editable.setData(editor.getData(1));

        editor.fire('ariaWidget', this);

        callback();
      });

      editor.addCommand('source', {
        // Switching between WYSIWYG and Source modes is asynchronous because the conversion is done server-side.
        async: true,
        canUndo: false,
        contextSensitive: false,
        editorFocus: false,
        // Technically, we don't need the content to be editable in order to switch the edit mode (we just need to be
        // able to get the data from the editor in order to perform the syntax conversion), but in practice the editor
        // is put in read-only mode when:
        // * the editor is loading (i.e. some asynchronous task is being performed, so we should wait for it to finish
        //   before switching the edit mode)
        // * the editor can't connect to the server (in which case switching the edit mode would fail anyway because we
        //   can't perform the syntax conversion)
        // The alternative is to override the checkAllowed method of the command to perform additional checks, but let's
        // keep it simple for now.
        // See CKEDITOR-66: Switch to source corrupt page when connection lost or when connection is very slow
        // See XWIKI-23489: The button to toggle the Source mode is enabled while the content is being reloaded after a
        // macro is inserted or updated
        readOnly: false,
        modes: {wysiwyg: 1, source: 1},
        exec: async function(editor) {
          if (editor.mode === 'wysiwyg') {
            editor.fire('saveSnapshot');
          }
          // Disable the command while switching between editing modes.
          this.disable();
          editor.setMode(editor.mode === 'source' ? 'wysiwyg' : 'source');
          try {
            await editor.toBeReady();
          } finally {
            // Re-enable the command with the proper toggle state.
            this.setState(editor.mode === 'source' ? CKEDITOR.TRISTATE_ON : CKEDITOR.TRISTATE_OFF);
            editor.fire('afterCommandExec', {name: this.name, command: this});
          }
        }
      });

      if (editor.ui.addButton) {
        editor.ui.addButton('Source', {
          label: editor.lang.sourcearea.toolbar,
          command: 'source',
          toolbar: 'mode,10'
        });
      }

      if (editor.elementMode === CKEDITOR.ELEMENT_MODE_INLINE) {
        // Update the source text area height after leaving the full screen mode.
        editor.on('maximize', function(event) {
          if (editor.mode === 'source' && event.data !== CKEDITOR.TRISTATE_ON) {
            updateSourceAreaHeight(editor);
          }
        });

        // Update the source text area height after the HTML to Wiki Syntax conversion is done, but before restoring the
        // selection because scrolling the selection into view depends on the text area height.
        CKEDITOR.plugins.xwikiSource.addModeChangeHandler(editor, () => {
          if (editor.mode === 'source') {
            updateSourceAreaHeight(editor);
          }
        }, 7);
      }
    }
  });

  function updateSourceAreaHeight(editor) {
    $(editor.editable().$).trigger('input');
  }

  const SourceEditable = CKEDITOR.tools.createClass({
    base: CKEDITOR.editable,
    proto: {
      setData: function(data) {
        this.setValue(data);
        this.status = 'ready';
        this.editor.fire('dataReady');
      },

      getData: function() {
        return this.getValue();
      },

      // Insertions are not supported in source editable.
      insertHtml: function() {},
      insertElement: function() {},
      insertText: function() {},

      // Read-only support for textarea.
      setReadOnly: function(isReadOnly) {
        this[(isReadOnly ? 'set' : 'remove') + 'Attribute']('readOnly', 'readonly');
      },

      detach: function() {
        SourceEditable.baseProto.detach.call(this);
        this.clearCustomData();
        this.remove();
      }
    }
  });

  // Credits: https://stackoverflow.com/a/25621277
  // The text area needs to be the only child of its parent in order for this to work.
  function autoHeight(textArea) {
    const $textArea = $(textArea);
    // Set the text area height on its parent (wrapper) in order to reserve the vertical space that the text area
    // currently takes so that the page layout doesn't change while we compute the updated text area height. If we
    // don't do this then the page vertical scroll position changes after we update the text area height and this
    // makes the caret "jump" up or down which is very annoying.
    $textArea.parent().css('min-height', $textArea.height() + 'px');
    // Update the text area height.
    $textArea.css({
      'height': 'auto',
      'overflow-y': 'visible'
    }).height(textArea.scrollHeight);
    // Restore the parent (wrapper) styles.
    $textArea.parent().css('min-height', '');
    return $textArea;
  }
  function initAutoHeight(textArea, initialHeight, isEnabled) {
    const $textArea = isEnabled() ? (
      initialHeight ? $(textArea).height(initialHeight) : autoHeight(textArea)
    ) : $(textArea);
    // Make sure we don't register the input listener twice.
    $textArea.off('input.autoHeight').on('input.autoHeight', function() {
      if (isEnabled()) {
        autoHeight(this);
      }
    });
  }
})();