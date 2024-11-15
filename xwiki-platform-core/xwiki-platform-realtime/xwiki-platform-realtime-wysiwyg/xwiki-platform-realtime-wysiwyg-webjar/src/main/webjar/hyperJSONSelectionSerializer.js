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
define('xwiki-realtime-wysiwyg-hyperJSONSelectionSerializer', [
  'hyper-json',
  'json.sortify'
], function (
  HyperJSON, JSONSortify
) {
  'use strict';

  function serializeHyperJSONSelection(contentWrapper) {
    // Collect the selection boundaries that were previously marked in the DOM.
    const selectionBoundaries = [];
    let hyperJSON = HyperJSON.fromDOM(contentWrapper, null, filterDOMSelectionMarkers(selectionBoundaries));

    // Serialize the HyperJSON content and collect the selection boundaries using the previously inserted markers.
    hyperJSON = JSONSortify(hyperJSON || []);
    hyperJSON = filterHyperJSONSelectionMarkers(hyperJSON, selectionBoundaries);

    return {
      content: hyperJSON,
      selection: selectionBoundaries
    };
  }

  /**
   * Creates a HyperJSON filter function that replaces the DOM selection markers with HyperJSON selection markers and
   * collects the selection boundaries.
   *
   * @param {object[]} selectionBoundaries an array of objects that describe the selection boundaries
   * @returns {Function} a HyperJSON filter function that injects HyperJSON selection markers based on the found DOM
   *   selection markers
   */
  function filterDOMSelectionMarkers(selectionBoundaries) {
    return element => {
      const elementName = element[0];
      const attributes = element[1];
      if (elementName.toLowerCase() === 'xwiki-comment' && attributes.value?.startsWith('selectionBoundary:')) {
        try {
          const boundary = JSON.parse(attributes.value.substring('selectionBoundary:'.length));
          // Replace the comment with a fake child node that we can look for in the serialized JSON to determine the
          // selection boundary.
          element = {'': selectionBoundaries.length};
          selectionBoundaries.push(boundary);
        } catch (error) {
          // Ignore invalid selection boundaries.
        }
      }
      return element;
    };
  }

  /**
   * Removes the HyperJSON selection markers from the given HyperJSON string and collects the selection boundaries.
   *
   * @param {string} markedHyperJSON a HyperJSON string that contains selection markers
   * @param {object[]} selectionBoundaries an array of objects that describe the selection boundaries
   * @returns the given HyperJSON string without the selection markers
   */
  function filterHyperJSONSelectionMarkers(markedHyperJSON, selectionBoundaries) {
    let hyperJSON = '', lastIndex = 0;
    markedHyperJSON.matchAll(/\{"":(\d+)\},?/g).forEach(match => {
      hyperJSON += markedHyperJSON.substring(lastIndex, match.index);
      if (hyperJSON.endsWith(',') && !match[0].endsWith(',')) {
        // Remove the trailing comma from the last item in the children array.
        hyperJSON = hyperJSON.substring(0, hyperJSON.length - 1);
      }
      lastIndex = match.index + match[0].length;
      const boundary = selectionBoundaries[parseInt(match[1])];
      // We have to add 1 to the text offset because the text starts with a quote.
      boundary.offset = hyperJSON.length + (typeof boundary.textOffset === 'number' ? 1 + boundary.textOffset : 0);
      delete boundary.textOffset;
    });
    selectionBoundaries.sort((a, b) => a.offset - b.offset);
    hyperJSON += markedHyperJSON.substring(lastIndex);
    return hyperJSON;
  }

  return serializeHyperJSONSelection;
});