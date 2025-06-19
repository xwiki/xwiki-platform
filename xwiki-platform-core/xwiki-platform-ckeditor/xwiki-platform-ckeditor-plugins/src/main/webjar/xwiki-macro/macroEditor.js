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
  'parametersRequestFailed',
  'installRequestFailed',
  'noParameters',
  'content',
  'required',
  'selectFeature'
]);

/**
 * Macro Parameter Tree Sorter
 */
define('macroParameterEnhancer', ['jquery'], function($) {
  'use strict';

  let enhanceMacroParameters = function(macroDescriptor, macroCall, macroParameters) {
    for (let parameterId in macroDescriptor.parametersMap) {
      let parameter = macroDescriptor.parametersMap[parameterId];
      maybeSetParameterValue(parameter, macroCall, macroParameters);
      maybeHideParameter(parameter);
    }
  },

  maybeSetParameterValue = function(parameter, macroCall, macroParameters) {
    let parameterCall = macroCall.parameters[parameter.id.toLowerCase()];
    let macroParameterValue = macroParameters[parameter.id.toLowerCase()];
    if (macroParameterValue) {
      parameterCall = macroParameterValue;
    } else if (parameter.id.toLowerCase() === '$content') {
      parameterCall = macroCall.content;
    }
    // Check if the macro parameter is set.
    if (parameterCall !== null && parameterCall !== undefined) {
      parameter.value = (typeof parameterCall === 'object' && typeof parameterCall.name === 'string') ?
        parameterCall.value : parameterCall;
    }
  },

  /**
   * The following macro parameters should be hidden:
   * - parameters marked as "display hidden" in the descriptor and not having any value set
   * - deprecated parameters that don't have a value set
   */
  maybeHideParameter = function(parameter) {
    // We can't hide mandatory parameters that don't have a value set.
    parameter.hidden = isHiddenParameterAndNoValue(parameter) || isDeprecatedParameterUnset(parameter);
  },

  isHiddenParameterAndNoValue = function(macroParameter) {
    // Don't display parameters that are marked hidden in the macro descriptor, unless they have a value set in which
    // case we need to display it so that the user can see it/modify it.
    return macroParameter.hidden && macroParameter.value === undefined;
  },

  isDeprecatedParameterUnset = function(macroParameter) {
    return macroParameter.deprecated && macroParameter.value === undefined;
  };

  return {
    enhance: enhanceMacroParameters
  };
});

/**
 * Macro Parameter Tree Displayer
 */
