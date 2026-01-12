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
define('xwiki-realtime-wysiwyg-selection', [
  'json.sortify'
], function (
  JSONSortify
) {
  'use strict';

  function markDOMSelection(ranges) {
    if (ranges?.length) {
      // Browsers normally support a single selection range.
      markDOMRange(ranges[0]);
    }
  }

  function markDOMRange(range) {
    markDOMRangeBoundary(range.reversed ? 'End' : 'Start', range.startContainer, range.startOffset);
    markDOMRangeBoundary(range.reversed ? 'Start' : 'End', range.endContainer, range.endOffset);
  }

  function markDOMRangeBoundary(type, container, offset) {
    if (container.nodeType === Node.TEXT_NODE) {
      container.parentElement.dataset[`xwikiRealtimeSelection${type}Index`] = getDOMNodeIndex(container);
      container.parentElement.dataset[`xwikiRealtimeSelection${type}Offset`] = offset;
    } else if (container.nodeType === Node.ELEMENT_NODE) {
      container.dataset[`xwikiRealtimeSelection${type}Index`] = offset;
    }
  }

  function getDOMNodeIndex(node) {
    return Array.from(node.parentNode.childNodes).indexOf(node);
  }

  function unmarkDOMSelection(root) {
    let start = unmarkDOMRangeBoundary('Start', root);
    let end = unmarkDOMRangeBoundary('End', root);
    if (!start && !end) {
      return [];
    } else if (!start) {
      start = end;
    } else if (!end) {
      end = start;
    }
    const collapsed = start[0] === end[0] && start[1] === end[1];
    const range = root.ownerDocument.createRange();
    range.setStart(...start);
    range.setEnd(...end);
    if (range.collapsed && !collapsed) {
      range.reversed = true;
      range.setEnd(...start);
    }
    removeAllDOMSelectionMarkers(root);
    return [range];
  }

  function unmarkDOMRangeBoundary(type, root) {
    let container = root;
    const indexAttribute = `data-xwiki-realtime-selection-${type.toLowerCase()}-index`;
    if (!root.hasAttribute(indexAttribute)) {
      container = root.querySelector(`[${indexAttribute}]`);
      if (!container) {
        return;
      }
    }
    const index = Number.parseInt(container.dataset[`xwikiRealtimeSelection${type}Index`]);
    let offset = container.dataset[`xwikiRealtimeSelection${type}Offset`];
    if (typeof offset === 'string') {
      offset = Number.parseInt(offset);
      const textNode = container.childNodes[index];
      if (textNode?.nodeType === Node.TEXT_NODE) {
        container = textNode;
        // Make sure the offset is within the limits of the text node's value.
        offset = Math.min(Math.max(offset, 0), textNode.length);
        return [container, offset];
      }
    }

    // Make sure the offset is within the limits of the container's child nodes.
    offset = Math.min(Math.max(index, 0), container.childNodes.length);
    return [container, offset];
  }

  function removeAllDOMSelectionMarkers(root) {
    const markers = root.querySelectorAll([
      '[data-xwiki-realtime-selection-start-index]',
      '[data-xwiki-realtime-selection-end-index]'
    ].join(','));
    // querySelectorAll doesn't match the root element, so we handle it separately.
    for (const marker of [root, ...markers]) {
      delete marker.dataset.xwikiRealtimeSelectionStartIndex;
      delete marker.dataset.xwikiRealtimeSelectionStartOffset;
      delete marker.dataset.xwikiRealtimeSelectionEndIndex;
      delete marker.dataset.xwikiRealtimeSelectionEndOffset;
    }
  }

  function markHyperJSONSelection(hyperJSON, selectionStart, selectionEnd) {
    if (typeof hyperJSON === 'string') {
      hyperJSON = JSON.parse(hyperJSON);
    }

    let startContainer, startOffset, endContainer, endOffset;
    iterateAndSerializeHyperJSON(hyperJSON, {
      before: (node, partialHyperJSON) => {
        const offset = partialHyperJSON.length + (node.index > 0 ? 1 : 0);
        if (!startContainer && selectionStart <= offset && node.parent) {
          // The selection starts before this node.
          startContainer = node.parent;
          startOffset = node.index;
        }
        if (!endContainer && selectionEnd <= offset && node.parent) {
          // The selection ends before this node.
          endContainer = node.parent;
          endOffset = node.index;
        }
      },

      after: (node, partialHyperJSON) => {
        // jshint maxcomplexity:false
        if (node.children) {
          // Element node.
          if (!startContainer && selectionStart < partialHyperJSON.length) {
            // The selection starts inside this node, after its last child.
            startContainer = node;
            startOffset = node.children.length;
          }
          if (!endContainer && selectionEnd < partialHyperJSON.length) {
            // The selection ends inside this node, after its last child.
            endContainer = node;
            endOffset = node.children.length;
          }
        } else if (node.parent) {
          // Text node.
          // We have to remove 1 for the closing quote.
          const textStart = partialHyperJSON.length - node.value.length - 1;
          if (!startContainer && selectionStart < partialHyperJSON.length) {
            // The selection starts inside this text node.
            startContainer = node;
            startOffset = selectionStart - textStart;
          }
          if (!endContainer && selectionEnd < partialHyperJSON.length) {
            // The selection ends inside this text node.
            endContainer = node;
            endOffset = selectionEnd - textStart;
          }
        }
      }
    });

    markHyperJSONSelectionBoundary('start', startContainer, startOffset);
    markHyperJSONSelectionBoundary('end', endContainer, endOffset);

    return JSONSortify(hyperJSON);
  }

  function markHyperJSONSelectionBoundary(type, container, offset) {
    if (container) {
      if (container.attributes) {
        // Element node.
        container.attributes[`data-xwiki-realtime-selection-${type}-index`] = String(offset);
      } else {
        // Text node.
        container.parent.attributes[`data-xwiki-realtime-selection-${type}-index`] = String(container.index);
        container.parent.attributes[`data-xwiki-realtime-selection-${type}-offset`] = String(offset);
      }
    }
  }

  function unmarkHyperJSONSelection(hyperJSON) {
    if (typeof hyperJSON === 'string') {
      hyperJSON = JSON.parse(hyperJSON);
    }
    let selectionStart = 0, selectionEnd = 0;
    hyperJSON = iterateAndSerializeHyperJSON(hyperJSON, {
      before: (node, partialHyperJSON) => {
        const start = unmarkHyperJSONSelectionBoundary('start', node, partialHyperJSON);
        if (start !== undefined) {
          selectionStart = start;
        }
        const end = unmarkHyperJSONSelectionBoundary('end', node, partialHyperJSON);
        if (end !== undefined) {
          selectionEnd = end;
        }
      },
      after: (node, partialHyperJSON) => {
        if (!selectionStart && Number.isInteger(node.selection?.startIndex)) {
          // There was no child matching the start index, so we place the selection start after the last child, before
          // the closing ']]'.
          selectionStart = partialHyperJSON.length - 2;
        }
        if (!selectionEnd && Number.isInteger(node.selection?.endIndex)) {
          // There was no child matching the end index, so we place the selection end after the last child, before the
          // closing ']]'.
          selectionEnd = partialHyperJSON.length - 2;
        }
      }
    });
    return {content: hyperJSON, selectionStart, selectionEnd};
  }

  function unmarkHyperJSONSelectionBoundary(type, node, partialHyperJSON) {
    if (node.attributes) {
      // Extract the selection information so that it doesn't get serialized.
      node.selection = node.selection || {};
      node.selection[type + 'Index'] = Number.parseInt(node.attributes[`data-xwiki-realtime-selection-${type}-index`]);
      delete node.attributes[`data-xwiki-realtime-selection-${type}-index`];
      node.selection[type + 'Offset'] = Number.parseInt(
        node.attributes[`data-xwiki-realtime-selection-${type}-offset`]);
      delete node.attributes[`data-xwiki-realtime-selection-${type}-offset`];
    }
    if (node.parent?.selection[type + 'Index'] === node.index) {
      const offset = node.parent.selection[type + 'Offset'];
      if (Number.isNaN(offset) || typeof node.value !== 'string') {
        // The selection boundary is before this node. We include the child separator if this is not the first child.
        return partialHyperJSON.length + (node.index > 0 ? 1 : 0);
      } else {
        // The selection boundary is inside this text node. We include the child separator and the opening quote.
        return partialHyperJSON.length + (node.index > 0 ? 2 : 1) + offset;
      }
    }
  }

  function iterateAndSerializeHyperJSON(root, visitor) {
    let hyperJSON = '';
    iterateHyperJSON(root, {
      before: (node) => {
        visitor.before(node, hyperJSON);
        if (node.index > 0) {
          hyperJSON += ',';
        }
        if (typeof node.value === 'string') {
          hyperJSON += JSON.stringify(node.value);
        } else {
          hyperJSON += `[${JSON.stringify(node.name)},${JSONSortify(node.attributes)},[`;
        }
      },
      after: (node) => {
        if (typeof node.value !== 'string') {
          hyperJSON += ']]';
        }
        visitor.after(node, hyperJSON);
      }
    });
    return hyperJSON;
  }

  function iterateHyperJSON(root, visitor) {
    iterateHyperJSONNode(null, root, 0, visitor);
  }

  function iterateHyperJSONNode(parent, node, index, visitor) {
    if (typeof node === 'string') {
      const self = {
        value: node,
        parent,
        index,
      };
      visitor.before(self);
      visitor.after(self);
    } else if (Array.isArray(node) && node.length === 3) {
      const children = node[2];
      const self = {
        name: node[0],
        attributes: node[1],
        children,
        parent,
        index,
      };
      visitor.before(self);
      for (let i = 0; i < children.length; i++) {
        iterateHyperJSONNode(self, children[i], i, visitor);
      }
      visitor.after(self);
    }
  }

  function copyRootSelectionMarkers(source, destination) {
    if (source.nodeType === Node.ELEMENT_NODE) {
      return copyRootSelectionMarkersFromDOMToHyperJSON(source, destination);
    } else {
      copyRootSelectionMarkersFromHyperJSONToDOM(source, destination);
    }
  }

  function copyRootSelectionMarkersFromDOMToHyperJSON(root, hyperJSON) {
    try {
      hyperJSON = JSON.parse(hyperJSON);
      const attributes = hyperJSON[1];
      for (const attributeName of root.getAttributeNames()) {
        if (attributes && attributeName.startsWith('data-xwiki-realtime-selection-')) {
          attributes[attributeName] = root.getAttribute(attributeName);
        }
      }
    } catch (e) {
      console.error(e);
    }
    return hyperJSON;
  }

  function copyRootSelectionMarkersFromHyperJSONToDOM(hyperJSON, root) {
    try {
      hyperJSON = JSON.parse(hyperJSON);
      const attributes = hyperJSON[1];
      for (const attributeName in (attributes || {})) {
        if (attributeName.startsWith('data-xwiki-realtime-selection-')) {
          root.setAttribute(attributeName, attributes[attributeName]);
        }
      }
    } catch (e) {
      console.error(e);
    }
  }

  return {
    markDOMSelection,
    unmarkDOMSelection,
    markHyperJSONSelection,
    unmarkHyperJSONSelection,
    copyRootSelectionMarkers
  };
});