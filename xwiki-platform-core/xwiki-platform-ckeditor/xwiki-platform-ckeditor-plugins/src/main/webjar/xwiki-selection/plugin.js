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

  CKEDITOR.plugins.xwikiSelection = {
    saveSelection: function(editor) {
      var editable = editor.editable();
      if (editable) {
        // Convert the fake editor selection to a real (native) selection if needed.
        ensureRealSelection(editor);
        editor._.textSelection = CKEDITOR.plugins.xwikiSelection.textSelection.from(editable.$);
      } else {
        delete editor._.textSelection;
      }
    },

    restoreSelection: function(editor) {
      var editable = editor.editable();
      var textSelection = editor._.textSelection;
      if (editable && textSelection) {
        editor.focus();
        textSelection.applyTo(editable.$);
        // Check if there is a widget to select.
        var widget = editor.widgets && getWidgetToSelect(editor);
        if (widget) {
          // Select the widget.
          scrollIntoViewAndFocusWidget(widget);
        } else {
          // Just notify the editor that the selection has changed so that it can update the tool bar for instance.
          editor.selectionChange();
        }
      }
    }
  };

  CKEDITOR.plugins.add('xwiki-selection', {
    onLoad: function() {
      require(['textSelection'], function(textSelection) {
        CKEDITOR.plugins.xwikiSelection.textSelection = textSelection;
      });
    }
  });

  /**
   * The native selection doesn't always match what the user perceives as selected in the editor UI. For instance, when
   * the user selects a widget (e.g. a macro or an image, which are implemented as widgets) the editor creates a fake
   * selection and places the native selection inside a hidden element. This way the widget text doesn't appear as
   * selected and the editor can style the selected widget in any way. The role of this function is to ensure that the
   * native selection mathes the fake selection.
   */
  var ensureRealSelection = function(editor) {
    var selection = editor.getSelection();
    if (selection && selection.isFake) {
      var nativeSelection = selection.document.$.defaultView.getSelection();
      nativeSelection.removeAllRanges();
      selection.getRanges().map(toNativeRange).forEach(nativeSelection.addRange.bind(nativeSelection));
    }
  };

  var toNativeRange = function(range) {
    var nativeRange = range.startContainer.$.ownerDocument.createRange();
    nativeRange.setStart(range.startContainer.$, range.startOffset);
    nativeRange.setEnd(range.endContainer.$, range.endOffset);
    return nativeRange;
  };

  var getWidgetToSelect = function(editor) {
    var selection = editor.getSelection(/* forceRealSelection: */ true);
    if (selection) {
      var nativeSelection = selection.getNative();
      for (var i = 0; i < nativeSelection.rangeCount; i++) {
        var widget = getWidgetInRange(nativeSelection.getRangeAt(i), editor);
        if (widget) {
          return widget;
        }
      }
    }
  };

  var getWidgetInRange = function(range, editor) {
    var widgetWrapper;
    var startEditable = $(range.startContainer).closest('[contenteditable]');
    var endEditable = $(range.endContainer).closest('[contenteditable]');
    if (startEditable.attr('contenteditable') === 'false' && (startEditable[0] === endEditable[0] ||
        rangeEndsJustAfter(range, startEditable[0]))) {
      // The range starts inside a widget and ends either inside the same widget or just after it.
      widgetWrapper = startEditable[0];
    } else if (endEditable.attr('contenteditable') === 'false' && rangeStartsJustBefore(range, endEditable[0])) {
      // The range ends inside a widget and starts just before it.
      widgetWrapper = endEditable[0];
    }
    return widgetWrapper && editor.widgets.getByElement(new CKEDITOR.dom.element(widgetWrapper), true);
  };

  var rangeEndsJustAfter = function(range, node) {
    var delta = node.ownerDocument.createRange();
    delta.setStartAfter(node);
    delta.setEnd(range.endContainer, range.endOffset);
    return isEmptyRange(delta);
  };

  var rangeStartsJustBefore = function(range, node) {
    var delta = node.ownerDocument.createRange();
    delta.setStart(range.startContainer, range.startOffset);
    delta.setEndBefore(node);
    return isEmptyRange(delta);
  };

  var isEmptyRange = function(range) {
    // TODO: The the complexity can onlu be lowered. Once below the default maxcomplexity (10 at the time of writing), 
    //  the jshint annotation can be removed.
    /*jshint maxcomplexity:12 */
    while (!range.collapsed) {
      if (typeof range.endContainer.nodeValue === 'string') {
        if (range.endOffset === 0) {
          // The range ends at the start of a text node. Move the end before that text node.
          range.setEndBefore(range.endContainer);
        } else {
          break;
        }
      } else if (range.endOffset === 0) {
        // The range ends at the start of an element (inside). Move the end before that element.
        range.setEndBefore(range.endContainer);
      } else {
        break;
      }
    }
    var skippedBR = false;
    while (!range.collapsed) {
      if (typeof range.startContainer.nodeValue === 'string') {
        if (range.startOffset === range.startContainer.nodeValue.length) {
          // The range starts at the end of a text node. Move the start after that text node.
          range.setStartAfter(range.startContainer);
        } else {
          break;
        }
      } else if (range.startOffset === range.startContainer.childNodes.length) {
        // The range starts at the end of an element (inside). Move the start after that element.
        range.setStartAfter(range.startContainer);
      } else if (!skippedBR && range.startOffset === range.startContainer.childNodes.length - 1 &&
          range.startContainer.lastChild.nodeName.toLowerCase() === 'br') {
        // The element ends with a BR and the range starts just before it. This BR is often used to make an empty line
        // editable (can't place the caret on that empty line otherwise), so we can skip it, but only once (we don't
        // want to skip multiple empty lines). Move the start after the element.
        range.setStartAfter(range.startContainer);
        skippedBR = true;
      } else {
        break;
      }
    }
    return range.collapsed;
  };

  var scrollIntoViewAndFocusWidget = function(widget) {
    widget.wrapper.scrollIntoView();
    const placeholder = widget.wrapper.findOne('.macro-placeholder')?.$;
    const selection = widget.editor.getSelection(/* forceRealSelection: */ true)?.getNative();
    const firstNestedEditable = Object.values(widget.editables)[0];
    // The selection ends up in the macro placeholder after inserting a macro widget, which is usually hidden so the
    // selection is not visible. Let's move the caret at the start of the first nested editable in this case (for macros
    // that can be edited inline).
    if (placeholder?.contains(selection?.focusNode) && firstNestedEditable) {
      // Place the caret at the start of the first nested editable.
      let range = new CKEDITOR.dom.range(widget.editor.document);
      range.moveToElementEditablePosition(firstNestedEditable);
      range.select();
    } else {
      widget.focus();
    }
  };
})();