define('macroParameterTreeDisplayer', ['jquery', 'l10n!macroEditor'], function($, translations) {
  'use strict';

  let displayMacroParameterTree = function(macroParameterTree, requiredSkinExtensions) {
    let output = $('<div></div>');
    if (macroParameterTree.mandatoryNodes.length < 1 && macroParameterTree.optionalNodes.length < 1) {
      output.append($('<div class="empty"></div>').text(translations.get('noParameters')));
    } else {
      let parametersMap = macroParameterTree.parametersMap;
      output.append(macroParameterTree.mandatoryNodes.map(key => {
        let node = parametersMap[key];
        return displayMacroParameterTreeNode(parametersMap, node);
      }));
      output.append(displayOptionalNodes(parametersMap, macroParameterTree.optionalNodes));
    }
    if (macroParameterTree.mandatoryNodes.length < 1) {
      output.find('.nav-tabs').empty();
    }

    output.find('a[role="tab"]').on('click', function(event) {
      event.preventDefault();
      $(this).tab('show');
    });
    // Load the pickers.
    $(document).loadRequiredSkinExtensions(requiredSkinExtensions);
    return output.children();
  },

  displayMacroParameterTreeNode = function(parametersMap, node) {
    switch (node.type) {
      case 'GROUP': return displayGroup(parametersMap, node);
      case 'PARAMETER': return displayMacroParameter(node);
    }
  },

  macroParameterGroupOptionalsTemplate =
      '<div class="macro-parameter-group-optionals single-choice">' +
        '<ul class="nav nav-tabs" role="tablist">' +
          '<li class="active" role="presentation">' +
            '<a class="macro-parameter-group-name" href="#groupId" aria-controls="groupId" role="tab"></a>' +
          '</li>' +
        '</ul>' +
        '<div class="tab-content">' +
          '<div id="groupId" class="macro-parameter-group-members tab-pane active" role="tabpanel"></div>' +
        '</div>' +
      '</div>',

  displayOptionalNodes = function(parametersMap, optionalNodeList) {
    let output = $(macroParameterGroupOptionalsTemplate);
    let tabs = output.find('ul[role="tablist"]');
    let tabTemplate = tabs.children().remove();
    let tabPanels = output.find('.tab-content');
    let tabPanelTemplate = tabPanels.children().remove();
    optionalNodeList.forEach(function(nodeKey, index) {
      let childNode = parametersMap[nodeKey];
      let tab = tabTemplate.clone().appendTo(tabs);
      let tabPanel = tabPanelTemplate.clone().appendTo(tabPanels);
      // Some of the child nodes might be hidden so we will activate the first visible tab at the end.
      tab.add(tabPanel).removeClass('active').toggleClass('hidden', !!childNode.hidden);
      fillNodeTab(parametersMap, childNode, tab.children().first(), tabPanel);
    });
    // Activate the first visible tab.
    let activeTab = tabs.children().not('.hidden').first();
    let activeTabPanel = tabPanels.children().not('.hidden').first();
    activeTab.add(activeTabPanel).addClass('active');
    toggleMacroParameterGroupVisibility(output);
    // we remove all occurrences of feature title since we already have the tabs name.
    output.find('.feature-container .panel-heading').remove();
    return output;
  },

  displayGroup = function(parametersMap, groupNode) {
    return displayFeature(parametersMap, groupNode, groupNode.featureOnly);
  },

  macroFeatureContainerTemplate =
    '<div class="feature-container panel panel-default" id="feature-{{featureName}}">' +
      '<div class="panel-heading">' +
        '<span class="feature-title">{{featureTitle}}</span>' +
        '<span class="mandatory"></span>' +
      '</div>' +
      '<div class="panel-body"></div>' +
    '</div>',

  macroFeatureContentTemplate =
    '<div class="feature-parameter">' +
      '<div class="feature-choice">' +
        '<input type="radio" class="feature-radio" id="feature-radio-{{featureName}}-{{parameterId}}"' +
      ' name="feature-radio-{{featureName}}" value="{{parameterKey}}" />' +
        '<label class="feature-choice-name" for="feature-radio-{{featureName}}-{{parameterId}}">' +
          '{{parameterLabel}}' +
        '</label>' +
      '</div>' +
      '<div class="feature-choice-body"></div>' +
    '</div>',

  displayFeature = function (parametersMap, featureNode, isFeature) {
    let name = (isFeature) ? featureNode.featureName : featureNode.name;
    let output = $(macroFeatureContainerTemplate
        .replaceAll("{{featureName}}", featureNode.id)
        .replaceAll("{{featureTitle}}", name));
    if (featureNode.mandatory) {
      output.find('.mandatory').text('(' + translations.get('required') + ')');
      output.addClass('mandatory');
    }
    output.find('.panel-body').append(featureNode.children.map(nodeKey => {
      let paramNode = parametersMap[nodeKey];
      let nodeOutput = $(macroFeatureContentTemplate
          .replaceAll("{{featureName}}", featureNode.id)
          .replaceAll("{{parameterKey}}", nodeKey)
          .replaceAll("{{parameterId}}", paramNode.id)
          .replaceAll("{{parameterLabel}}", translations.get('selectFeature', paramNode.name)));
      if (isFeature && featureNode.mandatory) {
        nodeOutput.find('.feature-radio').on('change', function() {
          $(this).parents('.feature-container').find('.feature-choice-body').removeClass('mandatory');
          $(this).parents('.feature-parameter').find('.feature-choice-body').addClass('mandatory');
        });
      } else if (!isFeature) {
        nodeOutput.find('.feature-choice').empty();
      }
      nodeOutput
          .find('.feature-choice-body')
          .append(displayMacroParameterTreeNode(parametersMap, paramNode));

      if (isFeature) {
        nodeOutput.find('.feature-choice-body').addClass('with-choice');
      }
      return nodeOutput;
    }));
    return output;
  },

  // The given node can be a group or a parameter.
  fillNodeTab = function(parametersMap, node, tab, tabPanel) {
    var id = 'macroParameterTreeNode-' + node.id;
    tab.attr({
      'href': '#' + id,
      'aria-controls': id
    }).text(node.name);
    // If the given node is a parameter (no children) then we handle it as a group with a single parameter.
    let childNodes = node.children || [node.key];
    let tabOutput;
    if (node.featureOnly) {
      tabOutput = displayFeature(parametersMap, node, true);
    } else {
      tabOutput = childNodes.map(nodeKey =>
          displayMacroParameterTreeNode(parametersMap, parametersMap[nodeKey]));
    }

    tabPanel
        .attr('id', id)
        .append(tabOutput);
    // Hide the parameter name if there is only one child node and its name matches the name used on the tab.
    if (childNodes.length === 1 && childNodes[0] && parametersMap[childNodes[0]].name === node.name) {
      tabPanel.find('.macro-parameter-name').hide();
    }
  },

  // Make the macro parameter grouping (tabs) invisible if there is only one visible tab and it contains a single item.
  toggleMacroParameterGroupVisibility = function(group) {
    var visibleTabs = group.find('ul[role="tablist"]').first().children().not('.hidden');
    var firstVisibleTabPanel = group.find('.tab-content').first().children().not('.hidden').first();
    group.toggleClass('invisible', visibleTabs.length < 2 && firstVisibleTabPanel.children().not('.hidden').length < 2);
  },

  macroParameterTemplate =
    '<div class="macro-parameter">' +
      '<div class="macro-parameter-name-container">' +
        '<label class="macro-parameter-name" for=""></label>' +
        '<span class="mandatory"></span>' +
      '</div>' +
      '<div class="macro-parameter-description"></div>' +
    '</div>',

  displayMacroParameter = function(parameter) {
    let output = $(macroParameterTemplate);
    output.attr('data-id', parameter.id).attr('data-type', parameter.displayType);
    output.find('.macro-parameter-name').attr('for', 'parameter-' + parameter.id);
    output.find('.macro-parameter-name').text(parameter.name);
    if (parameter.mandatory) {
      output.find('.mandatory').text('(' + translations.get('required') + ')');
    }
    output.find('.macro-parameter-description').text(parameter.description);
    output.toggleClass('mandatory', !!parameter.mandatory);
    output.toggleClass('hidden', !!parameter.hidden);
    output.append(displayMacroParameterField(parameter));
    return output;
  },

  displayMacroParameterField = function(parameter) {
    let field = $('<div></div>').addClass('macro-parameter-field').html(parameter.editTemplate);
    // Look for input elements whose name matches the parameter id.
    let valueInputs = field.find(':input').filter(function() {
      return $(this).attr('name') === parameter.id;
    });
    // set the id of the input
    valueInputs.first().attr('id', 'parameter-' + parameter.id);
    let firstInputType = valueInputs.prop('type');
    let value = parameter.hasOwnProperty('value') ? parameter.value : parameter.defaultValue;
    let matchesParameterValue = function(value) {
      return function() {
        if (parameter.caseInsensitive) {
          return $(this).val().toUpperCase() === value.toUpperCase();
        } else {
          return $(this).val() === value;
        }
      };
    };
    if (firstInputType === 'checkbox' || firstInputType === 'radio') {
      // Keep only the input elements with the same type as the first one.
      valueInputs = valueInputs.filter(function() {
        return $(this).prop('type') === firstInputType;
      });
      if (parameter.caseInsensitive) {
        // Use the canonical value.
        value = valueInputs.filter(matchesParameterValue(value)).val() || value;
      }
    } else {
      // Keep only the first input element.
      valueInputs = valueInputs.first();
      // For select inputs we should add the value to the list of options if it's missing.
      if (value && valueInputs.is('select')) {
        value = valueInputs.prop('type') === 'select-multiple' ? value.split(',') : [value];
        value.forEach(function (val, index) {
          let matchedOption = valueInputs.find('option').filter(matchesParameterValue(val));
          if (matchedOption.length > 0) {
            // Use the canonical value.
            value[index] = matchedOption.val();
          } else {
            // Add the missing option.
            $('<option></option>').val(val).text(val).appendTo(valueInputs);
          }
        });
      }
    }
    // FIXME: we need to find a way to select automatically the parent radio button in case of feature
    // We pass the value as an array in order to properly handle radio inputs and checkboxes.
    valueInputs.val(Array.isArray(value) ? value : [value]);
    return field;
  };

  return {
    display: displayMacroParameterTree
  };
});

