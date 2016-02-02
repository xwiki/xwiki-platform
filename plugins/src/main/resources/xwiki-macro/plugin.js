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
  CKEDITOR.plugins.add('xwiki-macro', {
    requires: 'widget,xwiki-marker',
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

      editor.plugins['xwiki-marker'].addMarkerHandler(editor, 'macro', {
        toHtml: wrapMacroOutput
      });

      // macroOutputWrapper: CKEDITOR.htmlParser.element
      var unWrapMacroOutput = function(macroOutputWrapper) {
        var startMacroComment = new CKEDITOR.htmlParser.comment(macroOutputWrapper.attributes['data-macro']);
        var stopMacroComment = new CKEDITOR.htmlParser.comment('stopmacro');
        var macro = new CKEDITOR.htmlParser.fragment();
        macro.add(startMacroComment);
        if (editor.config.fullData) {
          macroOutputWrapper.children.forEach(function(child) {
            macro.add(child);
          });
        }
        macro.add(stopMacroComment);
        return macro;
      };      

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
        // The elementspath plugin takes the path name from the 'data-cke-display-name' attribute which is already set
        // by the widget plugin when this event is fired.
        this.wrapper.data('cke-display-name', this.pathName);
      });
    },

    parseMacroCall: function(startMacroComment) {
      // Unescape the text of the start macro comment.
      var text = CKEDITOR.tools.unescapeComment(startMacroComment);

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
          value: CKEDITOR.tools.unescape(text.substring(start, end), '\\')
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
      return CKEDITOR.tools.escapeComment(output.join(''));
    }
  });
})();
