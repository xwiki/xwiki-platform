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

  // Declare the configuration namespace.
  CKEDITOR.config['xwiki-macro'] = CKEDITOR.config['xwiki-macro'] || {
    __namespace: true
  };

  // Macro widget template is different depending on whether the macro is in-line or not.
  var blockMacroWidgetTemplate =
    '<div class="macro" data-macro="">' +
      '<div class="macro-placeholder">macro:name</div>' +
    '</div>';
  var inlineMacroWidgetTemplate = blockMacroWidgetTemplate.replace(/div/g, 'span');

  var nestedEditableTypeAttribute = 'data-xwiki-unchanged-content';
  var nestedEditableNameAttribute = 'data-xwiki-parameter-name';

  var selectedMacroMarker = '__cke_selected_macro';

  var withLowerCaseKeys = function(object) {
    var key, keys = Object.keys(object);
    var n = keys.length;
    var result = {};
    while (n--) {
      key = keys[n];
      result[key.toLowerCase()] = object[key];
    }
    return result;
  };

  CKEDITOR.plugins.add('xwiki-macro', {
    requires: 'widget,balloontoolbar,notification,xwiki-marker,xwiki-source,xwiki-localization',

    init: function(editor) {
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
          var text = editor.localization.get('xwiki-macro.placeholder', macroCall.name);
          placeholder.add(new CKEDITOR.htmlParser.text(text));
          wrapper.add(placeholder);
        }

        startMacroComment.replaceWith(wrapper);
      };

      editor.plugins['xwiki-marker'].addMarkerHandler(editor, 'macro', {
        toHtml: wrapMacroOutput
      });

      editor.ui.addButton('xwiki-macro', {
        label: editor.localization.get('xwiki-macro.buttonHint'),
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
        // The passed widgetElement is actually a clone of the widget element, of type CKEDITOR.htmlParser.element
        downcast: function(widgetElementClone) {
          var startMacroComment = new CKEDITOR.htmlParser.comment(widgetElementClone.attributes['data-macro']);
          var stopMacroComment = new CKEDITOR.htmlParser.comment('stopmacro');
          var macro = new CKEDITOR.htmlParser.fragment();
          macro.add(startMacroComment);
          if (editor.config.fullData) {
            widgetElementClone.children.forEach(function(child) {
              macro.add(child);
            });
          } else {
            // If the widget has nested editables we need to include them, otherwise their content is not saved.
            widgetElementClone.forEach(function(element) {
              // We look only for block-level nested editables because the in-line nested editables are not well
              // supported. See https://github.com/ckeditor/ckeditor-dev/issues/1091
              if (element.name === 'div' && element.attributes[nestedEditableTypeAttribute]) {
                macro.add(element);
                return false;
              }
            }, CKEDITOR.NODE_ELEMENT, true);
          }
          macro.add(stopMacroComment);
          return macro;
        },
        init: function() {
          // Initialize the nested editables.
          macroPlugin.initializeNestedEditables(this, editor);
          // Initialize the widget data.
          var data = macroPlugin.parseMacroCall(this.element.getAttribute('data-macro'));
          // We need to focus the macro widget after it has been inserted or updated beause the edited content has been
          // refreshed so the selection was lost.
          if (data.parameters[selectedMacroMarker]) {
            setTimeout($.proxy(this, 'scrollIntoViewAndFocus'), 0);
          }
          // Remove this meta data from the macro call so that it doesn't get saved.
          delete data.parameters[selectedMacroMarker];
          // Preserve the macro type (in-line vs. block) as much as possible when editing a macro.
          data.inline = this.inline;
          // Update the macro widget data.
          this.setData(data);
        },
        scrollIntoViewAndFocus: function() {
          this.wrapper.scrollIntoView();
          (this.editables.content || this).focus();
        },
        data: function(event) {
          this.element.setAttribute('data-macro', macroPlugin.serializeMacroCall(this.data));
          this.pathName = editor.localization.get('xwiki-macro.placeholder', this.data.name);
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
          this.showMacroWizard(this.data);
        },
        insert: function() {
          var selectedText = (editor.getSelection().getSelectedText() || '').trim();
          // Macros that produce block level content shouldn't be inserted inline. But since the user hasn't picked the
          // macro yet we can only assume that if he selected multiple lines of text then he probably wants to insert a
          // block level macro. This is a temporary solution until we fix the rendering to stop generating invalid HTML.
          // See XRENDERING-517: Invalid HTML generated when a macro that is called inline produces block level content
          var inline = selectedText.indexOf('\n') > 0 ? false : this.inline;
          this.showMacroWizard({
            parameters: {},
            // Prefill the macro content text area with the selected text.
            content: selectedText,
            inline: inline
          });
        },
        showMacroWizard: function(macroCall) {
          var widget = this;
          var nestedEditableTypes = (editor.config['xwiki-macro'] || {}).nestedEditableTypes || {};
          var input = {
            macroCall: macroCall,
            hiddenMacroParameterTypes: Object.keys(nestedEditableTypes)
          };
          // Show our custom insert/edit dialog.
          require(['macroWizard'], function(macroWizard) {
            macroWizard(input).done(function(data) {
              macroPlugin.insertOrUpdateMacroWidget(editor, data, widget);
            });
          });
        }
      });

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
            editor.showNotification(editor.localization.get('xwiki-macro.refreshFailed'), 'warning');
          }
          editor.fire('afterCommandExec', {name: this.name, command: this});
        }
      });

      // Register the dedicated insert macro buttons.
      ((editor.config['xwiki-macro'] || {}).insertButtons || []).forEach(function(definition) {
        macroPlugin.maybeRegisterDedicatedInsertMacroButton(editor, definition);
      });
    },

    // Setup the balloon tool bar for the nested editables, after the balloontoolbar plugin has been fully initialized.
    afterInit: function(editor) {
      // When the selection is inside a nested editable the macro widget button will insert a new macro so we need
      // another button to edit the current macro widget that holds the nested editable.
      editor.addCommand('xwiki-macro-edit', {
        // We want to enable the command only when the selection is inside a nested editable.
        context: true,
        contextSensitive: true,
        startDisabled: true,
        exec: function(editor) {
          this.getCurrentWidget(editor).edit();
        },
        refresh: function(editor, path) {
          // The nested editables are not marked as focused right away in Chrome so we need to defer the refresh a bit.
          setTimeout($.proxy(this, '_refresh', editor, path), 0);
        },
        _refresh: function(editor, path) {
          var currentWidget = this.getCurrentWidget(editor);
          if (currentWidget && currentWidget.name === 'xwiki-macro') {
            this.enable();
          } else {
            this.disable();
          }
        },
        getCurrentWidget: function(editor) {
          return editor.widgets.focused || editor.widgets.widgetHoldingFocusedEditable;
        }
      });

      editor.ui.addButton('xwiki-macro-edit', {
        label: editor.localization.get('xwiki-macro.editButtonHint'),
        command: 'xwiki-macro-edit',
        // We use a tool bar group that is not shown on the main tool bar so that this button is shown only on the
        // macro balloon tool bar.
        toolbar: 'xwiki-macro'
      });

      editor.balloonToolbars.create({
        buttons: 'xwiki-macro-edit,xwiki-macro',
        // Show the macro balloon tool bar when a nested editable is focused.
        cssSelector: 'div.macro div.xwiki-metadata-container.cke_widget_editable'
      });
    },

    insertOrUpdateMacroWidget: function(editor, data, widget) {
      // We're going to refresh the edited content after inserting / updating the macro widget so we mark the macro in
      // order to be able to select it afterwards.
      data.parameters[selectedMacroMarker] = true;
      // Prevent the editor from recording a history entry where the macro data is updated but the macro output is not
      // refreshed. The lock is removed by the call to setLoading(false) after the macro output is refreshed.
      editor.fire('lockSnapshot', {dontUpdate: true});
      var expectedElementName = data.inline ? 'span' : 'div';
      if (widget && widget.element && widget.element.getName() === expectedElementName) {
        // We have edited a macro and the macro type (inline vs. block) didn't change.
        // We can safely update the existing macro widget.
        widget.setData(data);
      } else {
        // We are either inserting a new macro or we are changing the macro type (e.g. from in-line to block). Changing
        // the macro type may require changes to the HTML structure (e.g. split the parent paragraph) in order to
        // preserve the HTML validity, so we have to replace the existing macro widget.
        this.createMacroWidget(editor, data);
      }
      // Refresh all the macros because a change in one macro can affect the output of the other macros.
      setTimeout($.proxy(editor, 'execCommand', 'xwiki-refresh'), 0);
    },

    createMacroWidget: function(editor, data) {
      var widgetDefinition = editor.widgets.registered['xwiki-macro'];
      var macroWidgetTemplate = data.inline ? inlineMacroWidgetTemplate : blockMacroWidgetTemplate;
      var element = CKEDITOR.dom.element.createFromHtml(macroWidgetTemplate);
      var wrapper = editor.widgets.wrapElement(element, widgetDefinition.name);
      // Isolate the widget in a separate DOM document fragment.
      var documentFragment = new CKEDITOR.dom.documentFragment(wrapper.getDocument());
      documentFragment.append(wrapper);
      editor.widgets.initOn(element, widgetDefinition, data);
      editor.widgets.finalizeCreation(documentFragment);
    },

    maybeRegisterDedicatedInsertMacroButton: function(editor, definition) {
      if (definition) {
        if (typeof definition === 'string') {
          definition = {
            macroCall: {
              name: definition
            }
          };
        } else if (typeof definition !== 'object' || typeof definition.macroCall !== 'object' ||
            definition.macroCall === null || typeof definition.macroCall.name !== 'string') {
          return;
        }
        definition.commandId = definition.commandId || 'xwiki-macro-' + definition.macroCall.name;
        // Macro parameter names are case-insensitive. Make all parameter names lowercase to ease the lookup.
        definition.macroCall.parameters = withLowerCaseKeys(definition.macroCall.parameters || {});
        this.registerDedicatedInsertMacroButton(editor, definition);
      }
    },

    registerDedicatedInsertMacroButton: function(editor, definition) {
      var macroPlugin = this;
      editor.addCommand(definition.commandId, {
        async: true,
        modes: {wysiwyg: 1},
        exec: function(editor) {
          var command = this;
          require(['macroWizard'], function(macroWizard) {
            macroWizard(definition.macroCall).done(function(data) {
              macroPlugin.insertOrUpdateMacroWidget(editor, data);
            }).always(function() {
              editor.fire('afterCommandExec', {name: command.name, command: command});
            });
          });
        }
      });
      editor.ui.addButton(definition.commandId, {
        label: editor.localization.get('xwiki-macro.dedicatedInsertButtonHint', definition.macroCall.name),
        command: definition.commandId,
        toolbar: 'insert,50'
      });
    },

    initializeNestedEditables: function(widget, editor) {
      var nestedEditableTypes = (editor.config['xwiki-macro'] || {}).nestedEditableTypes || {};
      // We look only for block-level nested editables because the in-line nested editables are not well supported.
      // See https://github.com/ckeditor/ckeditor-dev/issues/1091
      var nestedEditables = widget.element.find('div[' + nestedEditableTypeAttribute + ']');
      for (var i = 0; i < nestedEditables.count(); i++) {
        var nestedEditable = nestedEditables.getItem(i);
        // The specified widget can have nested widgets. Initialize only the nested editables that correspond to the
        // current widget.
        var nestedEditableOwner = nestedEditable.getAscendant(CKEDITOR.plugins.widget.isDomWidgetElement);
        if (!widget.element.equals(nestedEditableOwner)) {
          continue;
        }
        var nestedEditableName = 'content';
        var nestedEditableSelector = 'div[' + nestedEditableTypeAttribute + ']:not([' +
          nestedEditableNameAttribute + '])';
        if (nestedEditable.hasAttribute(nestedEditableNameAttribute)) {
          nestedEditableName = nestedEditable.getAttribute(nestedEditableNameAttribute);
          nestedEditableSelector = 'div[' + nestedEditableNameAttribute + '="' + nestedEditableName + '"]';
        }
        var nestedEditableType = nestedEditable.getAttribute(nestedEditableTypeAttribute);
        var nestedEditableConfig = nestedEditableTypes[nestedEditableType] || {};
        widget.initEditable(nestedEditableName, {
          selector: nestedEditableSelector,
          allowedContent: nestedEditableConfig.allowedContent,
          disallowedContent: nestedEditableConfig.disallowedContent,
          pathName: nestedEditableName
        });
      }
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
