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
  const $ = jQuery;

  CKEDITOR.plugins.xwikiSelection = {
    getSelection: function(editor) {
      if (editor._.cachedSelectionRanges && !this.isEditingAreaFocused(editor)) {
        // Use the cached selection ranges because the editing area doesn't have the focus.
        return editor._.cachedSelectionRanges;
      } else {
        // Convert the CKEditor selection ranges to standard DOM ranges. Note that CKEditor doesn't provide the
        // selection when the Source mode is active. In that case we take the selection directly from the plain text
        // area used for editing the source. We'll have to update this code when and if we'll add support for syntax
        // highlighting to the Source area.
        const selection = editor.getSelection();
        const domRanges = selection?.getRanges().map(range => {
          const domRange = range.startContainer.$.ownerDocument.createRange();
          domRange.setStart(range.startContainer.$, range.startOffset);
          domRange.setEnd(range.endContainer.$, range.endOffset);
          return domRange;
        }) || [];
        // We have to indicate the selection direction (e.g. the focus node/offset is before the anchor node/offset when
        // you use Shift + Left Arrow to select text), otherwise we wouldn't be able to restore it properly.
        const nativeSelection = selection?.getNative();
        const singleRange = domRanges.length === 1 && domRanges[0];
        if (singleRange && !singleRange.collapsed && selection.getType() === CKEDITOR.SELECTION_TEXT &&
            nativeSelection?.anchorNode === singleRange.endContainer &&
            nativeSelection.anchorOffset === singleRange.endOffset) {
          singleRange.reversed = true;
        }
        return domRanges;
      }
    },

    saveSelection: function(editor) {
      var editable = editor.editable();
      if (editable) {
        const domRanges = this.getSelection(editor);
        editor._.textSelection = this.textSelection.from(editable.$, domRanges);
        // Remember to also restore the focus when restoring the selection. In order to know if we have to restore the
        // focus we need to check the active element on the top level window. We can't rely on the focus state of the
        // editable area because the focus is lost when the user switches to a different browser tab or a different
        // window. This is especially important when running functional tests were we have to switch between the browser
        // tabs but we want to simulate multiple users editing.
        editor._.textSelection.restoreFocus = this.isEditingAreaFocused(editor);
      } else {
        delete editor._.textSelection;
      }
    },

    restoreSelection: function(editor, ranges) {
      var editable = editor.editable();
      var textSelection = editor._.textSelection;
      if (editable && textSelection) {
        const focus = textSelection.restoreFocus ? {preventScroll: !textSelection.contentOverwritten} : false;
        if (focus) {
          // This is mostly needed for the Source mode. For the WYSIWYG mode we take care of focusing the editable area
          // that contains the selection when we apply it.
          editable.$.focus(focus);
        }
        if (ranges) {
          // Apply the provided selection.
          this.setSelection(editor, ranges, focus);
        } else {
          // Restore the saved text selection.
          // Scroll the restored selection into view if the editor content was overwritten since we saved the selection
          // and the editing area had the focus when the selection was saved.
          const scrollIntoView = focus && !focus.preventScroll;
          textSelection.applyTo(editable.$, {
            scrollIntoView,
            applyDOMRange: domRange => {
              maybeMoveToFirstNestedEditable(editor, domRange, !focus.preventScroll);
              this.setSelection(editor, [domRange], focus);
            }
          });
        }
      }
    },

    isEditingAreaFocused: function(editor) {
      // We check if the editing area contains the active element instead of relying on the focus state because the
      // focus is lost when the user switches to a different browser tab or a different window, but the JavaScript code
      // can still interact with the editing area as if it was focused (we can update the selection after applying
      // remote changes received in a realtime editing session).
      return editor.ui.contentsElement.$.contains(document.activeElement);
    },

    setSelection: function(editor, ranges, focus) {
      if (focus && ranges.length) {
        // Updating the selection using the standard DOM API focuses the target element and also scrolls it into view.
        // This is not desirable when the selection is restored after applying a remote change in a realtime editing
        // session because it prevents the user from scrolling the content. At the same time, updating the selection
        // using CKEditor's API  temporarily steals the focus from the currently active element and gives it back, which
        // creates UI flickering. To avoid these problems we focus the target element without scrolling:
        // * the standard DOM selection API doesn't scroll the target element into view anymore because it's already
        //   focused
        // * there's no UI flickering because CKEditor doesn't have to steal the focus from the currently active element
        focusEditable(ranges[0], Object.assign({preventScroll: true}, focus));
      }
      if (this.isEditingAreaFocused(editor)) {
        // The editing area is focused so we can set the selection right away.
        delete editor._.cachedSelectionRanges;
        // Convert the standard DOM ranges to CKEditor selection ranges.
        const selection = editor.getSelection();
        selection?.selectRanges(ranges.map(range => {
          const ckRange = new CKEDITOR.dom.range(editor.editable());
          ckRange.setStart(new CKEDITOR.dom.node(range.startContainer), range.startOffset);
          ckRange.setEnd(new CKEDITOR.dom.node(range.endContainer), range.endOffset);
          return ckRange;
        }));
        const nativeSelection = selection?.getNative();
        const singleRange = ranges.length === 1 && ranges[0];
        if (singleRange && !singleRange.collapsed && singleRange.reversed && nativeSelection) {
          // CKEditor's selection API doesn't support setting the selection direction, so we have to use the native
          // selection API to set the proper selection direction.
          nativeSelection.setBaseAndExtent(singleRange.endContainer, singleRange.endOffset, singleRange.startContainer,
            singleRange.startOffset);
        }
      } else {
        // The editing area is not focused. In order to set the selection inside the editing area the CKEditor needs to
        // quickly steal the focus from the currently active element and give it back. This triggers the focus event on
        // the editor, followed immediately by the blur event, which makes some of the UI elements (like the floating
        // toolbar of the in-place editor) appear and disappear quickly causing an annoying flicker. We prevent this by
        // postponing the selection update until the editing area receives the focus.
        // Note that using directly the standard DOM selection API is not an option because it steals the focus from the
        // currently active element without giving it back, which is worse.
        editor._.cachedSelectionRanges = ranges;
      }
    }
  };

  function focusEditable(range, options) {
    let editable = range.startContainer;
    if (editable.nodeType !== Node.ELEMENT_NODE) {
      editable = editable.parentElement;
    }
    editable = editable.closest('[contenteditable]');
    editable?.focus(options);
  }

  function maybeMoveToFirstNestedEditable(editor, range, shouldMove) {
    // Check if a single element is selected by the given range.
    if (shouldMove && !range.collapsed && range.startContainer === range.endContainer &&
        range.startContainer.nodeType === Node.ELEMENT_NODE && range.endOffset - range.startOffset === 1) {
      const selectedNode = range.startContainer.childNodes[range.startOffset];
      if (selectedNode.nodeType === Node.ELEMENT_NODE) {
        // Check if the selected element is a widget wrapper.
        const widget = editor.widgets.getByElement(new CKEDITOR.dom.element(selectedNode), true);
        const firstNestedEditable = Object.values(widget?.editables)[0];
        if (firstNestedEditable) {
          // Move the selection to the first nested editable of the selected widget.
          const ckRange = new CKEDITOR.dom.range(editor.editable());
          ckRange.moveToElementEditablePosition(firstNestedEditable);
          range.setStart(ckRange.startContainer.$, ckRange.startOffset);
          range.collapse(true);
        }
      }
    }
  }

  CKEDITOR.plugins.add('xwiki-selection', {
    onLoad: function() {
      require(['textSelection'], function(textSelection) {
        CKEDITOR.plugins.xwikiSelection.textSelection = textSelection;
      });
    },

    beforeInit: function(editor) {
      editor.on('setData', () =>{
        // The selection needs to be scrolled into view when restored, if the editor content is overwritten after the
        // selection is saved.
        if (editor._.textSelection) {
          editor._.textSelection.contentOverwritten = true;
        }
      });
    },

    init: function(editor) {
      // We need to attach the focus listener whenever the editing area is recreated (for the in-place editor is happens
      // only once, but for the iframe-based editor it happens whenever we insert a rendering macro or switch to Source
      // and back). Note that we can't listen to the focus event on editor.ui.contentsElement (which wraps the editing
      // area) because for the iframe-based editor the focus event is triggered only inside the iframe (even though the
      // iframe becomes the active element on the top level window that hosts the editor).
      editor.on('contentDom', () => {
        // Capture the focus event before it reaches its target (we can't catch it otherwise because the focus event
        // doesn't bubble up) and apply the cached selection ranges before CKEditor commands are executed (e.g. after
        // submitting the insert macro modal).
        editor.editable().$.addEventListener('focus', () => {
          if (editor._.cachedSelectionRanges) {
            CKEDITOR.plugins.xwikiSelection.setSelection(editor, editor._.cachedSelectionRanges);
          }
        }, true);
      });
    }
  });
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
  function isTextInput(element) {
    return typeof element.setSelectionRange === 'function';
  }

  function getTextSelection(root, ranges) {
    if (isTextInput(root)) {
      return {
        text: root.value,
        startOffset: root.selectionStart,
        endOffset: root.selectionEnd
      };
    } else {
      // We currently support only one range.
      const range = ranges?.[0];
      if (range && root.contains(range.commonAncestorContainer)) {
        return getTextSelectionFromRange(root, range);
      }
      return {
        text: getVisibleText(root),
        startOffset: 0,
        endOffset: 0
      };
    }
  }

  function getTextSelectionFromRange(root, range) {
    const beforeRange = root.ownerDocument.createRange();
    beforeRange.setStart(root, 0);
    beforeRange.setEnd(range.startContainer, range.startOffset);
    const beforeText = getVisibleTextInRange(beforeRange);

    const selectionRange = root.ownerDocument.createRange();
    selectionRange.setStart(range.startContainer, range.startOffset);
    selectionRange.setEnd(range.endContainer, range.endOffset);
    const lastSelectedNode = range.endContainer.childNodes[range.endOffset - 1];
    if (!range.collapsed && lastSelectedNode?.nodeType === Node.ELEMENT_NODE) {
      // Place the selection range end inside the last selected element, after its last child node, in order to exclude
      // the end separator associated with the last selected node from the selected text. We need this to be able to
      // represent the fact that the selection ends after the last selected element (if that element is focusable) and
      // not at the start of the next focusable element or visible text node.
      selectionRange.setEnd(lastSelectedNode, lastSelectedNode.childNodes.length);
    }
    const selectedText = getVisibleTextInRange(selectionRange);

    const afterRange = root.ownerDocument.createRange();
    afterRange.setStart(selectionRange.endContainer, selectionRange.endOffset);
    afterRange.setEnd(root, root.childNodes.length);
    const afterText = getVisibleTextInRange(afterRange);

    const startOffset = beforeText.length;
    return {
      text: beforeText + selectedText + afterText,
      startOffset,
      endOffset: startOffset + selectedText.length,
      // Remember the text selection direction. This is especially important when editing in realtime because the user
      // might be selecting text backwards (e.g. using Shift + Left Arrow) when a remove change is applied.
      reversed: range.reversed
    };
  }

  function getVisibleText(node) {
    const range = node.ownerDocument.createRange();
    range.selectNodeContents(node);
    return getVisibleTextInRange(range);
  }

  function getVisibleTextInRange(range) {
    // Partial text nodes are included in the iteration so we have to remember to remove the text before the start
    // offset and after the end offset.
    let visibleText = '', removeStart = 0, removeEnd = 0;
    iterateRangeWithCaret(range, {
      before: node => {
        if (node.nodeType === Node.TEXT_NODE) {
          visibleText += node.nodeValue;
          if (node === range.startContainer) {
            removeStart = range.startOffset;
          }
        } else {
          visibleText += '\n';
        }
      },
      after: node => {
        visibleText += '\n';
        if (node.nodeType === Node.TEXT_NODE && node === range.endContainer) {
          // Note that we add 1 to take into account the separator (new line) added after each text fragment.
          removeEnd = node.length - range.endOffset + 1;
        }
      }
    });

    return visibleText.substring(removeStart, visibleText.length - removeEnd);
  }

  function iterateRangeWithCaret(range, visitor) {
    // We're going to iterate the given range using a clone, by moving the start until the clone collapses.
    const caret = range.cloneRange();

    // Include partial text nodes in the iterating range because it simplifies the iteration.
    if (range.startContainer.nodeType !== Node.ELEMENT_NODE) {
      caret.setStartBefore(range.startContainer);
    }
    if (range.endContainer.nodeType !== Node.ELEMENT_NODE) {
      caret.setEndAfter(range.endContainer);
    }

    while (!caret.collapsed) {
      let found, child = caret.startContainer.childNodes[caret.startOffset];
      if (!child) {
        // We reached the end of start container. Check if the user can place the caret after this element.
        found = maybeVisitAfterElement(visitor, caret.startContainer);
        caret.setStartAfter(caret.startContainer);
      } else if (!isNodeVisible(child)) {
        // Skip hidden nodes.
        caret.setStartAfter(child);
      } else if (child.nodeType === Node.ELEMENT_NODE) {
        // Step inside.
        caret.setStart(child, 0);
        // Check if the user can place the caret before this element.
        found = maybeVisitBeforeElement(visitor, child);
      } else {
        // Step over.
        caret.setStartAfter(child);
        // The user can place the caret before and after a visible text node.
        found = maybeVisitText(visitor, child);
      }
      if (found) {
        return found;
      }
    }
  }

  function isNodeVisible(node) {
    if (node.nodeType === Node.TEXT_NODE) {
      const range = node.ownerDocument.createRange();
      range.selectNode(node);
      return range.getClientRects().length;
    } else if (node.nodeType === Node.ELEMENT_NODE) {
      return node.offsetWidth || node.offsetHeight ||
        [...node.getClientRects()].some(rect => rect.width || rect.height);
    } else {
      return false;
    }
  }

  const maybeVisitText = (visitor, node) => node.nodeType === Node.TEXT_NODE &&
    (visitor.before(node) || visitor.after(node));
  const maybeVisitBeforeElement = (visitor, element) => canPlaceCaretBeforeElement(element) && visitor.before(element);
  const maybeVisitAfterElement = (visitor, element) => canPlaceCaretAfterElement(element) && visitor.after(element);

  function canPlaceCaretBeforeElement(element) {
    return element.hasAttribute('tabindex') || (element.nodeName === 'BR' &&
      (!element.previousSibling || element.previousSibling.nodeName === 'BR'));
  }

  function canPlaceCaretAfterElement(element) {
    return isNodeVisible(element) && element.hasAttribute('tabindex');
  }

  function getRangeFromTextSelection(root, textSelection) {
    const start = findTextOffsetInDOM(root, textSelection.startOffset);
    const end = textSelection.endOffset === textSelection.startOffset ? start :
      findTextOffsetInDOM(root, textSelection.endOffset);
    const range = root.ownerDocument.createRange();
    range.setStart(start.node, start.offset);
    range.setEnd(end.node, end.offset);
    // Preserve the text selection direction.
    range.reversed = textSelection.reversed;
    return range;
  }

  function findTextOffsetInDOM(root, offset) {
    // We use a range to iterate the caret positions inside the DOM subtree with the given root.
    const range = root.ownerDocument.createRange();
    range.selectNodeContents(root);
    // We count the number of characters (caret positions) until we reach the given offset. We need to remember the last
    // visited node (that can hold the caret) in case the given offset is greater than the number of characters (caret
    // positions) in the given DOM subtree.
    let count = 0, lastNode;
    // Places the caret where the given node starts.
    const caretBeforeNode = node => node.nodeType === Node.TEXT_NODE ? {node, offset: 0} : {
      node: node.parentNode,
      offset: [...node.parentNode.childNodes].indexOf(node)
    };
    const caretAfterNode = node => node.nodeType === Node.TEXT_NODE ? {node, offset: node.length} : {
      node: node.parentNode,
      offset: [...node.parentNode.childNodes].indexOf(node) + 1
    };
    const caretInsideOrAfterNode = node => node.nodeType === Node.TEXT_NODE ? {
      node,
      offset: offset - (count - node.length)
    } : caretAfterNode(node);
    return iterateRangeWithCaret(range, {
      before: node => {
        if (count >= offset) {
          return caretBeforeNode(node);
        }
        // We replicate the behavior from getVisibleTextInRange: we count the number of characters for text nodes and
        // for focusable elements we count 1 (as if that element was an image and we can jump over it by pressing the 
        // arrow key once).
        count += node.nodeType === Node.TEXT_NODE ? node.length : 1;
      },
      after: node => {
        if (count >= offset) {
          return caretInsideOrAfterNode(node);
        }
        // We count one to be able to distinguish between the end of the last node and the start of the next node.
        count++;
        lastNode = node;
      }
    // Use the last visited node if the given offset is greater than the number of characters (caret positions).
    }) || (lastNode && caretAfterNode(lastNode)) || {
    // Use the root node if there were no nodes visited (either the root node is empty or there are no visible child
    // nodes that can hold the caret).
      node: root,
      offset: offset > 0 ? root.childNodes.length : 0
    };
  }

  function changeText(textSelection, newText) {
    const changes = diff(textSelection.text, newText);
    const startOffset = findTextOffsetInChanges(changes, textSelection.startOffset);
    const endOffset = textSelection.endOffset === textSelection.startOffset ? startOffset :
      findTextOffsetInChanges(changes, textSelection.endOffset);
    return {
      text: newText,
      startOffset,
      endOffset
    };
  }

  function findTextOffsetInChanges(changes, oldOffset) {
    let count = 0, newOffset = oldOffset;
    for (let i = 0; i < changes.length && count < oldOffset; i++) {
      const change = changes[i];
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
  }

  function scrollSelectionIntoView(element, range) {
    if (isTextInput(element)) {
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
    } else {
      scrollUtils.centerVertically(getScrollTarget(range), 65);
    }
  }

  function getScrollTarget(range) {
    // Try the child node at the specified offset, or the last child if the offset is greater than the number of
    // children, or the node itself if it doesn't have child nodes (e.g. if it's a text node).
    let target = range.startContainer.childNodes[range.startOffset] || range.startContainer.lastChild ||
      range.startContainer;
    if (target.nodeType !== Node.ELEMENT_NODE) {
      target = target.previousElementSibling || target.parentNode;
    }
    return target;
  }

  return {
    from: function(root, ranges) {
      return $.extend({}, this, getTextSelection(root, ranges));
    },

    applyTo: function(element, options) {
      options = Object.assign({
        scrollIntoView: true,
        applyDOMRange: range => {
          const selection = element.ownerDocument.defaultView.getSelection();
          if (range.reversed) {
            selection.setBaseAndExtent(range.endContainer, range.endOffset, range.startContainer, range.startOffset);
          } else {
            selection.setBaseAndExtent(range.startContainer, range.startOffset, range.endContainer, range.endOffset);
          }
        }
      }, options);

      var range;
      if (isTextInput(element)) {
        range = this.withText(element.value);
      } else {
        range = this.withText(getVisibleText(element)).asRange(element);
      }

      if (options.scrollIntoView) {
        scrollSelectionIntoView(element, range);
      }

      if (isTextInput(element)) {
        element.setSelectionRange(range.startOffset, range.endOffset);
      } else {
        options.applyDOMRange(range);
      }
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
