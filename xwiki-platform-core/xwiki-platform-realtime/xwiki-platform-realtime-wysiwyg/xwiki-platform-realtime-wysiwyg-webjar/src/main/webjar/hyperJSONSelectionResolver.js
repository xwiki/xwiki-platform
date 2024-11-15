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
define('xwiki-realtime-wysiwyg-hyperJSONSelectionResolver', [
  'clarinet',
  'json.sortify'
], function (
  Clarinet, JSONSortify
) {
  'use strict';

  function resolveSelection(hyperJSON, positions) {
    const selectionBoundaries = [];
    let stack = [], previousPosition = 0, index = 0;
    const parser = Clarinet.parser();

    Object.assign(parser, {
      onerror: function(error) {
        console.error('Failed to parse JSON: ', error);
      },
  
      onvalue: function(value) {
        const currentNode = stack[stack.length - 1];
        if (currentNode?.type === 'array') {
          // Before value.
          const length = JSON.stringify(value).length;
          const start = this.position - length;
          currentNode.end = start;
          this._onPathChange();
  
          // After value.
          stack.push({
            type: 'value',
            path: currentNode.offset,
            start,
            end: this.position
          });
          this._onPathChange();
  
          stack.pop();
          currentNode.offset++;
          currentNode.end = this.position;
        } else if (currentNode?.type === 'object') {
          currentNode.end = this.position;
          this._onPathChange();
        }
      },
  
      onopenobject: function(key) {
        let start = previousPosition;
        const currentNode = stack[stack.length - 1];
        if (currentNode?.type === 'array') {
          // Before object.
          const length = ('{' + (key ? JSON.stringify(key) : '')).length;
          start = this.position - length;
          currentNode.end = start;
          this._onPathChange();
        }
        
        // Inside object.
        stack.push({
          type: 'object',
          path: currentNode?.offset,
          start,
          end: this.position,
          offset: key
        });
        this._onPathChange();      
      },
  
      onkey: function (key) {
        const currentNode = stack[stack.length - 1];
        currentNode.offset = key;
        currentNode.end = this.position;
        this._onPathChange();
      },
  
      oncloseobject: function () {
        this.onclosearray();
      },
  
      onopenarray: function () {
        let start = previousPosition;
        const currentNode = stack[stack.length - 1];
        if (currentNode?.type === 'array') {
          // Before array.
          const length = '['.length;
          start = this.position - length;
          currentNode.end = start;
          this._onPathChange();
        }
      
        // Inside the array.
        stack.push({
          type: 'array',
          path: currentNode?.offset,
          start,
          end: this.position,
          offset: 0
        });
        this._onPathChange();
      },
  
      onclosearray: function () {
        // Step out of the array.
        stack.pop();
        const currentNode = stack[stack.length - 1];
        if (currentNode?.type === 'array') {
          currentNode.offset++;
        }
        if (currentNode) {
          currentNode.end = this.position;
        }
        this._onPathChange(1);
      },
  
      onend: function () {
      },
  
      _onPathChange: function(positionDelta) {
        const currentNode = stack[stack.length - 1];
        const currentPosition = (currentNode?.end || 0) + (positionDelta || 0);
        for (; index < positions.length; index++) {
          const wantedPosition = positions[index];
          if (wantedPosition < currentPosition) {
            selectionBoundaries.push({
              path: stack.map(item => item.path),
              offset: currentNode?.type === 'value' ? (wantedPosition - (currentNode.start || 0)) : currentNode?.offset
            });
          } else {
            break;
          }
        }
        previousPosition = currentNode?.end;
      }
    });

    parser.write(JSONSortify(hyperJSON)).close();

    // Add not found positions.
    for (; index < positions.length; index++) {
      selectionBoundaries.push({
        path: [undefined],
        offset: hyperJSON.length
      });
    }

    return selectionBoundaries;
  }

  function injectSelectionMarkers(object, selection) {
    const selectionBoundaries = resolveSelection(object, selection.map(boundary => boundary.offset));
    const matches = selectionBoundaries.map(boundary => findHyperJSONNode(object, boundary.path.slice(1),
      boundary.offset));
    matches.toReversed().forEach((match, index) => {
      const data = {
        rangeIndex: selection[index].rangeIndex,
        type: selection[index].type
      };
      if (typeof match.textOffset === 'number') {
        data.textOffset = match.textOffset;
      }
      // Insert the selection boundary marker.
      const marker = ['XWIKI-COMMENT', {value: 'selectionBoundary:' + JSONSortify(data)}, []];
      const children = match.node[2];
      children.splice(match.childOffset, 0, marker);
    });
  }

  function findHyperJSONNode(parent, path, offset) {
    const children = parent[2];
    if (path.length) {
      if (path[0] !== 2) {
        // The path doesn't enter the child list. If the path is before the child list we consider that the selection is
        // at the start of the parent, otherwise if the path is after the child list we consider that the selection is
        // at the end of the parent.
        return {
          node: parent,
          childOffset: path[0] < 2 ? 0 : children.length
        };
      } else if (path.length > 1) {
        return findHyperJSONNodeInChildList(parent, path.slice(1), offset);
      } else {
        // We're at the end of the path, inside the child list. The specified offset represents the position inside the
        // child list.
        return {
          node: parent,
          childOffset: minMax(offset, 0, children.length)
        };
      }
    } else {
      // The path ends inside the parent but not inside the child list. If the offset is before the child list we
      // consider that the selection is at the start of the parent, otherwise if the offset is after the child list we
      // consider that the selection is at the end of the parent.
      return {
        node: parent,
        childOffset: offset > 2 ? children.length : 0
      };
    }
  }

  function findHyperJSONNodeInChildList(parent, path, offset) {
    const children = parent[2];
    // The path continues with one of the child nodes. We have two types of nodes: text nodes and elements. Text
    // nodes are simple strings, so we can't make a recursive call, like we do for elements which are arrays.
    const child = children[path[0]];
    if (Array.isArray(child)) {
      // The path continues inside a child element.
      return findHyperJSONNode(child, path.slice(1), offset);
    } else {
      // The path reached a leaf node, most probably a text node.
      const match = {
        node: parent,
        childOffset: minMax(path[0], 0, children.length)
      };
      if (typeof child === 'string') {
        // The path ends with a text node. Make sure the text offset is within the text content.
        match.textOffset = minMax(offset, 0, child.length);
      }
      return match;
    }
  }

  function minMax(value, min, max) {
    return Math.min(Math.max(value, min), max);
  }

  return injectSelectionMarkers;
});