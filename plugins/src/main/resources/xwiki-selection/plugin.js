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

  CKEDITOR.plugins.xwikiSelection = {
    saveSelection: function(editor) {
      var editable = editor.editable();
      if (editable) {
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
        // Update the tool bar state.
        editor.selectionChange();
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
})();

define('node-module', {
  load: function(name, req, onLoad, config) {
    window.module = window.module || {};
    req([name], function () {
      onLoad(window.module.exports);
    });
  }
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
