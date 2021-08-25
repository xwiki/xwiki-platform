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
define([
  'RTFrontend_treesome',
  'RTFrontend_rangy',
], function (Tree, Rangy, saveRestore) {
  'use strict';

  window.Rangy = Rangy;
  window.Tree = Tree;
  // do some function for the start and end of the cursor

  var verbose = function(x) {
    if (window.verboseMode) {
      console.log(x);
    }
  };

  /* accepts the document used by the editor */
  return function(inner) {
    var cursor = {};

    // there ought to only be one cursor at a time, so let's just
    // keep it internally
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
    cursor.update = function(sel, root) {
      verbose("cursor.update");
      root = root || inner;
      sel = sel || Rangy.getSelection(root);

      // if the root element has no focus, there will be no range
      if (!sel.rangeCount) { return; }
      var range = sel.getRangeAt(0);

      // Big R Range is caught in closure, and maintains persistent state
      ['start', 'end'].forEach(function (pos) {
        Range[pos].el = range[pos+'Container'];
        Range[pos].offset = range[pos+'Offset'];
      });
    };

    cursor.exists = function() {
      return (Range.start.el ? 1 : 0) | (Range.end.el ? 2 : 0);
    };

    /*
      0 if neither
      1 if start
      2 if end
      3 if start and end
    */
    cursor.inNode = function(el) {
      var state = ['start', 'end'].map(function (pos, i) {
        return Tree.contains(el, Range[pos].el) ? i + 1 : 0;
      });
      return state[0] | state[1];
    };

    cursor.confineOffsetToElement = function(el, offset) {
      return Math.max(Math.min(offset, el.textContent.length), 0);
    };

    cursor.makeSelection = function() {
      var sel = Rangy.getSelection(inner);
      return sel;
    };

    cursor.makeRange = function() {
      return Rangy.createRange();
    };

    cursor.fixStart = function(el, offset) {
      Range.start.el = el;
      Range.start.offset = cursor.confineOffsetToElement(el,
        (typeof offset !== 'undefined') ? offset : Range.start.offset);
    };

    cursor.fixEnd = function(el, offset) {
      Range.end.el = el;
      Range.end.offset = cursor.confineOffsetToElement(el,
        (typeof offset !== 'undefined') ? offset : Range.end.offset);
    };

    cursor.fixSelection = function(sel, range) {
      if (Tree.contains(Range.start.el, inner) && Tree.contains(Range.end.el, inner)) {
        var order = Tree.orderOfNodes(Range.start.el, Range.end.el, inner);
        var backward;

        // this could all be one line but nobody would be able to read it
        if (order === -1) {
          // definitely backward
          backward = true;
        } else if (order === 0) {
          // might be backward, check offsets to know for sure
          backward = (Range.start.offset > Range.end.offset);
        } else {
          // definitely not backward
          backward = false;
        }

        if (backward) {
          range.setStart(Range.end.el, Range.end.offset);
          range.setEnd(Range.start.el, Range.start.offset);
        } else {
          range.setStart(Range.start.el, Range.start.offset);
          range.setEnd(Range.end.el, Range.end.offset);
        }

        // actually set the cursor to the new range
        sel.setSingleRange(range);
      } else {
        var errText = "[cursor.fixSelection] At least one of the " +
          "cursor nodes did not exist, could not fix selection";
        console.error(errText);
        return errText;
      }
    };

    cursor.pushDelta = function(oldVal, newVal, offset) {
      if (oldVal === newVal) { return; }
      var commonStart = 0;
      while (oldVal.charAt(commonStart) === newVal.charAt(commonStart)) {
        commonStart++;
      }

      var commonEnd = 0;
      while (oldVal.charAt(oldVal.length - 1 - commonEnd) === newVal.charAt(newVal.length - 1 - commonEnd) &&
        commonEnd + commonStart < oldVal.length && commonEnd + commonStart < newVal.length) {
        commonEnd++;
      }

      var insert = false, remove = false;
      if (oldVal.length !== commonStart + commonEnd) {
        // there was a removal?
        remove = true;
      }
      if (newVal.length !== commonStart + commonEnd) {
        // there was an insertion?
        insert = true;
      }

      var lengthDelta = newVal.length - oldVal.length;

      return {
        commonStart: commonStart,
        commonEnd: commonEnd,
        delta: lengthDelta,
        insert: insert,
        remove: remove
      };
    };

    /* getLength assumes that both nodes exist inside of the active editor. */
    // unused currently
    cursor.getLength = function() {
      if (Range.start.el === Range.end.el) {
        return Math.abs(Range.end.offset - Range.start.offset);
      } else {
        var start, end, order = Tree.orderOfNodes(Range.start.el, Range.end.el, inner);
        if (order === 1) {
          start = Range.start;
          end = Range.end;
        } else if (order === -1) {
          start = Range.end;
          end = Range.start;
        } else {
          console.error("unexpected ordering of nodes...");
          return null;
        }
        var L = (start.el.textContent.length - start.offset);
        var cur = Tree.nextNode(start.el, inner);
        while (cur && cur !== end.el) {
          L += cur.textContent.length;
          cur = Tree.nextNode(cur, inner);
        }
        L += end.offset;
        return L;
      }
    };

    cursor.brFix = function() {
      cursor.update();
      var start = Range.start;
      var end = Range.end;
      if (!start.el) {return;}

      if (start.el === end.el && start.offset === end.offset) {
        if (start.el.tagName === 'BR') {
          var br = start.el;

          var P = (Tree.indexOfNode(br) === 0 ?
            br.parentNode: br.previousSibling);

          [cursor.fixStart, cursor.fixEnd].forEach(function(f) {
            f(P, 0);
          });

          cursor.fixSelection(cursor.makeSelection(), cursor.makeRange());
        }
      }
    };

    return cursor;
  };
});
