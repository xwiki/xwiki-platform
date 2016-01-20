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
CKEDITOR.plugins.add('xwiki-macro', {
  requires: 'widget',
  init : function(editor) {
    // startMacroComment: CKEDITOR.htmlParser.comment
    var getMacroOutput = function(startMacroComment) {
      var output = [];
      var parent = startMacroComment.parent;
      for (var i = startMacroComment.getIndex() + 1; i < parent.children.length; i++) {
        var child = parent.children[i];
        if (child.type === CKEDITOR.NODE_COMMENT && child.value === 'stopmacro') {
          break;
        } else {
          output.push(child);
        }
      }
      return output;
    };

    // nodes: CKEDITOR.htmlParser.node[]
    var isInline = function(nodes) {
      for (var i = 0; i < nodes.length; i++) {
        var node = nodes[i];
        if (node.type === CKEDITOR.NODE_ELEMENT && !!CKEDITOR.dtd.$block[node.name]) {
          return false;
        }
      }
      return true;
    };

    // startMacroComment: CKEDITOR.htmlParser.comment
    var wrapMacroOutput = function(startMacroComment) {
      var output = getMacroOutput(startMacroComment);
      var wrapperName = isInline(output) ? 'span' : 'div';
      var wrapper = new CKEDITOR.htmlParser.element(wrapperName, {
        'class': 'macro',
        'data-macro': startMacroComment.value
      });
      for (var i = 0; i < output.length; i++) {
        output[i].remove();
        wrapper.add(output[i]);
      }
      startMacroComment.replaceWith(wrapper);
    };

    // comment: CKEDITOR.htmlParser.comment
    var maybeWrapMacroOutput = function(comment) {
      if (comment.value.substring(0, 11) === 'startmacro:') {
        wrapMacroOutput(comment);
      } else if (comment.value === 'stopmacro') {
        comment.remove();
      }
    };

    // macroOutputWrapper: CKEDITOR.htmlParser.element
    var unWrapMacroOutput = function(macroOutputWrapper) {
      var startMacroComment = new CKEDITOR.htmlParser.comment(macroOutputWrapper.attributes['data-macro']);
      var stopMacroComment = new CKEDITOR.htmlParser.comment('stopmacro');
      var macro = new CKEDITOR.htmlParser.fragment();
      macro.add(startMacroComment);
      macro.add(stopMacroComment);
      return macro;
    };

    // content: CKEDITOR.htmlParser.fragment
    var getMacroOutputComments = function(content) {
      var macroOutputMarkers = [];
      // Note that forEach is iterating a live list, meaning that the list is updated if we remove a node from the DOM.
      // That's why we have to collect the macro output markers first and then process them.
      content.forEach(function(comment) {
        if (comment.value.substring(0, 11) === 'startmacro:' || comment.value === 'stopmacro') {
          macroOutputMarkers.push(comment);
        }
      }, CKEDITOR.NODE_COMMENT, true);
      return macroOutputMarkers;
    };

    // We didn't use the editor.dataProcessor.dataFilter because it is executed with priority 10, so after the widgets
    // are upcasted (priority 8). Only element nodes can be upcasted and wiki macro output is marked with comment nodes
    // so we need to add the macro output wrapper before the upcast takes place.
    // See http://docs.ckeditor.com/#!/api/CKEDITOR.editor-event-toHtml
    // See http://docs.ckeditor.com/#!/api/CKEDITOR.htmlDataProcessor
    // See http://docs.ckeditor.com/#!/api/CKEDITOR.htmlParser.filter
    editor.on('toHtml', function(event) {
      // dataValue is a CKEDITOR.htmlParser.fragment instance.
      getMacroOutputComments(event.data.dataValue).forEach(maybeWrapMacroOutput);
    }, null, null, 7);

    // See http://docs.ckeditor.com/#!/api/CKEDITOR.plugins.widget.definition
    editor.widgets.add('xwiki-macro', {
      requiredContent: 'div(macro)[data-macro]; span(macro)[data-macro]',
      upcast: function(element) {
        return (element.name == 'div' || element.name == 'span') &&
          element.hasClass('macro') && element.attributes['data-macro'];
      },
      downcast: unWrapMacroOutput,
      pathName: 'macro'
    });

    var macroPlugin = this;
    editor.widgets.onWidget('xwiki-macro', 'ready', function(event) {
      var macroCall = macroPlugin.parseMacroCall(this.element.getAttribute('data-macro'));
      this.pathName += ':' + macroCall.name;
      // The elementspath plugin takes the path name from the 'data-cke-display-name' attribute which is already set by
      // the widget plugin when this event is fired.
      this.wrapper.data('cke-display-name', this.pathName);
    });
  },

  parseMacroCall: function(startMacroComment) {
    /**
     * Unescapes characters escaped with the specified escape character.
     * 
     * @param text the text to be unescaped
     * @param escapeChar the character that was used for escaping
     * @return the unescaped text
     */
    var unescape = function(text, escapeChar) {
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

    // Unescape the text of the start macro comment.
    var text = unescape(startMacroComment, '\\');

    // Extract macro name.
    var separator = '|-|';
    var start = 'startmacro:'.length;
    var end = text.indexOf(separator, start);
    var macroCall = {
      name: text.substring(start, end),
      parameters: {}
    };

    // Extract the macro parameters.
    // Look for the first parameter.
    start = end + separator.length;
    var equalIndex = text.indexOf('=', start);
    var separatorIndex = text.indexOf(separator, start);
    var parameters = {};
    while (equalIndex > 0 && (separatorIndex < 0 || equalIndex < separatorIndex)) {
      var parameterName = text.substring(start, equalIndex).trim();

      // Opening quote.
      start = text.indexOf('"', equalIndex + 1) + 1;
      // Look for the closing quote.
      end = start;
      var escaped = false;
      while (escaped || text.charAt(end) !== '"') {
        escaped = !escaped && '\\' == text.charAt(end);
        end++;
      }

      macroCall.parameters[parameterName.toLowerCase()] = {
        name: parameterName,
        value: unescape(text.substring(start, end), '\\')
      };

      // Look for the next parameter.
      start = end + 1;
      equalIndex = text.indexOf('=', start);
      separatorIndex = text.indexOf(separator, start);
    }

    // Extract the macro content, if specified.
    if (separatorIndex >= 0) {
      macroCall.content = text.substring(separatorIndex + separator.length);
    }

    return macroCall;
  },

  serializeMacroCall: function(macroCall) {
    /**
     * Escapes the {@code --} sequence before setting the text of a comment DOM node.
     * 
     * @param text the text that needs to be put in a comment node
     * @return the escaped text, which will be put in a comment node
     */
    var escapeComment = function(text) {
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

    var separator = '|-|';
    var output = ['startmacro:', macroCall.name, separator];
    for (var parameterId in macroCall.parameters) {
      var parameter = macroCall.parameters[parameterId];
      // Escapes the quotes inside a macro parameter value.
      var escapedMacroParameterValue = parameter.value.replace(/([\\\"])/g, '\\$1');
      output.push(parameter.name, '="', escapedMacroParameterValue, '" ');
    }
    if (typeof macroCall.content === 'string') {
      output.push(separator, macroCall.content);
    }
    return escapeComment(output.join(''));
  }
});
