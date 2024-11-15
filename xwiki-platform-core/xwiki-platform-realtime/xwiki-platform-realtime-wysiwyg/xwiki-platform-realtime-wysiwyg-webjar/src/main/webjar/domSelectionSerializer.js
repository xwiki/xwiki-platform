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
define('xwiki-realtime-wysiwyg-domSelectionSerializer', [
  'json.sortify'
], function (JSONSortify) {
  'use strict';

  /**
   * Inserts special HTML comments in the DOM to mark the boundaries of the specified selection.
   *
   * @param {Selection} selection the DOM selection to serialize
   * @returns the set of markers (comment nodes) that were inserted in the DOM to mark the selection boundaries
   */
  function serializeDOMSelection(selection) {
    const markers = new Set();
    selection.forEach((range, rangeIndex) => {
      markers.add(markSelectionBoundary(rangeIndex, range.reversed ? 'end' : 'start', range.startContainer,
        range.startOffset));
      markers.add(markSelectionBoundary(rangeIndex, range.reversed ? 'start' : 'end', range.endContainer,
        range.endOffset));
    });
    return markers;
  }

  /**
   * Inserts a special HTML comment to mark the specified selection boundary.
   *
   * @param {number} rangeIndex the selection range that owns the specified boundary
   * @param {string} type either 'start' or 'end' (the type of selection boundary)
   * @param {Node} container the DOM node where the selection boundary is located
   * @param {number} offset the offset within the container where the selection boundary is located
   */
  function markSelectionBoundary(rangeIndex, type, container, offset) {
    const marker = container.ownerDocument.createComment('selectionBoundary:');
    const boundary = {rangeIndex, type};
    if (container.nodeType === Node.TEXT_NODE) {
      boundary.textOffset = offset;
      container.before(marker);
    } else if (container.nodeType === Node.ELEMENT_NODE) {
      container.insertBefore(marker, container.childNodes[offset]);
    }
    marker.nodeValue += JSONSortify(boundary);
    return marker;
  }

  return serializeDOMSelection;
});