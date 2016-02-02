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
  CKEDITOR.plugins.add('xwiki-marker', {
    addMarkerHandler: function(editor, type, handler) {
      // comment: CKEDITOR.htmlParser.comment
      var toHtml = function(comment) {
        if (comment.value.substring(0, 5) === 'start') {
          handler.toHtml(comment);
        }
        if (comment.parent) {
          comment.remove();
        }
      };

      // content: CKEDITOR.htmlParser.fragment
      var getMarkers = function(content, type) {
        var markers = [];
        var startMarkerPrefix = 'start' + type + ':';
        var stopMarker = 'stop' + type;
        // Note that forEach is iterating a live list, meaning that the list is updated if we remove a node from the
        // DOM. That's why we have to collect the markers first and then process them.
        content.forEach(function(comment) {
          if (comment.value.substring(0, startMarkerPrefix.length) === startMarkerPrefix ||
              comment.value === stopMarker) {
            markers.push(comment);
          }
        }, CKEDITOR.NODE_COMMENT, true);
        return markers;
      };

      // We didn't use the editor.dataProcessor.dataFilter because it is executed with priority 10, so after the widgets
      // are upcasted (priority 8). Only element nodes can be upcasted so we need to handle the markers (which are
      // comment nodes) before the upcast takes place so that handlers can generate widgets.
      // See http://docs.ckeditor.com/#!/api/CKEDITOR.editor-event-toHtml
      // See http://docs.ckeditor.com/#!/api/CKEDITOR.htmlDataProcessor
      // See http://docs.ckeditor.com/#!/api/CKEDITOR.htmlParser.filter
      if (typeof handler.toHtml === 'function') {
        editor.on('toHtml', function(event) {
          // dataValue is a CKEDITOR.htmlParser.fragment instance.
          getMarkers(event.data.dataValue, type).forEach(toHtml);
        }, null, null, 7);
      }

      // content: CKEDITOR.htmlParser.fragment
      var getMarkedElements = function(content) {
        var markedElements = [];
        // Note that forEach is iterating a live list, meaning that the list is updated if we remove a node from the
        // DOM. That's why we have to collect the marked elements first and then process them.
        content.forEach(function(element) {
          if (handler.isMarked(element)) {
            markedElements.push(element);
          }
        }, CKEDITOR.NODE_ELEMENT, true);
        return markedElements;
      };

      if (typeof handler.toDataFormat === 'function' && typeof handler.isMarked === 'function') {
        editor.on('toDataFormat', function(event) {
          // dataValue is a CKEDITOR.htmlParser.fragment instance.
          getMarkedElements(event.data.dataValue).forEach(handler.toDataFormat);
        }, null, null, 14);
      }
    }
  });

  /**
   * Escapes the {@code --} sequence before setting the text of a comment DOM node.
   * 
   * @param text the text that needs to be put in a comment node
   * @return the escaped text, which will be put in a comment node
   */
  CKEDITOR.tools.escapeComment = function(text) {
    if (typeof text !== 'string' || text.length === 0) {
      return text;
    }
    var result = [];
    var lastChar = 0;
    for (var i = 0; i < text.length; i++) {
      var c = text.charAt(i);
      if (c === '\\') {
        // Escape the backslash (the escaping character).
        result.push('\\');
      } else if (c === '-' && lastChar === '-') {
        // Escape the second short dash.
        result.push('\\');
      }
      result.push(c);
      lastChar = c;
    }
    if (lastChar === '-') {
        // If the comment data ends with a short dash, add an escaping character.
        result.push('\\');
    }
    return result.join('');
  };

  /**
   * Unescapes characters escaped with the specified escape character.
   * 
   * @param text the text to be unescaped
   * @param escapeChar the character that was used for escaping
   * @return the unescaped text
   */
  CKEDITOR.tools.unescape = function(text, escapeChar) {
    if (typeof text !== 'string' || text.length === 0) {
      return text;
    }
    var result = [];
    var escaped = false;
    for (var i = 0; i < text.length; i++) {
      var c = text.charAt(i);
      if (!escaped && c === escapeChar) {
        escaped = true;
        continue;
      }
      result.push(c);
      escaped = false;
    }
    return result.join('');
  };

  /**
   * Unescapes the {@code --} sequence from the text of a comment DOM node.
   * 
   * @param text the text of a comment node
   * @return the unescaped text
   */
  CKEDITOR.tools.unescapeComment = function(text) {
    return CKEDITOR.tools.unescape(text, '\\');
  };
})();
