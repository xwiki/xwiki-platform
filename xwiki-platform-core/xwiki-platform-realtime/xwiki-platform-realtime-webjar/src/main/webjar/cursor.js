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
define('xwiki-realtime-cursor', ['rangy-core'], function (Rangy) {
  'use strict';
  var verbose = function(x) {
    if (window.verboseMode) {
      console.log(x);
    }
  };

  /**
   * @param editableElement the root DOM element of the editable area used by the real-time editor
   */
  var Cursor = function(editableElement) {
    var cursor = {};

    // There ought to be only one cursor at a time, so let's just keep it internally.
    var Range = cursor.Range = {
      start: {
        el: null,
        offset: 0
      },
      end: {
        el: null,
        offset:0
      }
    };

    /**
     * Takes notes about wherever the cursor was last seen in the event of a cursor loss, the information produced by
     * side effects of this function should be used to recover the cursor.
     *
     * @return an error string if no range is found
     */
    cursor.update = function(selection, root) {
      verbose('cursor.update');
      root = root || editableElement;
      selection = selection || Rangy.getSelection(root);

      // If the root element has no focus there will be no range.
      if (!selection.rangeCount) {
        return;
      }
      var range = selection.getRangeAt(0);

      // Big R Range is caught in closure, and maintains persistent state.
      ['start', 'end'].forEach(function (pos) {
        Range[pos].el = range[pos + 'Container'];
        Range[pos].offset = range[pos + 'Offset'];
      });
    };

    cursor.exists = function() {
      return (Range.start.el ? 1 : 0) | (Range.end.el ? 2 : 0);
    };

    /**
     * Checks if the cursor end-points are included in the given node.
     *
     * @return 0 if neither, 1 if only start, 2 if only end, 3 if both start and end are included
     */
    cursor.inNode = function(node) {
      var state = ['start', 'end'].map(function (pos, i) {
        return (
          // Text node
          node === Range[pos].el ||
          // Element node
          (node && node.contains && node.contains(Range[pos].el))
        ) ? i + 1 : 0;
      });
      return state[0] | state[1];
    };

    cursor.confineOffsetToElement = function(element, offset) {
      return Math.max(Math.min(offset, element.textContent.length), 0);
    };

    cursor.makeSelection = function() {
      return Rangy.getSelection(editableElement);
    };

    cursor.makeRange = function() {
      return Rangy.createRange();
    };

    cursor.fixStart = function(element, offset) {
      Range.start.el = element;
      Range.start.offset = cursor.confineOffsetToElement(element,
        (typeof offset !== 'undefined') ? offset : Range.start.offset);
    };

    cursor.fixEnd = function(element, offset) {
      Range.end.el = element;
      Range.end.offset = cursor.confineOffsetToElement(element,
        (typeof offset !== 'undefined') ? offset : Range.end.offset);
    };

    cursor.fixSelection = function(selection, range) {
      if (editableElement.contains(Range.start.el) && editableElement.contains(Range.end.el)) {
        var domRange = editableElement.ownerDocument.createRange();
        domRange.setStart(Range.start.el, Range.start.offset);
        domRange.setEnd(Range.end.el, Range.end.offset);

        if (domRange.compareBoundaryPoints(window.Range.END_TO_START, domRange) > 0) {
          // The range start if after the range end. We need to swap the start with the end.
          range.setStart(Range.end.el, Range.end.offset);
          range.setEnd(Range.start.el, Range.start.offset);
        } else {
          // The range start is either before or equal to the range end.
          range.setStart(Range.start.el, Range.start.offset);
          range.setEnd(Range.end.el, Range.end.offset);
        }

        // Actually set the cursor to the new range.
        selection.setSingleRange(range);
      } else {
        var error = "[cursor.fixSelection] At least one of the cursor nodes did not exist, could not fix selection";
        console.error(error);
        return error;
      }
    };

    cursor.pushDelta = function(oldVal, newVal, offset) {
      if (oldVal === newVal) {
        return;
      }

      var commonStart = 0;
      while (oldVal.charAt(commonStart) === newVal.charAt(commonStart)) {
        commonStart++;
      }

      var commonEnd = 0;
      while (oldVal.charAt(oldVal.length - 1 - commonEnd) === newVal.charAt(newVal.length - 1 - commonEnd) &&
          commonEnd + commonStart < oldVal.length && commonEnd + commonStart < newVal.length) {
        commonEnd++;
      }

      return {
        commonStart,
        commonEnd,
        // There was an insertion?
        insert: newVal.length !== commonStart + commonEnd,
        // There was a removal?
        remove: oldVal.length !== commonStart + commonEnd,
        delta: newVal.length - oldVal.length
      };
    };

    cursor.brFix = function() {
      cursor.update();
      var start = Range.start;
      var end = Range.end;
      if (!start.el) {
        return;
      }

      if (start.el === end.el && start.offset === end.offset) {
        if (start.el.tagName === 'BR') {
          var br = start.el;

          var nodeToFix = br.previousSibling || br.parentNode;

          [cursor.fixStart, cursor.fixEnd].forEach(function(fix) {
            fix(nodeToFix, 0);
          });

          cursor.fixSelection(cursor.makeSelection(), cursor.makeRange());
        }
      }
    };

    return cursor;
  };

  return Cursor;
});
