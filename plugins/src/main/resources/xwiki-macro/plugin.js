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
  var $ = jQuery;
  CKEDITOR.plugins.add('xwiki-macro', {
    requires: 'widget,notification,xwiki-marker,xwiki-source',
    init : function(editor) {
      var macroPlugin = this;

      // node: CKEDITOR.htmlParser.node
      var isInlineNode = function(node) {
        return node.type !== CKEDITOR.NODE_ELEMENT || CKEDITOR.dtd.$inline[node.name];
      };

      // node: CKEDITOR.htmlParser.node
      var getPreviousSibling = function(node, siblingTypes) {
        var i = node.getIndex() - 1;
        while (i >= 0) {
          var sibling = node.parent.children[i--];
          if (!siblingTypes || siblingTypes.indexOf(sibling.type) >= 0) {
            return sibling;
          }
        }
        return null;
      };

      // node: CKEDITOR.htmlParser.node
      var getNextSibling = function(node, siblingTypes) {
        var i = node.getIndex() + 1;
        while (i < node.parent.children.length) {
          var sibling = node.parent.children[i++];
          if (!siblingTypes || siblingTypes.indexOf(sibling.type) >= 0) {
            return sibling;
          }
        }
        return null;
      };

      // startMacroComment: CKEDITOR.htmlParser.comment
      // output: CKEDITOR.htmlParser.node[]
      var isInlineMacro = function(startMacroComment, output) {
        if (output.length > 0) {
          var i = 0;
          while (i < output.length && isInlineNode(output[i])) {
            i++;
          }
          return i >= output.length;
        } else {
          var previousSibling = getPreviousSibling(startMacroComment, [CKEDITOR.NODE_ELEMENT, CKEDITOR.NODE_TEXT]);
          var nextSibling = getNextSibling(startMacroComment, [CKEDITOR.NODE_ELEMENT, CKEDITOR.NODE_TEXT]);
          if (previousSibling || nextSibling) {
            return (previousSibling && isInlineNode(previousSibling)) || (nextSibling && isInlineNode(nextSibling));
          } else {
            return !CKEDITOR.dtd.$blockLimit[startMacroComment.parent.name];
          }
        }
      };

      // startMacroComment: CKEDITOR.htmlParser.comment
      // output: CKEDITOR.htmlParser.node[]
      var wrapMacroOutput = function(startMacroComment, output) {
        var wrapperName = isInlineMacro(startMacroComment, output) ? 'span' : 'div';
        var wrapper = new CKEDITOR.htmlParser.element(wrapperName, {
          'class': 'macro',
          'data-macro': startMacroComment.value
        });
        if (output.length > 0) {
          for (var i = 0; i < output.length; i++) {
            output[i].remove();
            wrapper.add(output[i]);
          }
        } else {
          // Use a placeholder for the macro output. The user cannot edit the macro otherwise.
          var placeholder = new CKEDITOR.htmlParser.element(wrapperName, {'class': 'macro-placeholder'});
          var macroCall = macroPlugin.parseMacroCall(startMacroComment.value);
          placeholder.add(new CKEDITOR.htmlParser.text('macro:' + macroCall.name));
          wrapper.add(placeholder);
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

      editor.ui.addButton('xwiki-macro', {
        label: 'XWiki Macro',
        command: 'xwiki-macro',
        toolbar: 'insert,40'
      });

      // See http://docs.ckeditor.com/#!/api/CKEDITOR.plugins.widget.definition
      editor.widgets.add('xwiki-macro', {
        requiredContent: 'div(macro,macro-placeholder)[data-macro];span(macro,macro-placeholder)[data-macro]',
        pathName: 'macro',
        upcast: function(element) {
          return (element.name == 'div' || element.name == 'span') &&
            element.hasClass('macro') && element.attributes['data-macro'];
        },
        downcast: unWrapMacroOutput,
        init: function() {
          var data = macroPlugin.parseMacroCall(this.element.getAttribute('data-macro'));
          // Preserve the macro type (in-line vs. block) as much as possible when editing a macro.
          data.inline = this.inline;
          this.setData(data);
        },
        data: function(event) {
          this.element.setAttribute('data-macro', macroPlugin.serializeMacroCall(this.data));
          this.pathName = 'macro:' + this.data.name;
          // The elementspath plugin takes the path name from the 'data-cke-display-name' attribute which is set by the
          // widget plugin only once, based on the initial pathName property from the widget definition. We have to
          // update the attribute ourselves in order to have a dynamic path name (since the user can change the macro).
          this.wrapper.data('cke-display-name', this.pathName);
          $(this.element.$).find('.macro-placeholder').text(this.pathName);
        },
        edit: function(event) {
          // Prevent the default behavior because we want to use our custom dialog.
          event.cancel();
          // Our custom edit dialog allows the user to change the macro, which means the user can change from an in-line
          // macro to a block macro (or the other way around). As a consequence we may have to replace the existing
          // macro widget (in-line widgets and block widgets are handled differently by the editor).
          this.insert();
        },
        insert: function() {
          var widget = this;
          // Show our custom insert/edit dialog.
          require(['macroWizard'], function(macroWizard) {
            macroWizard(widget.data).done(function(data) {
              // Prevent the editor from recording a history entry where the macro data is updated but the macro
              // output is not refreshed. The lock is removed by the call to setLoading(false) after the macro output
              // is refreshed.
              editor.fire('lockSnapshot', {dontUpdate: true});
              var expectedElementName = data.inline ? 'span' : 'div';
              if (widget.element && widget.element.getName() === expectedElementName) {
                // We have edited a macro and the macro type (inline vs. block) didn't change.
                // We can safely update the existing macro widget.
                widget.setData(data);
              } else {
                // We are either inserting a new macro or we are changing the macro type (e.g. from in-line to block).
                // Changing the macro type may require changes to the HTML structure (e.g. split the parent paragraph)
                // in order to preserve the HTML validity, so we have to replace the existing macro widget.
                createMacroWidget(data);
              }
              // Refresh all the macros because a change in one macro can affect the output of the other macros.
              setTimeout($.proxy(editor, 'execCommand', 'xwiki-refresh'), 0);
            });
          });
        }
      });

      // Macro widget template is different depending on whether the macro is in-line or not.
      var blockMacroWidgetTemplate =
        '<div class="macro" data-macro="">' +
          '<div class="macro-placeholder">macro:name</div>' +
        '</div>';
      var inlineMacroWidgetTemplate = blockMacroWidgetTemplate.replace(/div/g, 'span');
      var createMacroWidget = function(data) {
        var widgetDefinition = editor.widgets.registered['xwiki-macro'];
        var macroWidgetTemplate = data.inline ? inlineMacroWidgetTemplate : blockMacroWidgetTemplate;
        var element = CKEDITOR.dom.element.createFromHtml(macroWidgetTemplate);
        var wrapper = editor.widgets.wrapElement(element, widgetDefinition.name);
        // Isolate the widget in a separate DOM document fragment.
        var documentFragment = new CKEDITOR.dom.documentFragment(wrapper.getDocument());
        documentFragment.append(wrapper);
        editor.widgets.initOn(element, widgetDefinition, data);
        editor.widgets.finalizeCreation(documentFragment);
      };

      editor.addCommand('xwiki-refresh', {
        async: true,
        exec: function(editor) {
          var command = this;
          editor.plugins['xwiki-source'].setLoading(editor, true);
          var config = editor.config['xwiki-source'] || {};
          $.post(config.htmlConverter, {
            fromHTML: true,
            toHTML: true,
            text: editor.getData()
          }).done(function(html) {
            editor.setData(html, {callback: $.proxy(command, 'done', true)});
          }).fail($.proxy(this, 'done'));
        },
        done: function(success) {
          editor.plugins['xwiki-source'].setLoading(editor, false);
          if (!success) {
            editor.showNotification('Failed to refresh the edited content.', 'warning');
          }
          editor.fire('afterCommandExec', {name: this.name, command: this});
        }
      });
    },

    parseMacroCall: function(startMacroComment) {
      // Unescape the text of the start macro comment.
      var text = CKEDITOR.tools.unescapeComment(startMacroComment);

      // Extract the macro name.
      var separator = '|-|';
      var start = 'startmacro:'.length;
      var end = text.indexOf(separator, start);
      var macroCall = {
        name: text.substring(start, end),
        parameters: {}
      };

      // Extract the macro parameters.
      start = end + separator.length;
      var separatorIndex = CKEDITOR.plugins.xwikiMarker.parseParameters(text, macroCall.parameters,
        start, separator, true);

      // Extract the macro content, if specified.
      if (separatorIndex >= 0) {
        macroCall.content = text.substring(separatorIndex + separator.length);
      }

      return macroCall;
    },

    serializeMacroCall: function(macroCall) {
      var separator = '|-|';
      var output = ['startmacro:', macroCall.name, separator,
        CKEDITOR.plugins.xwikiMarker.serializeParameters(macroCall.parameters)];
      if (typeof macroCall.content === 'string') {
        output.push(separator, macroCall.content);
      }
      return CKEDITOR.tools.escapeComment(output.join(''));
    }
  });
})();