define('node-module', ['jquery'], function($) {
  return {
    load: function(name, req, onLoad, config) {
      $.get(req.toUrl(name + '.js'), function(text) {
        onLoad.fromText('define(function(require, exports, module) {' + text + '});');
      }, 'text');
    }
  };
});

define('textSelection', ['jquery', 'node-module!fast-diff', 'scrollUtils'], function($, diff, scrollUtils) {
  var isTextInput = function(element) {
    return typeof element.setSelectionRange === 'function';
  };

  var getTextSelection = function(element) {
    if (isTextInput(element)) {
      return {
        text: element.value,
        startOffset: element.selectionStart,
        endOffset: element.selectionEnd
      };
    } else {
      var selection = element.ownerDocument.defaultView.getSelection();
      if (selection && selection.rangeCount > 0) {
        var range = selection.getRangeAt(0);
        if (elementContainsRange(element, range)) {
          return getTextSelectionFromRange(element, range);
        }
      }
      return {
        text: element.textContent,
        startOffset: 0,
        endOffset: 0
      };
    }
  };

  var elementContainsRange = function(element, range) {
    var rangeContainer = range.commonAncestorContainer;
    // Element#contains() returns false on IE11 if we pass text nodes. Let's pass the parent element in this case.
    if (rangeContainer.nodeType !== Node.ELEMENT_NODE) {
      rangeContainer = rangeContainer.parentNode;
    }
    return element.contains(rangeContainer);
  };

  var getTextSelectionFromRange = function(root, range) {
    var beforeRange = root.ownerDocument.createRange();
    beforeRange.setStartBefore(root);
    beforeRange.setEnd(range.startContainer, range.startOffset);
    var startOffset = beforeRange.toString().length;
    return {
      text: root.textContent,
      startOffset: startOffset,
      endOffset: startOffset + range.toString().length
    };
  };

  var getRangeFromTextSelection = function(root, textSelection) {
    var start = findTextOffsetInDOM(root, textSelection.startOffset);
    var end = textSelection.endOffset === textSelection.startOffset ? start :
      findTextOffsetInDOM(root, textSelection.endOffset);
    var range = root.ownerDocument.createRange();
    range.setStart(start.node, start.offset);
    range.setEnd(end.node, end.offset);
    return range;
  };

  var findTextOffsetInDOM = function(root, offset) {
    var count = 0, node, iterator = root.ownerDocument.createNodeIterator(root, NodeFilter.SHOW_TEXT,
      // Accept all text nodes (we need this for IE11 which complains that the last 2 arguments are not optional).
      function(node) {
        return NodeFilter.FILTER_ACCEPT;
      }, false);
    do {
      node = iterator.nextNode();
      count += node ? node.nodeValue.length : 0;
    } while (node && count < offset);
    if (node) {
      return {
        node: node,
        offset: offset - (count - node.nodeValue.length)
      };
    } else {
      return {
        node: root,
        offset: offset > 0 ? root.childNodes.length : 0
      };
    }
  };

  var changeText = function(textSelection, newText) {
    var changes = diff(textSelection.text, newText);
    var startOffset = findTextOffsetInChanges(changes, textSelection.startOffset);
    var endOffset = textSelection.endOffset === textSelection.startOffset ? startOffset :
      findTextOffsetInChanges(changes, textSelection.endOffset);
    return {
      text: newText,
      startOffset: startOffset,
      endOffset: endOffset
    };
  };

  var findTextOffsetInChanges = function(changes, oldOffset) {
    var count = 0, newOffset = oldOffset;
    for (var i = 0; i < changes.length && count < oldOffset; i++) {
      var change = changes[i];
      if (change[0] < 0) {
        // Delete: shift the offset to the left.
        if (count + change[1].length > oldOffset) {
          // Shift the offset to the left with the number of deleted characters before the original offset.
          newOffset -= oldOffset - count;
        } else {
          // Shift the offset to the left with the number of deleted characters.
          newOffset -= change[1].length;
        }
        count += change[1].length;
      } else if (change[0] > 0) {
        // Insert: shift the offset to the right with the number of inserted characters.
        newOffset += change[1].length;
      } else {
        // Keep: don't change the offset.
        count += change[1].length;
      }
    }
    return newOffset;
  };

  var applySelection = function(element, range) {
    if (isTextInput(element)) {
      // Scroll the selection into view.
      // See https://bugs.chromium.org/p/chromium/issues/detail?id=331233
      var fullText = element.value;
      element.value = fullText.substring(0, range.endOffset);
      // Scroll to the bottom.
      element.scrollTop = element.scrollHeight;
      var canScroll = element.scrollHeight > element.clientHeight;
      element.value = fullText;
      if (canScroll) {
        // Scroll to center the selection.
        element.scrollTop += element.clientHeight / 2;
      }
      // And then apply the selection.
      element.setSelectionRange(range.startOffset, range.endOffset);
    } else {
      // Scroll the selection into view.
      var scrollTarget = range.startContainer;
      if (scrollTarget.nodeType !== Node.ELEMENT_NODE) {
        scrollTarget = scrollTarget.parentNode;
      }
      scrollUtils.centerVertically(scrollTarget, 65);
      // And then apply the selection.
      var selection = element.ownerDocument.defaultView.getSelection();
      selection.removeAllRanges();
      selection.addRange(range);
    }
  };

  return {
    from: function(element) {
      return $.extend({}, this, getTextSelection(element));
    },
    applyTo: function(element) {
      var range;
      if (isTextInput(element)) {
        range = this.withText(element.value);
      } else {
        range = this.withText(element.textContent).asRange(element);
      }
      applySelection(element, range);
    },
    withText: function(text) {
      if (this.text === text) {
        return this;
      } else {
        return $.extend({}, this, changeText(this, text));
      }
    },
    asRange: function(root) {
      return getRangeFromTextSelection(root, this);
    }
  };
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
