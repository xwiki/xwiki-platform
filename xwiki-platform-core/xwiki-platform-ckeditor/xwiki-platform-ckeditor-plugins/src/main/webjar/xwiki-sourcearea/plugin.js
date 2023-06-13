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
        modes: {wysiwyg: 1, source: 1},
        editorFocus: false,
        readOnly: 1,
        exec: function(editor) {
          if (editor.mode === 'wysiwyg') {
            editor.fire('saveSnapshot');
          }
          editor.getCommand('source').setState(CKEDITOR.TRISTATE_DISABLED);
          editor.setMode(editor.mode === 'source' ? 'wysiwyg' : 'source');
        },
        canUndo: false
      });

      editor.on('mode', function() {
        editor.getCommand('source').setState(editor.mode === 'source' ? CKEDITOR.TRISTATE_ON : CKEDITOR.TRISTATE_OFF);
      });

      if (editor.ui.addButton) {
        editor.ui.addButton('Source', {
          label: editor.lang.sourcearea.toolbar,
          command: 'source',
          toolbar: 'mode,10'
        });
      }

      if (editor.elementMode === CKEDITOR.ELEMENT_MODE_INLINE) {
        editor.on('maximize', function(event) {
          // Update the height of the source text area after leaving the full screen mode and center the selection.
          if (event.data !== CKEDITOR.TRISTATE_ON && editor.mode === 'source') {
            centerSourceAreaSelectionVertically(editor);
          }
        });

        require(['centerTextAreaSelectionVertically'], function($) {
          editor.on('modeReady', function() {
            if (editor.mode === 'source') {
              // Update the text area height after the HTML to Wiki Syntax conversion is done.
              // Then try to center the text selection vertically, if needed.
              centerSourceAreaSelectionVertically(editor);
            }
          });
        });

        var centerSourceAreaSelectionVertically = function(editor) {
          $(editor.editable().$).trigger('input').centerTextAreaSelectionVertically({padding: 65});
        };
      }
    }
  });

  var SourceEditable = CKEDITOR.tools.createClass({
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
  var initAutoHeight = function(textArea, initialHeight, isEnabled) {
    var autoHeight = function(textArea) {
      var $textArea = $(textArea);
      // Set the text area height on its parent (wrapper) in order to reserve the vertical space that the text area
      // currently takes so that the page layout doesn't change while we compute the updated text area height. If we
      // don't do this then the page vertical scroll position changes after we update the text area height and this
      // makes the caret "jump" up or down which is very annoying.
      $textArea.parent().css('min-height', $textArea.height() + 'px');
      // Update the text area height.
      $textArea.css({
        'height': 'auto',
        'overflow-y': 'hidden'
      }).height(textArea.scrollHeight);
      // Restore the parent (wrapper) styles.
      $textArea.parent().css('min-height', '');
      return $textArea;
    };
    var $textArea = isEnabled() ? (
      initialHeight ? $(textArea).height(initialHeight) : autoHeight(textArea)
    ) : $(textArea);
    // Make sure we don't register the input listener twice.
    $textArea.off('input.autoHeight').on('input.autoHeight', function() {
      if (isEnabled()) {
        autoHeight(this);
      }
    });
  };
})();

define('centerTextAreaSelectionVertically', ['jquery', 'scrollUtils'], function($, scrollUtils) {
  /**
   * Tries to center vertically the start of the text area selection within the first parent that has vertical scroll
   * bar.
   */
  var centerTextAreaSelectionVertically = function(textArea, settings) {
    var verticalScrollParent = scrollUtils.getVerticalScrollParent(textArea);
    if (verticalScrollParent) {
      var relativeTopOffset = scrollUtils.getRelativeTopOffset(textArea, verticalScrollParent);
      var selectionTopOffset = getSelectionTopOffset(textArea);
      // Compute the vertical distance from the start of the scroll parent to the start of the text selection.
      var selectionPosition = relativeTopOffset + selectionTopOffset;
      if (!settings.padding ||
          !scrollUtils.isCenteredVertically(verticalScrollParent, settings.padding, selectionPosition)) {
        // Center the selection by removing half of the scroll parent height (i.e. half of the visible vertical space)
        // from the selection position. If this is a negative value then the browser will use 0 instead.
        var scrollTop = selectionPosition - (verticalScrollParent.clientHeight / 2);
        verticalScrollParent.scrollTop = scrollTop;
      }
    }
  };

  var getSelectionTopOffset = function(textArea) {
    // Save the selection because we need to restore it at the end.
    var selection = {
      start: textArea.selectionStart,
      end: textArea.selectionEnd
    };

    // Save the value because we need to restore it at the end.
    var value = textArea.value;

    // Save the original styles before we change them, so that we can restore them at the end.
    var originalStyles = {};
    ['height', 'overflowY'].forEach(function(style) {
      originalStyles[style] = textArea.style[style] || '';
    });

    // Determine the height of the text area content before the selection.
    var $textArea = $(textArea).css({
      'height': 0,
      'overflow-y': 'hidden'
    }).val(value.substring(0, selection.start));

    var topOffset = textArea.scrollHeight;

    // Reset the styles, the value and the selection.
    $textArea.css(originalStyles).val(value);
    textArea.setSelectionRange(selection.start, selection.end);

    return topOffset;
  };

  $.fn.centerTextAreaSelectionVertically = function(settings) {
    settings = settings || {};
    return this.filter('textarea').each(function() {
      centerTextAreaSelectionVertically(this, settings);
    });
  };

  return $;
});
