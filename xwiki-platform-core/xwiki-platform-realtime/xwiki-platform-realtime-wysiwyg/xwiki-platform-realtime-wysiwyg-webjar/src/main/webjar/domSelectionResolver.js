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
define('xwiki-realtime-wysiwyg-domSelectionResolver', [], function () {
  'use strict';

  /**
   * Looks for the selection markers (special comments) in the edited content and restores the selection boundaries.
   *
   * @param {Node} contentWrapper the DOM node that wraps the edited content
   * @returns {Range[]} an array of selection ranges to set on the edited content
   */
  function resolveDOMSelection(contentWrapper) {
    const ranges = {};
    getDOMSelectionBoundaries(getDOMSelectionMarkers(contentWrapper))
      .filter(boundary => boundary?.container)
      .forEach(boundary => {
        const range = ranges[boundary.rangeIndex] || (ranges[boundary.rangeIndex] = {index: boundary.rangeIndex});
        range[`${boundary.type}Container`] = boundary.container;
        range[`${boundary.type}Offset`] = boundary.offset;
      });
    return Object.values(ranges)
      .filter(range => range.startContainer && range.endContainer)
      .sort((a, b) => a.index - b.index)
      .map(range => {
        const collapsed = range.startContainer === range.endContainer && range.startOffset === range.endOffset;
        const domRange = range.startContainer.ownerDocument.createRange();
        domRange.setStart(range.startContainer, range.startOffset);
        domRange.setEnd(range.endContainer, range.endOffset);
        if (domRange.collapsed !== collapsed) {
          domRange.reversed = true;
          domRange.setStart(range.endContainer, range.endOffset);
          domRange.setEnd(range.startContainer, range.startOffset);
        }
        return domRange;
      });
  }

  /**
   * Resolve the selection boundaries from the given comment nodes (selection markers).
   *
   * @param {Comment[]} markers an array of comment nodes that are used to mark the selection boundaries
   * @returns {object[]} the resolved selection boundaries
   */
  function getDOMSelectionBoundaries(markers) {
    return markers.map(({boundary, comment}) => {
      if (typeof boundary.textOffset === 'number') {
        boundary.container = getNextSiblingTextNode(comment);
        boundary.offset = boundary.textOffset;
        delete boundary.textOffset;
      } else {
        boundary.container = comment.parentNode;
        boundary.offset = Array.from(comment.parentNode.childNodes).indexOf(comment);
      }
      comment.remove();
      return boundary;
    });
  }

  /**
   * @param {Node} node a node for which to find the next sibling text node
   * @returns {Text} the next sibling text node, or null if there is no such node
   */
  function getNextSiblingTextNode(node) {
    let sibling = node.nextSibling;
    while (sibling && sibling.nodeType !== Node.TEXT_NODE) {
      sibling = sibling.nextSibling;
    }
    return sibling;
  }

  /**
   * Collect the comment nodes that are used to mark the selection boundaries.
   *
   * @param {Node} root the root DOM node where to look for selection markers
   * @returns {Node[]} an array of comment nodes that are used to mark the selection boundaries
   */
  function getDOMSelectionMarkers(root) {
    const treeWalker = root.ownerDocument.createTreeWalker(root, NodeFilter.SHOW_COMMENT);
    const markers = [];
    while (treeWalker.nextNode()) {
      const comment = treeWalker.currentNode;
      const marker = getDOMSelectionMarker(comment);
      if (marker) {
        markers.push(marker);
      }
    }
    return markers;
  }

  function getDOMSelectionMarker(comment) {
    let value = comment.nodeValue;
    const protectedCommentPrefix = '{cke_protected}{C}';
    if (comment.nodeValue.startsWith(protectedCommentPrefix)) {
      // Protected comment. We need to decode its value.
      value = comment.nodeValue.substring(protectedCommentPrefix.length);
      value = decodeURIComponent(value);
      // Remove the comment syntax: <!-- actual value -->.
      value = value.substring(4, value.length - 3).trim();
    }
    const selectionBoundaryPrefix = 'selectionBoundary:';
    if (value.startsWith(selectionBoundaryPrefix)) {
      value = value.substring(selectionBoundaryPrefix.length);
      try {
        return {
          boundary: JSON.parse(value),
          comment
        };
      } catch (error) {
        // Ignore invalid selection boundaries.
      }
    }
  }

  return resolveDOMSelection;
});