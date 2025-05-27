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
  'installRequestFailed',
  'noParameters',
  'content',
  'more'
]);

/**
 * Macro Parameter Tree Sorter
 */
define('macroParameterEnhancer', ['jquery'], function($) {
  'use strict';

  var enhanceMacroParameters = function(macroDescriptor, macroCall) {
    for (let parameterId in macroDescriptor.parametersMap) {
      let parameter = macroDescriptor.parametersMap[parameterId];
      maybeSetParameterValue(parameter, macroCall);
      maybeHideParameter(parameter);
    }
  },

  maybeSetParameterValue = function(parameter, macroCall) {
    var parameterCall = macroCall.parameters[parameter.id.toLowerCase()];
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

  var displayMacroParameterTree = function(macroParameterTree, requiredSkinExtensions) {
    var output = $('<div></div>');
    if (macroParameterTree.mandatoryNodes.length < 1 && macroParameterTree.optionalNodes.length < 1) {
      output.append($('<li class="empty"></li>').text(translations.get('noParameters')));
    } else {
      let parametersMap = macroParameterTree.parametersMap;
      output.append(macroParameterTree.mandatoryNodes.map(key => {
        let node = parametersMap[key];
        return displayMacroParameterTreeNode(parametersMap, node, true);
      }));
      output.append(macroParameterTree.optionalNodes.map(key => {
        let node = parametersMap[key];
        return displayMacroParameterTreeNode(parametersMap, node, false);
      }));
    }

    output.find('a[role="tab"]').on('click', function(event) {
      event.preventDefault();
      $(this).tab('show');
    });
    // Load the pickers.
    $(document).loadRequiredSkinExtensions(requiredSkinExtensions);
    return output.children();
  },

  displayMacroParameterTreeNode = function(parametersMap, node, isMandatory) {
    switch (node.type) {
      case 'GROUP': return displayGroup(parametersMap, node, isMandatory);
      case 'PARAMETER': return displayMacroParameter(node, isMandatory);
    }
  },

  macroParameterGroupOptionalsTemplate =
      '<li class="macro-parameter-group-optionals single-choice">' +
        '<ul class="nav nav-tabs" role="tablist">' +
          '<li class="active" role="presentation">' +
            '<a class="macro-parameter-group-name" href="#groupId" aria-controls="groupId" role="tab"></a>' +
          '</li>' +
        '</ul>' +
        '<div class="tab-content">' +
          '<ul id="groupId" class="macro-parameter-group-members tab-pane active" role="tabpanel"></ul>' +
        '</div>' +
      '</li>',

  macroParameterGroupTemplate = 
    '<li class="macro-parameter-group">' +
      '<ul class="nav nav-tabs" role="tablist">' +
        '<li class="active" role="presentation">' +
          '<a class="macro-parameter-group-name" href="#groupId" aria-controls="groupId" role="tab"></a>' +
        '</li>' +
      '</ul>' +
      '<div class="tab-content">' +
        '<ul id="groupId" class="macro-parameter-group-members tab-pane active" role="tabpanel"></ul>' +
      '</div>' +
    '</li>',

  displayGroup = function(parametersMap, groupNode, isMandatory) {
    if (!isMandatory) {
      return displaySingleChoiceGroup(parametersMap, groupNode, isMandatory);
    } else if (groupNode.feature) {
      return displayFeature(parametersMap, groupNode, isMandatory);
    } else {
      var output = $(macroParameterGroupTemplate).addClass('multiple-choice');
      fillNodeTab(parametersMap, groupNode,
          output.find('.macro-parameter-group-name'),
          output.find('.macro-parameter-group-members'),
          isMandatory);
      toggleMacroParameterGroupVisibility(output);
      return output;
    }
  },

  macroFeatureContainerTemplate =
    '<div class="panel-group" id="accordion-feature-{{featureName}}" role="tablist" aria-multiselectable="true"></div>',

  macroFeatureContentTemplate =
    '<div class="panel panel-default">' +
      '<div class="panel-heading" role="tab" id="feature-heading-{{parameterId}}">' +
        '<h4 class="panel-title">' +
          '<a role="button" data-toggle="collapse" data-parent="#accordion-feature-{{featureName}}" ' +
      'href="#feature-collapse-{{parameterId}}" ' +
          'aria-expanded="true" aria-controls="feature-collapse-{{parameterId}}"></a>' +
        '</h4>' +
      '</div>' +
      '<div id="feature-collapse-{{parameterId}}" class="panel-collapse collapse" role="tabpanel"' +
      ' aria-labelledby="feature-heading-{{parameterId}}">' +
        '<div class="panel-body"></div>' +
      '</div>' +
    '</div>',

  displayFeature = function (parametersMap, featureNode, isMandatory) {
    let featureName = featureNode.featureName;
    let output = $(macroFeatureContainerTemplate.replaceAll("{{featureName}}", featureName));
    output.append(featureNode.children.map(nodeKey => {
      let paramNode = parametersMap[nodeKey];
      let parameterId = paramNode.id;
      let nodeOutput = $(macroFeatureContentTemplate
          .replaceAll("{{featureName}}", featureName)
          .replaceAll("{{parameterId}}", parameterId));
      return nodeOutput
          .find('.panel-body')
          .append(displayMacroParameterTreeNode(parametersMap, paramNode, isMandatory));
    }));
  },

  // The given node can be a group or a parameter.
  fillNodeTab = function(parametersMap, node, tab, tabPanel, isMandatory) {
    var id = 'macroParameterTreeNode-' + node.id;
    tab.attr({
      'href': '#' + id,
      'aria-controls': id
    }).text(node.name);
    // If the given node is a parameter (no children) then we handle it as a group with a single parameter.
    var childNodes = node.children || [node.key];
    tabPanel
        .attr('id', id)
        .append(childNodes.map(nodeKey =>
            displayMacroParameterTreeNode(parametersMap, parametersMap[nodeKey], isMandatory)));
    // Hide the parameter name if there is only one child node and its name matches the name used on the tab.
    if (childNodes.length === 1 && childNodes[0] && parametersMap[childNodes[0]].name === node.name) {
      tabPanel.find('.macro-parameter-name').hide();
    }
  },

  // FIXME
  displaySingleChoiceGroup = function(parametersMap, groupNode, isMandatory) {
    let output = $(macroParameterGroupTemplate).addClass('single-choice').attr('data-feature', groupNode.id);
    let tabs = output.find('ul[role="tablist"]');
    let tabTemplate = tabs.children().remove();
    let tabPanels = output.find('.tab-content');
    let tabPanelTemplate = tabPanels.children().remove();
    groupNode.children.forEach(function(childNodeKey, index) {
      let childNode = parametersMap[childNodeKey];
      let tab = tabTemplate.clone().appendTo(tabs);
      let tabPanel = tabPanelTemplate.clone().appendTo(tabPanels);
      // Some of the child nodes might be hidden so we will activate the first visible tab at the end.
      tab.add(tabPanel).removeClass('active').toggleClass('hidden', !!childNode.hidden);
      fillNodeTab(parametersMap, childNode, tab.children().first(), tabPanel, isMandatory);
    });
    // Activate the first visible tab.
    let activeTab = tabs.children().not('.hidden').first();
    let activeTabPanel = tabPanels.children().not('.hidden').first();
    activeTab.add(activeTabPanel).addClass('active');
    toggleMacroParameterGroupVisibility(output);
    return output;
  },

  // Make the macro parameter grouping (tabs) invisible if there is only one visible tab and it contains a single item.
  toggleMacroParameterGroupVisibility = function(group) {
    var visibleTabs = group.find('ul[role="tablist"]').first().children().not('.hidden');
    var firstVisibleTabPanel = group.find('.tab-content').first().children().not('.hidden').first();
    group.toggleClass('invisible', visibleTabs.length < 2 && firstVisibleTabPanel.children().not('.hidden').length < 2);
  },

  macroParameterTemplate =
    '<li class="macro-parameter">' +
      '<div class="macro-parameter-name"></div>' +
      '<div class="macro-parameter-description"></div>' +
    '</li>',

  displayMacroParameter = function(parameter, isMandatory) {
    var output = $(macroParameterTemplate);
    output.attr('data-id', parameter.id).attr('data-type', parameter.displayType);
    output.find('.macro-parameter-name').text(parameter.name);
    output.find('.macro-parameter-description').text(parameter.description);
    output.toggleClass('mandatory', !!parameter.mandatory);
    output.toggleClass('hidden', !!parameter.hidden);
    output.append(displayMacroParameterField(parameter));
    return output;
  },

  displayMacroParameterField = function(parameter) {
    var field = $('<div></div>').addClass('macro-parameter-field').html(parameter.editTemplate);
    // Look for input elements whose name matches the parameter id.
    var valueInputs = field.find(':input').filter(function() {
      return $(this).attr('name') === parameter.id;
    });
    var firstInputType = valueInputs.prop('type');
    var value = parameter.hasOwnProperty('value') ? parameter.value : parameter.defaultValue;
    var matchesParameterValue = function(value) {
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
          var matchedOption = valueInputs.find('option').filter(matchesParameterValue(val));
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

  var macroEditorTemplate =
    '<div>' +
      '<div class="macro-name"></div>' +
      '<div class="macro-description"></div>' +
      '<ul class="macro-parameters"></ul>' +
    '</div>',

  createMacroEditor = function(macroCall, macroDescriptorData) {
    let macroDescriptor = macroDescriptorData.descriptor;
    let macroEditor = $(macroEditorTemplate);
    macroEditor.find('.macro-name').text(macroDescriptor.name);
    macroEditor.find('.macro-description').text(macroDescriptor.description);
    macroParameterEnhancer.enhance(macroDescriptor, macroCall);
    macroEditor.find('.macro-parameters').append(macroParameterTreeDisplayer
        .display(macroDescriptor, macroDescriptorData.requiredSkinExtensions));
    this.removeClass('loading').data('macroDescriptor', macroDescriptor).append(macroEditor.children());
    $(document).trigger('xwiki:dom:updated', {'elements': this.toArray()});
  },

  maybeCreateMacroEditor = function(requestNumber, macroCall, macroDescriptorData) {
    // Check if the macro descriptor corresponds to the last request.
    if (this.prop('requestNumber') === requestNumber) {
      createMacroEditor.call(this, macroCall, macroDescriptorData);
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
    var data = {};
    container.find(':input').filter(function() {
      // Ignore the parameters from non-active groups.
      return $(this).parents('.macro-parameter-group-members').not('.active').length === 0;
    }).serializeArray().forEach(function(parameter) {
      var value = data[parameter.name];
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
    // TODO: The the complexity can onlu be lowered. Once below the default maxcomplexity (10 at the time of writing), 
    //  the jshint annotation can be removed.
    /*jshint maxcomplexity:12 */
    if (!macroDescriptor) {
      return null;
    }
    var macroCall = {
      name: macroDescriptor.id,
      content: undefined,
      parameters: {}
    };
    // Note that we include the empty content in the macro call (instead of leaving it undefined) because we want to
    // generate {{macro}}{{/macro}} instead of {{macro/}} when the macro supports content.
    if (typeof formData.$content === 'string' && macroDescriptor.parametersMap["PARAMETER:$content"]) {
      macroCall.content = formData.$content;
    }
    for (var parameterId in formData) {
      // The parameter descriptor map keys are lower case for easy lookup (macro parameter names are case insensitive).
      var parameterDescriptor = macroDescriptor.parametersMap[parameterId.toLowerCase()];
      if (parameterDescriptor) {
        var value = formData[parameterId];
        if (Array.isArray(value)) {
          value = isBooleanType(parameterDescriptor.displayType) ? value[0] : value.join();
        }
        var defaultValue = parameterDescriptor.defaultValue;
        if (value !== '' && (defaultValue === undefined || defaultValue === null || (defaultValue + '') !== value)) {
          macroCall.parameters[parameterId] = value;
        }
      }
    }
    return macroCall;
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
        var macroCall = this.getMacroCall();
        // Exclude the mandatory parameters that are editable in-place (they are hidden from the modal).
        var emptyMandatoryParams = macroEditor.find('.macro-parameter.mandatory:not(.hidden)').filter(function() {
          var id = $(this).attr('data-id');
          var value = id === '$content' ? macroCall.content : macroCall.parameters[id];
          return value === undefined || value === '';
        });
        emptyMandatoryParams.first().addClass('has-error').find(':input').not(':hidden').focus();
        setTimeout(function() {
          emptyMandatoryParams.first().removeClass('has-error');
        }, 1000);
        return emptyMandatoryParams.length === 0;
      },
      update: function(macroCall, syntaxId, sourceDocumentReference) {
        var macroId = macroCall.name;
        if (syntaxId) {
          macroId += '/' + syntaxId;
        }
        var requestNumber = (macroEditor.prop('requestNumber') || 0) + 1;
        macroEditor.empty().addClass('loading')
          .attr('data-macroId', macroId)
          .prop('requestNumber', requestNumber);

        // Load the macro descriptor
        macroService.getMacroDescriptor(macroId, sourceDocumentReference)
          .done(maybeCreateMacroEditor.bind(macroEditor, requestNumber, macroCall))
          .fail(maybeShowError.bind(macroEditor, requestNumber, 'descriptorRequestFailed'));
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
      macroEditorAPI = macroEditor.xwikiMacroEditor(macroCall, input.syntaxId);
    } else {
      macroEditorAPI.update(macroCall, input.syntaxId, input.sourceDocumentReference);
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

  $.fn.xwikiMacroEditor = function(macroCall, syntaxId, sourceDocumentReference) {
    this.each(function() {
      var macroEditor = $(this);
      if (!macroEditor.data('macroEditorAPI')) {
        var macroEditorAPI = createMacroEditorAPI(macroEditor);
        macroEditor.data('macroEditorAPI', macroEditorAPI);
        macroEditorAPI.update(macroCall, syntaxId, sourceDocumentReference);
      }
    });
    return this.data('macroEditorAPI');
  };

  return editMacro;
});
