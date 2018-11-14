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
define('macroEditorTranslationKeys', [], [
  'title',
  'changeMacro',
  'submit',
  'descriptorRequestFailed',
  'noParameters',
  'content',
  'more'
]);

define('macroEditor', ['jquery', 'modal', 'l10n!macroEditor'], function($, $modal, translations) {
  'use strict';
  var macroDescriptors = {},

  getMacroDescriptor = function(macroId) {
    var deferred = $.Deferred();
    var macroDescriptor = macroDescriptors[macroId];
    if (macroDescriptor) {
      deferred.resolve(macroDescriptor);
    } else {
      var url = new XWiki.Document('MacroService', 'CKEditor').getURL('get', $.param({
        outputSyntax: 'plain',
        language: $('html').attr('lang')
      }));
      $.get(url, {data: 'descriptor', macroId: macroId}).done(function(macroDescriptor) {
        if (typeof macroDescriptor === 'object' && macroDescriptor !== null) {
          macroDescriptors[macroId] = macroDescriptor;
          deferred.resolve(macroDescriptor);
        } else {
          deferred.reject.apply(deferred, arguments);
        }
      }).fail(function() {
        deferred.reject.apply(deferred, arguments);
      });
    }
    return deferred.promise();
  },

  macroEditorTemplate =
    '<div>' +
      '<div class="macro-name"/>' +
      '<div class="macro-description"/>' +
      '<ul class="macro-parameters"/>' +
    '</div>',

  createMacroEditor = function(macroCall, macroDescriptor) {
    var macroEditor = $(macroEditorTemplate);
    macroEditor.find('.macro-name').text(macroDescriptor.name);
    macroEditor.find('.macro-description').text(macroDescriptor.description);
    macroEditor.find('.macro-parameters').append(addParameterSeparator(
      sortMacroParameters(macroDescriptor, macroCall)).map(maybeDisplayMacroParameter));
    macroEditor.find('.more').click(toggleMacroParameters).click();
    this.removeClass('loading').data('macroDescriptor', macroDescriptor).append(macroEditor.children());
  },

  maybeSetParameterValue = function(parameter, macroCall) {
    var parameterCall = macroCall.parameters[parameter.id.toLowerCase()];
    // Check if the macro parameter is set.
    if (parameterCall !== null && parameterCall !== undefined) {
      parameter.value = (typeof parameterCall === 'object' && typeof parameterCall.name === 'string') ?
        parameterCall.value : parameterCall;
    }
  },

  sortMacroParameters = function(macroDescriptor, macroCall) {
    var parameter, parameters = [];
    // Add the actual macro parameters (those specified in the macro descriptor).
    if (macroDescriptor.parameterDescriptorMap) {
      for (var parameterId in macroDescriptor.parameterDescriptorMap) {
        if (macroDescriptor.parameterDescriptorMap.hasOwnProperty(parameterId)) {
          parameter = $.extend({}, macroDescriptor.parameterDescriptorMap[parameterId]);
          maybeSetParameterValue(parameter, macroCall);
          parameter.name = parameter.name || parameter.id;
          parameters.push(parameter);
        }
      }
    }
    // Handle the macro content as a special macro parameter.
    if (macroDescriptor.contentDescriptor) {
      parameter = $.extend({
        id: '$content',
        name: translations.get('content')
      }, macroDescriptor.contentDescriptor);
      if (typeof macroCall.content === 'string') {
        parameter.value = macroCall.content;
      }
      parameters.push(parameter);
    }
    parameters.sort(parameterComparator);
    return parameters;
  },

  parameterComparator = function(alice, bob) {
    if (alice.mandatory === bob.mandatory) {
      var aliceHasValue = alice.hasOwnProperty('value');
      var bobHasValue = bob.hasOwnProperty('value');
      if (aliceHasValue === bobHasValue) {
        // Macro descriptor order.
        return alice.index - bob.index;
      } else {
        // Parameters with value first.
        return bobHasValue - aliceHasValue;
      }
    } else {
      // Mandatory parameters first.
      return bob.mandatory - alice.mandatory;
    }
  },

  addParameterSeparator = function(parameters) {
    var i = 0;
    // Show the mandatory parameters and those that have been set.
    while (i < parameters.length && (parameters[i].mandatory || parameters[i].hasOwnProperty('value'))) {
      i++;
    }
    // If there are no mandatory parameters and no parameter is set then show the first three parameters.
    if (i === 0 && parameters.length > 3) {
      i = 3;
    }
    if (i > 0 && i < parameters.length) {
      // Show a 'more' link to toggle the remaining parameters.
      parameters.splice(i, 0, {id: 'more'});
    } else if (parameters.length === 0) {
      // Show a message for the empty list of parameters.
      parameters.push({id: 'empty'});
    }
    return parameters;
  },

  maybeDisplayMacroParameter = function(parameter) {
    if (parameter.name) {
      return displayMacroParameter(parameter);
    } else if (parameter.id === 'more') {
      return $('<li class="more"><span class="arrow arrow-down"/> <a href="#more"/></li>')
        .find('a').text(translations.get('more')).end();
    } else if (parameter.id === 'empty') {
      return $('<li class="empty"/>').text(translations.get('noParameters'));
    }
  },

  macroParameterTemplate =
    '<li class="macro-parameter">' +
      '<div class="macro-parameter-name"/>' +
      '<div class="macro-parameter-description"/>' +
    '</li>',

  displayMacroParameter = function(parameter) {
    var output = $(macroParameterTemplate);
    output.attr('data-id', parameter.id).attr('data-type', parameter.type);
    output.find('.macro-parameter-name').text(parameter.name);
    output.find('.macro-parameter-description').text(parameter.description);
    if (parameter.mandatory) {
      output.addClass('mandatory');
    }
    output.append(displayMacroParameterField(parameter));
    return output;
  },

  booleanValue = function(value) {
    if (typeof value === 'string') {
      return value === 'true';
    } else {
      return !!value;
    }
  },

  displayMacroParameterField = function(parameter) {
    var field;
    if (parameter.id === '$content') {
      field = $('<textarea/>');
    } else if (parameter.type === 'boolean') {
      field = $(
        '<div>' +
          '<input type="checkbox" value="true"/>' +
          '<input type="hidden" value="false"/>' +
        '</div>'
      );
      field.children('input').attr('name', parameter.id);
      var checked = booleanValue(parameter.hasOwnProperty('value') ? parameter.value : parameter.defaultValue);
      field.children('input[type=checkbox]').prop('checked', checked);
    } else if (parameter.type === 'enum') {
      field = $('<select/>');
      field.append(parameter.values.map(function(value) {
        return $('<option/>').attr('value', value.id).text(value.label);
      }));
    } else {
      field = $('<input type="text"/>');
    }
    field.addClass('macro-parameter-field').filter(':input').attr('name', parameter.id)
      .val(parameter.value || parameter.defaultValue);
    return field;
  },

  toggleMacroParameters = function(event) {
    event.preventDefault();
    var toggle = $(this);
    toggle.nextAll().toggleClass('hidden');
    var arrow = toggle.find('.arrow');
    if (arrow.hasClass('arrow-down')) {
      arrow.removeClass('arrow-down').addClass('arrow-right');
    } else {
      arrow.removeClass('arrow-right').addClass('arrow-down');
    }
  },

  maybeCreateMacroEditor = function(requestNumber, macroCall, macroDescriptor) {
    // Check if the macro descriptor corresponds to the last request.
    if (this.prop('requestNumber') === requestNumber) {
      createMacroEditor.call(this, macroCall, macroDescriptor);
      this.trigger('ready');
    }
  },

  maybeShowError = function(requestNumber) {
    // Check if the error corresponds to the last request.
    if (this.prop('requestNumber') === requestNumber) {
      this.removeClass('loading').append(
        '<div class="box errormessage">' +
          translations.get('descriptorRequestFailed', '<strong/>') +
        '</div>'
      ).find('strong').text(this.attr('data-macroId'));
    }
  },

  extractFormData = function(container) {
    var data = {};
    container.find(':input').serializeArray().forEach(function(parameter) {
      var value = data[parameter.name];
      if (value === undefined) {
        data[parameter.name] = parameter.value;
      } else if ($.isArray(value)) {
        value.push(parameter.value);
      } else {
        data[parameter.name] = [value, parameter.value];
      }
    });
    return data;
  },

  toMacroCall = function(formData, macroDescriptor) {
    if (!macroDescriptor) {
      return null;
    }
    var macroCall = {
      name: macroDescriptor.id.id,
      parameters: {}
    };
    if (typeof formData.$content === 'string' && formData.$content !== '' && macroDescriptor.contentDescriptor) {
      macroCall.content = formData.$content;
    }
    for (var parameterId in formData) {
      // The parameter descriptor map keys are lower case for easy lookup (macro parameter names are case insensitive).
      var parameterDescriptor = macroDescriptor.parameterDescriptorMap[parameterId.toLowerCase()];
      if (parameterDescriptor) {
        var value = formData[parameterId];
        if ($.isArray(value)) {
          value = parameterDescriptor.type === 'boolean' ? value[0] : value.join();
        }
        var defaultValue = parameterDescriptor.defaultValue;
        if (value !== '' && (defaultValue === undefined || defaultValue === null || (defaultValue + '') !== value)) {
          macroCall.parameters[parameterId] = value;
        }
      }
    }
    return macroCall;
  },

  createMacroEditorAPI = function(macroEditor) {
    return {
      focus: function() {
        macroEditor.find(':input').not(':hidden').first().focus();
      },
      getMacroCall: function() {
        return toMacroCall(extractFormData(macroEditor), macroEditor.data('macroDescriptor'));
      },
      validate: function() {
        var macroCall = this.getMacroCall();
        var emptyMandatoryParams = macroEditor.find('.macro-parameter.mandatory ').filter(function() {
          var id = $(this).attr('data-id');
          var value = id === '$content' ? macroCall.content : macroCall.parameters[id];
          return value === undefined;
        });
        emptyMandatoryParams.first().addClass('has-error').find(':input').not(':hidden').focus();
        setTimeout(function() {
          emptyMandatoryParams.first().removeClass('has-error');
        }, 1000);
        return emptyMandatoryParams.length === 0;
      },
      update: function(macroCall, syntaxId) {
        var macroId = macroCall.name;
        if (syntaxId) {
          macroId += '/' + syntaxId;
        }
        var requestNumber = (macroEditor.prop('requestNumber') || 0) + 1;
        macroEditor.empty().addClass('loading')
          .attr('data-macroId', macroId)
          .prop('requestNumber', requestNumber);
        getMacroDescriptor(macroId).done($.proxy(maybeCreateMacroEditor, macroEditor, requestNumber, macroCall))
          .fail($.proxy(maybeShowError, macroEditor, requestNumber));
      }
    };
  },

  editMacro = $modal.createModalStep({
    'class': 'macro-editor-modal',
    title: translations.get('title'),
    content: '<div class="macro-editor xform"/>',
    acceptLabel: translations.get('submit'),
    onLoad: function() {
      var modal = this;
      var submitButton = modal.find('.modal-footer .btn-primary');
      var changeMacroButton = $('<button type="button" class="btn btn-default"/>').text(translations.get('changeMacro'))
        .insertBefore(submitButton);
      modal.on('show.bs.modal', function(event) {
        submitButton.prop('disabled', true);
      }).on('shown.bs.modal', function(event) {
        var macroEditor = modal.find('.macro-editor');
        var macroEditorAPI = macroEditor.data('macroEditorAPI');
        var input = modal.data('input');
        var macroCall = input.macroCall || {
          name: input.macroId,
          parameters: {}
        };
        // We need to obey the specified macro identifier in case the user has just changed the macro.
        macroCall.name = input.macroId || macroCall.name;
        if (!macroEditorAPI) {
          // Initialize the macro editor.
          macroEditor.on('ready', function(event) {
            macroEditorAPI.focus();
            submitButton.prop('disabled', false);
          });
          macroEditorAPI = macroEditor.xwikiMacroEditor(macroCall, input.syntaxId);
        } else {
          macroEditorAPI.update(macroCall, input.syntaxId);
        }
      });
      submitButton.click(function(event) {
        var macroEditor = modal.find('.macro-editor');
        var macroEditorAPI = macroEditor.xwikiMacroEditor();
        if (macroEditorAPI.validate()) {
          var output = modal.data('input');
          delete output.action;
          // Preserve the in-line/block mode if possible. Note that we consider the macro in-line if no value is
          // specified, because the caret is placed in an in-line context most of the time (e.g. inside a paragraph) in
          // order to allow the user to type text).
          var inline = (!output.macroCall || output.macroCall.inline !== false) &&
            macroEditor.data('macroDescriptor').supportsInlineMode;
          output.macroCall = macroEditorAPI.getMacroCall();
          output.macroCall.inline = inline;
          modal.data('output', output).modal('hide');
        }
      });
      changeMacroButton.click(function(event) {
        var macroEditorAPI = modal.find('.macro-editor').xwikiMacroEditor();
        var output = modal.data('input');
        output.macroCall = macroEditorAPI.getMacroCall();
        output.action = 'changeMacro';
        modal.data('output', output).modal('hide');
      });
    }
  });

  $.fn.xwikiMacroEditor = function(macroCall, syntaxId) {
    this.each(function() {
      var macroEditor = $(this);
      if (!macroEditor.data('macroEditorAPI')) {
        var macroEditorAPI = createMacroEditorAPI(macroEditor);
        macroEditor.data('macroEditorAPI', macroEditorAPI);
        macroEditorAPI.update(macroCall, syntaxId);
      }
    });
    return this.data('macroEditorAPI');
  };

  return editMacro;
});
