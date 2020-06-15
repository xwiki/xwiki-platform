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

  // The following code is partially taken (and adapted) from CKEditor's default sourcearea plugin.
  CKEDITOR.plugins.add('xwiki-sourcearea', {
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
          jQuery(textArea.$).autoHeight(contentsSpace.getSize('height', true));
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
        require(['centerTextAreaSelectionVertically'], function($) {
          editor.on('modeReady', function() {
            if (editor.mode === 'source') {
              // Update the text area height after the HTML to Wiki Syntax conversion is done.
              // Then try to center the text selection vertically, if needed.
              $(editor.editable().$).trigger('input').centerTextAreaSelectionVertically({padding: 65});
            }
          });
        });
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
  jQuery.fn.autoHeight = jQuery.fn.autoHeight || function(initialHeight) {
    var autoHeight = function(element) {
      return jQuery(element).css({
        'height': 'auto',
        'overflow-y': 'hidden'
      }).height(element.scrollHeight);
    };
    return this.each(function() {
      var $this = initialHeight ? jQuery(this).height(initialHeight) : autoHeight(this);
      // Make sure we don't register the input listener twice.
      $this.off('input.autoHeight').on('input.autoHeight', function() {
        autoHeight(this);
      });
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

define('scrollUtils', ['jquery'], function($) {
  /**
   * Look for the first ancestor, starting from the given element, that has vertical scroll.
   */
  var getVerticalScrollParent = function(element) {
    var parent = element.parentNode;
    while (parent && !(parent.nodeType === Node.ELEMENT_NODE && hasVerticalScrollBar(parent))) {
      parent = parent.parentNode;
    }
    return parent;
  };

  var hasVerticalScrollBar = function(element) {
    var overflowY = $(element).css('overflow-y');
    // Use a delta to detect the vertical scroll bar, in order to overcome a bug in Chrome.
    // See https://bugs.chromium.org/p/chromium/issues/detail?id=34224 (Incorrect scrollHeight on the <body> element)
    var delta = 4;
    return element.scrollHeight > (element.clientHeight + delta) && overflowY !== 'hidden' &&
      // The HTML and BODY tags can have vertical scroll bars even if overflow is visible.
      (overflowY !== 'visible' || element === element.ownerDocument.documentElement ||
        element === element.ownerDocument.body);
  };

  /**
   * Compute the top offset of the given element within the specified ancestor.
   */
  var getRelativeTopOffset = function(element, ancestor) {
    // Save the vertical scroll position so that we can restore it afterwards.
    var originalScrollTop = ancestor.scrollTop;
    // Scroll the contents of the specified ancestor to the top, temporarily, so that the element offset, relative to
    // its ancestor, is positive.
    ancestor.scrollTop = 0;
    var relativeTopOffset = $(element).offset().top - $(ancestor).offset().top;
    // Restore the previous vertical scroll position.
    ancestor.scrollTop = originalScrollTop;
    return relativeTopOffset;
  };

  var isCenteredVertically = function(verticalScrollParent, padding, position) {
    return position >= (verticalScrollParent.scrollTop + padding) &&
      position <= (verticalScrollParent.scrollTop + verticalScrollParent.clientHeight - padding);
  };

  /**
   * Center the given element vertically within its scroll parent, if needed.
   *
   * @param element the element to center vertically
   * @param padding the amount of pixels from the top and from the bottom of the scroll parent that delimits the center
   *          area; when specified, the element is centered vertically only if it's not already in the center area
   *          defined by this padding
   */
  var centerVertically = function(element, padding) {
    var verticalScrollParent = getVerticalScrollParent(element);
    if (verticalScrollParent) {
      var relativeTopOffset = getRelativeTopOffset(element, verticalScrollParent);
      if (!padding || !isCenteredVertically(verticalScrollParent, padding, relativeTopOffset)) {
        // Center the element by removing half of the scroll parent height (i.e. half of the visible vertical space)
        // from the element's relative position. If this is a negative value then the browser will use 0 instead.
        var scrollTop = relativeTopOffset - (verticalScrollParent.clientHeight / 2);
        verticalScrollParent.scrollTop = scrollTop;
      }
    }
  };

  return {
    getVerticalScrollParent: getVerticalScrollParent,
    hasVerticalScrollBar: hasVerticalScrollBar,
    getRelativeTopOffset: getRelativeTopOffset,
    isCenteredVertically: isCenteredVertically,
    centerVertically: centerVertically
  };
});
