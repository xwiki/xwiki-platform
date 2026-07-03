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
      var startMarkerPrefix = 'start' + type + ':';
      var stopMarker = 'stop' + type;

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
      var getMarkers = function(content, type) {
        var markers = [];
        // Note that forEach is iterating a live list, meaning that the list is updated if we remove a node from the
        // DOM. That's why we have to collect the markers first and then process them.
        content.forEach(function(comment) {
          if (comment.value.substring(0, startMarkerPrefix.length) === startMarkerPrefix) {
            markers.push(getMarker(comment));
          }
        }, CKEDITOR.NODE_COMMENT, true);
        return markers;
      };

      // startComment: CKEDITOR.htmlParser.comment
      var getMarker = function(startComment) {
        var content = [];
        var nextSibling = startComment.next;
        // We can have nested markers as sibling nodes: start ... start ... stop ... stop
        // We need to find the corresponding stop marker, skipping the nested markers.
        var depth = 0;
        while (nextSibling && (nextSibling.type !== CKEDITOR.NODE_COMMENT || nextSibling.value !== stopMarker ||
            depth > 0)) {
          if (nextSibling.type === CKEDITOR.NODE_COMMENT) {
            if (nextSibling.value.substring(0, startMarkerPrefix.length) === startMarkerPrefix) {
              depth++;
            } else if (nextSibling.value === stopMarker) {
              depth--;
            }
          }
          content.push(nextSibling);
          nextSibling = nextSibling.next;
        }
        return {
          startComment: startComment,
          content: content,
          stopComment: nextSibling
        };
      };

      var toHtml = function(marker) {
        var remove = handler.toHtml(marker.startComment, marker.content);
        if (remove !== false) {
          if (marker.startComment.parent) {
            marker.startComment.remove();
          }
          if (marker.stopComment && marker.stopComment.parent) {
            marker.stopComment.remove();
          }
        }
      };

      if (typeof handler.toDataFormat === 'function' && typeof handler.isMarked === 'function') {
        editor.on('toDataFormat', function(event) {
          // dataValue is a CKEDITOR.htmlParser.fragment instance.
          getMarkedElements(event.data.dataValue).forEach(handler.toDataFormat);
        }, null, null, 14);
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
    }
  });

  /**
   * Escapes a single character and appends it to the result.
   *
   * @param character the character to append
   * @param lastCharacter the last character
   * @param result the output buffer to which the character should be appended
   */
  function appendEscapedCharacter(character, lastCharacter, result)
  {
    // The characters '\' and '{' always need to be escaped. The former as it is the escape character, the
    // latter because of its special meaning in XWiki syntax that would allow to, e.g., close HTML macros.
    const needsEscaping = character === '\\' || character === '{';
    // Also add an escaping between any two '-' to avoid syntax that is illegal in comments and escape after
    // '<' to avoid any matching of comment contents as HTML tags, e.g., by CKEditor.
    if (needsEscaping || (character === '-' && lastCharacter === '-') || lastCharacter === '<') {
      result.push('\\');
    }

    result.push(character);
  }

  /**
   * Escapes for insertion as text of a comment node, consistent with server-side escaping of XML comments.
   * 
   * @param text the text that needs to be put in a comment node
   * @return the escaped text, which will be put in a comment node
   */
  CKEDITOR.tools.escapeComment = function(text) {
    if (typeof text !== 'string' || text.length === 0) {
      return text;
    }
    var result = [];
    // At the start of a comment, > isn't allowed.
    if (text.charAt(0) === '>') {
      result.push('\\');
    }
    // Initialize with '-', as "->" isn't allowed at the start of the comment. It is thus better to start with
    // an escape when the comment starts with '-'.
    var lastChar = '-';
    for (var i = 0; i < text.length; i++) {
      var c = text.charAt(i);
      appendEscapedCharacter(c, lastChar, result);
      lastChar = c;
    }
    if (lastChar === '-' || lastChar === '<') {
      // If the comment data ends with '-' or '<', add an escaping character to be on the safe side.
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

  CKEDITOR.plugins.xwikiMarker = {
    /**
     * Serializes the given parameters as a space separated list of key-value pairs:
     *
     *   param1="value1" param2="value2" ...
     *
     * The input must be an object that looks like this:
     *
     *   {param1: 'value1', param2: 'value2' ... }
     *
     * or like this:
     *
     *   {
     *     param1: {name: 'Param1', value: 'value1'},
     *     param2: {name: 'Param2', value: 'value2'},
     *     ...
     *   }
     *
     * @param parameters the parameters to serialize
     */
    serializeParameters: function(parameters) {
      var output = [];
      for (var key in parameters) {
        if (parameters.hasOwnProperty(key)) {
          var parameter = parameters[key];
          if (parameter !== null && parameter !== undefined) {
            output.push(this.serializeParameter(key, parameter));
          }
        }
      }
      return output.join(' ');
    },

    serializeParameter: function(key, parameter) {
      var name = parameter.name || key;
      var value = parameter;
      if (parameter.value !== null && parameter.value !== undefined) {
        value = parameter.value;
      }
      // Escape the quotes.
      var escapedValue = value.toString().replace(/([\\"])/g, '\\$1');
      return name + '="' + escapedValue + '"';
    },

    parseParameters: function(text, parameters, start, delimiter, ignoreCase) {
      // Look for the first parameter.
      var equalIndex = text.indexOf('=', start);
      var delimiterIndex = text.indexOf(delimiter, start);
      while (equalIndex > 0 && (delimiterIndex < 0 || equalIndex < delimiterIndex)) {
        var parameterName = text.substring(start, equalIndex).trim();

        // Opening quote.
        start = text.indexOf('"', equalIndex + 1) + 1;
        // Look for the closing quote.
        var end = start;
        var escaped = false;
        while (end < text.length && (escaped || text.charAt(end) !== '"')) {
          escaped = !escaped && '\\' == text.charAt(end);
          end++;
        }

        var parameterValue = CKEDITOR.tools.unescape(text.substring(start, end), '\\');
        if (ignoreCase === true) {
          parameters[parameterName.toLowerCase()] = {
            name: parameterName,
            value: parameterValue
          };
        } else {
          parameters[parameterName] = parameterValue;
        }

        // Look for the next parameter.
        start = end + 1;
        equalIndex = text.indexOf('=', start);
        delimiterIndex = text.indexOf(delimiter, start);
      }
      return delimiterIndex;
    }
  };
})();