/**
 * Macro Editor
 */
define(
  'macroEditor',
  ['jquery', 'modal', 'l10n!macroEditor', 'macroService', 'macroParameterEnhancer',
    'macroParameterTreeDisplayer', 'bootstrap'],
  // jshint maxparams:false
  function($, $modal, translations, macroService, macroParameterEnhancer,
    macroParameterTreeDisplayer)
{
  'use strict';

  const macroEditorTemplate =
    '<div>' +
      '<div class="macro-title">' +
        '<div class="macro-name"></div>' +
        '<div class="macro-description"></div>' +
      '</div>' +
      '<div class="macro-parameters"></div>' +
    '</div>',

  createMacroEditor = function(macroCall, macroDescriptorData, macroParameters) {
    // We'll perform changes of value in the descriptor, so we want to use a clone not the original instance.
    let macroDescriptor = structuredClone(macroDescriptorData.descriptor);
    let macroEditor = $(macroEditorTemplate);
    macroEditor.find('.macro-name').text(macroDescriptor.name);
    macroEditor.find('.macro-description').text(macroDescriptor.description);
    macroParameterEnhancer.enhance(macroDescriptor, macroCall, macroParameters);
    macroEditor.find('.macro-parameters').append(macroParameterTreeDisplayer
        .display(macroDescriptor, macroDescriptorData.requiredSkinExtensions));
    this.removeClass('loading').data('macroDescriptor', macroDescriptor).append(macroEditor.children());
    $(document).trigger('xwiki:dom:updated', {'elements': this.toArray()});
  },

  maybeCreateMacroEditor = function(requestNumber, macroCall, macroDescriptorData, macroParameters) {
    // Check if the macro descriptor corresponds to the last request.
    if (this.prop('requestNumber') === requestNumber) {
      createMacroEditor.call(this, macroCall, macroDescriptorData, macroParameters);
      this.trigger('ready');
    }
  },

  maybeShowError = function(requestNumber, messageKey) {
    // Check if the error corresponds to the last request.
    if (this.prop('requestNumber') === requestNumber) {
      this.removeClass('loading').append(
        '<div class="box errormessage">' +
          translations.get(messageKey, '<strong></strong>') +
        '</div>'
      ).find('strong').text(this.attr('data-macroId'));
    }
  },

  extractFormData = function(container) {
    let data = {};
    container.find(':input').serializeArray().forEach(function(parameter) {
      let value = data[parameter.name];
      if (value === undefined) {
        data[parameter.name] = parameter.value;
      } else if (parameter.value !== '') {
        if (Array.isArray(value)) {
          value.push(parameter.value);
        } else {
          data[parameter.name] = [value, parameter.value];
        }
      }
    });
    return data;
  },

  toMacroCall = function(formData, macroDescriptor) {
    if (!macroDescriptor) {
      return null;
    }
    let macroCall = {
      name: macroDescriptor.id,
      content: undefined,
      parameters: {}
    };
    // Note that we include the empty content in the macro call (instead of leaving it undefined) because we want to
    // generate {{macro}}{{/macro}} instead of {{macro/}} when the macro supports content.
    if (typeof formData.$content === 'string' && macroDescriptor.parametersMap["PARAMETER:$content"]) {
      macroCall.content = formData.$content;
    }

    for (let key in macroDescriptor.parametersMap) {
      handleDescriptorKeysToMacroCall(key, macroCall, formData, macroDescriptor);
    }
    return macroCall;
  },

  handleDescriptorKeysToMacroCall = function (key, macroCall, formData, macroDescriptor) {
    // we handle content separately.
    if (key === '$content') {
      return;
    }
    let descriptor = macroDescriptor.parametersMap[key];
    if (key.startsWith('PARAMETER:')) {
      addParameterValueToMacroCall(macroCall, formData, descriptor);
    } else if (key.startsWith('FEATURE:')) {
      let featureValueKey = formData['feature-radio-' + descriptor.id];
      if (featureValueKey) {
        handleDescriptorKeysToMacroCall(featureValueKey, macroCall, formData, macroDescriptor);
      }
    } else if (key.startsWith('GROUP:')) {
      descriptor.children.forEach(childKey =>
          handleDescriptorKeysToMacroCall(childKey, macroCall, formData, macroDescriptor));
    }
  },

  addParameterValueToMacroCall = function(macroCall, formData, parameterDescriptor) {
    let value = formData[parameterDescriptor.id];
    if (Array.isArray(value)) {
      value = isBooleanType(parameterDescriptor.displayType) ? value[0] : value.join();
    }
    let defaultValue = parameterDescriptor.defaultValue;
    if (value !== '' && (defaultValue === undefined || defaultValue === null || (defaultValue + '') !== value)) {
      macroCall.parameters[parameterDescriptor.id] = value;
    }
  },

  isBooleanType = function(type) {
    return type === 'boolean' || type === 'java.lang.Boolean';
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
        let macroCall = this.getMacroCall();

        let emptyMandatoryParams = [];
        // Include the mandatory features for which no option is checked.
        macroEditor.find('.feature-container.mandatory').filter(function () {
          return $(this).find('.feature-radio').length > 0 &&
              $(this).find('.feature-radio:checked').length === 0;
        }).map((index, elt) => emptyMandatoryParams.push(elt));
        // Exclude the hidden mandatory parameters
        macroEditor.find('.macro-parameter.mandatory:not(.hidden)').filter(function() {
          let id = $(this).attr('data-id');
          let value = id === '$content' ? macroCall.content : macroCall.parameters[id];
          return value === undefined || value === '';
        }).map((index, elt) => emptyMandatoryParams.push(elt));
        $(emptyMandatoryParams).first()
            .addClass('has-error')
            .find(':input')
            .not(':hidden')
            .first()
            .trigger('focus');
        setTimeout(function() {
          $(emptyMandatoryParams).first().removeClass('has-error');
        }, 1000);
        return emptyMandatoryParams.length === 0;
      },
      update: function(macroCall, syntaxId, sourceDocumentReference, widgetHtml) {
        var macroId = macroCall.name;
        if (syntaxId) {
          macroId += '/' + syntaxId;
        }
        var requestNumber = (macroEditor.prop('requestNumber') || 0) + 1;
        macroEditor.empty().addClass('loading')
          .attr('data-macroId', macroId)
          .prop('requestNumber', requestNumber);

        // Load the macro descriptor
        // FIXME: only perform this call if needed
        macroService.getMacroParametersFromHTML(macroId, widgetHtml)
            // TODO: this should be probably improved with better $.then construction.
          .done(function (parameters) {
                macroService.getMacroDescriptor(macroId, sourceDocumentReference)
                    .done(function (descriptor) {
                      maybeCreateMacroEditor.bind(macroEditor, requestNumber, macroCall, descriptor, parameters)();
                    })
                    .fail(maybeShowError.bind(macroEditor, requestNumber, 'descriptorRequestFailed'));
          }).fail(maybeShowError.bind(macroEditor, requestNumber, 'parametersRequestFailed'));
      }
    };
  },

  load = function(input, macroEditor) {
    var macroEditorAPI = macroEditor.data('macroEditorAPI');
    var macroCall = input.macroCall || {
      name: input.macroId,
      parameters: {}
    };
    // We need to obey the specified macro identifier in case the user has just changed the macro.
    macroCall.name = input.macroId || macroCall.name;
    if (!macroEditorAPI) {
      // Initialize the macro editor.
      var submitButton = $(this).find('.modal-footer .btn-primary');
      macroEditor.on('ready', function(event) {
        macroEditorAPI.focus();
        submitButton.prop('disabled', false);
      });
      macroEditorAPI = macroEditor.xwikiMacroEditor(macroCall, input.syntaxId, input.sourceDocumentReference,
          input.widgetHtml);
    } else {
      macroEditorAPI.update(macroCall, input.syntaxId, input.sourceDocumentReference, input.widgetHtml);
    }
  },

  install = function(input, macroEditor) {
    var macroId = input.macroId + '' + input.syntaxId;
    var requestNumber = (macroEditor.prop('requestNumber') || 0) + 1;

    macroEditor.empty().addClass('loading')
      .attr('data-macroId', macroId)
      .prop('requestNumber', requestNumber);

    macroService.installMacro(input.extensionId, input.extensionVersion)
      .done(load.bind(this, input, macroEditor))
      .fail(maybeShowError.bind(macroEditor, requestNumber, 'installRequestFailed'));
  },

  editMacro = $modal.createModalStep({
    'class': 'macro-editor-modal',
    title: translations.get('title'),
    content: '<div class="macro-editor xform"></div>',
    acceptLabel: translations.get('submit'),
    form: true,
    onLoad: function() {
      var modal = this;
      var submitButton = modal.find('.modal-footer .btn-primary');
      modal.on('show.bs.modal', function(event) {
        submitButton.prop('disabled', true);
      }).on('shown.bs.modal', function(event) {
        var macroEditor = modal.find('.macro-editor');
        var input = modal.data('input');

        // Install the macro extension (if not already installed)
        if (input.macroCategories && input.macroCategories.includes('_notinstalled')) {
          install.call(this, input, macroEditor);
        } else {
          load.call(this, input, macroEditor);
        }
      });
      modal.find('.modal-content').on('submit', function(event) {
        // Ideally we would handle this propagation by making the modal a dialog and using method="dialog" on the form
        event.preventDefault();
        event.stopPropagation();
        var macroEditor = modal.find('.macro-editor');
        var macroEditorAPI = macroEditor.xwikiMacroEditor();
        if (macroEditorAPI.validate()) {
          var output = modal.data('input');
          delete output.action;
          var macroDescriptor = macroEditor.data('macroDescriptor');
          // Preserve the in-line/block mode if possible. Note that we consider the macro in-line if no value is
          // specified, because the caret is placed in an in-line context most of the time (e.g. inside a paragraph) in
          // order to allow the user to type text).
          var inline = (!output.macroCall || output.macroCall.inline !== false) && macroDescriptor.supportsInlineMode;
          output.macroCall = macroEditorAPI.getMacroCall();
          output.macroCall.descriptor = macroDescriptor;
          output.macroCall.inline = inline;
          modal.data('output', output).modal('hide');
        }
      });
      var changeMacroButton = $('<button type="button" class="btn btn-default pull-left"></button>')
        .text(translations.get('changeMacro'))
        .prependTo(submitButton.parent());
      changeMacroButton.on('click', function(event) {
        var macroEditor = modal.find('.macro-editor');
        var macroEditorAPI = macroEditor.xwikiMacroEditor();
        var output = modal.data('input');
        output.action = 'changeMacro';
        // Preserve the in-line/block mode if possible.
        var inline = output.macroCall ? output.macroCall.inline : undefined;
        output.macroCall = macroEditorAPI.getMacroCall();
        if (output.macroCall) {
          output.macroCall.inline = inline;
        }
        modal.data('output', output).modal('hide');
      });
    }
  });

  $.fn.xwikiMacroEditor = function(macroCall, syntaxId, sourceDocumentReference, widgetHtml) {
    this.each(function() {
      var macroEditor = $(this);
      if (!macroEditor.data('macroEditorAPI')) {
        var macroEditorAPI = createMacroEditorAPI(macroEditor);
        macroEditor.data('macroEditorAPI', macroEditorAPI);
        macroEditorAPI.update(macroCall, syntaxId, sourceDocumentReference, widgetHtml);
      }
    });
    return this.data('macroEditorAPI');
  };

  return editMacro;
});
