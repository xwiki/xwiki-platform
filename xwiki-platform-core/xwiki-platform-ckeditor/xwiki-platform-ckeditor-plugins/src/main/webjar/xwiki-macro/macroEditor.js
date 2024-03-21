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
 * Macro Parameter Tree Builder
 *
 * The result is a JavaScript plain object with a structure similar to this:
 *
 *   [
 *     {
 *       type: 'parameter',
 *       data: {...}
 *     },
 *     {
 *       // The children of this node provide the same feature so only one of them needs to be set.
 *       type: 'group-single',
 *       data: {
 *         feature: '...'
 *       },
 *       children: [
 *         {
 *           type: 'parameter',
 *           data: {...}
 *         },
 *         {
 *           // This node groups related parameters, any can be set.
 *           type: 'group-multiple',
 *           data: {...},
 *           children: [
 *             {
 *               type: 'parameter',
 *               data: {...}
 *             },
 *             ...
 *           ]
 *         },
 *         {
 *           type: 'parameter',
 *           data: {...}
 *         }
 *       ]
 *     },
 *     {
 *       type: 'parameter',
 *       data: {...}
 *     }
 *   ];
 */
define('macroParameterTreeBuilder', ['jquery', 'l10n!macroEditor'], function($, translations) {
  'use strict';

  var buildMacroParameterTree = function(macroDescriptor) {
    var parameter, macroParameterTree = {children: []};
    // Add the macro parameter groups specified in the macro descriptor.
    $.each(macroDescriptor.groupDescriptorTree, function(groupId, groupDescriptor) {
      addMacroParameterGroupTreeNode(macroParameterTree, groupDescriptor);
    });
    // Add the macro parameters specified in the macro descriptor.
    if (macroDescriptor.parameterDescriptorMap) {
      for (var parameterId in macroDescriptor.parameterDescriptorMap) {
        if (macroDescriptor.parameterDescriptorMap.hasOwnProperty(parameterId)) {
          parameter = $.extend({}, macroDescriptor.parameterDescriptorMap[parameterId]);
          parameter.name = parameter.name || parameter.id;
          addMacroParameterTreeNode(parameter, macroParameterTree, macroDescriptor);
        }
      }
    }
    // Handle the macro content as a special macro parameter.
    if (macroDescriptor.contentDescriptor) {
      parameter = $.extend({
        id: '$content',
        name: translations.get('content')
      }, macroDescriptor.contentDescriptor);
      addMacroParameterTreeNode(parameter, macroParameterTree, macroDescriptor);
    }
    // Group parameters by feature.
    groupMacroParametersByFeature(macroParameterTree);
    return macroParameterTree;
  },

  addMacroParameterGroupTreeNode = function(parentNode, groupDescriptor) {
    var groupNode = {
      type: 'group-multiple',
      data: $.extend({}, groupDescriptor),
      children: []
    };
    delete groupNode.data.groups;
    $.each(groupDescriptor.groups, function(childGroupId, childGroupDescriptor) {
      addMacroParameterGroupTreeNode(groupNode, childGroupDescriptor);
    });
    parentNode.children.push(groupNode);
  },

  addMacroParameterTreeNode = function(parameter, macroParameterTree, macroDescriptor) {
    var parentNode = macroParameterTree;
    if (Array.isArray(parameter.group)) {
      parameter.group.forEach(function(groupId) {
        var childNode = getChildNode(parentNode, groupId);
        if (!childNode) {
          parentNode.children.push({
            type: 'group-multiple',
            data: {
              id: groupId,
              name: groupId,
              children: []
            }
          });
        }
        parentNode = childNode;
      });
    }
    parentNode.children.push({
      type: 'parameter',
      data: parameter
    });
  },

  getChildNode = function(parent, id) {
    for (var i = 0; parent.children && i < parent.children.length; i++) {
      var child = parent.children[i];
      if (child.data && child.data.id === id) {
        return child;
      }
    }
  },

  groupMacroParametersByFeature = function(parentNode) {
    var featureGroupNodes = {};
    var children = [];
    parentNode.children.forEach(function(childNode) {
      if (childNode.children) {
        groupMacroParametersByFeature(childNode);
      }
      if (childNode.data && childNode.data.feature) {
        var featureGroupNode = featureGroupNodes[childNode.data.feature];
        if (!featureGroupNode) {
          featureGroupNodes[childNode.data.feature] = featureGroupNode = {
            type: 'group-single',
            data: {feature: childNode.data.feature},
            children: []
          };
          children.push(featureGroupNode);
        }
        featureGroupNode.children.push(childNode);
      } else {
        children.push(childNode);
      }
    });
    parentNode.children = children;
  };

  return {
    build:  buildMacroParameterTree
  };
});

/**
 * Macro Parameter Tree Sorter
 */
define('macroParameterTreeSorter', ['jquery'], function($) {
  'use strict';

  var sortMacroParameterTree = function(parentNode, macroCall, hiddenMacroParameters) {
    if (parentNode.children) {
      // We need to be able to compare parameters and groups.
      parentNode.data = $.extend({
        hidden: true,
        mandatory: false,
        value: false,
        advanced: true,
        index: Infinity
      }, parentNode.data);
      parentNode.children.forEach(function(childNode) {
        sortMacroParameterTree(childNode, macroCall, hiddenMacroParameters);
        // The parent is hidden if all the children are hidden.
        parentNode.data.hidden = parentNode.data.hidden && childNode.data.hidden === true;
        // The parent is mandatory if at least one child is mandatory.
        parentNode.data.mandatory = parentNode.data.mandatory || childNode.data.mandatory;
        // The parent has value if at least one child has value.
        parentNode.data.value = parentNode.data.value ||
          (childNode.data.hasOwnProperty('value') && childNode.data.value !== false);
        // The parent is advanced if all the children are advanced.
        parentNode.data.advanced = parentNode.data.advanced && childNode.data.advanced === true;
        // The parent index is the minimum index from its children.
        parentNode.data.index = Math.min(parentNode.data.index, childNode.data.index);
      });
      // Sort the child nodes.
      parentNode.children.sort(compareMacroParameterTreeNodes);
      // Add parameter separator (More).
      addParameterSeparator(parentNode.children);
    } else if (parentNode.type === 'parameter') {
      maybeSetParameterValue(parentNode.data, macroCall);
      maybeHideParameter(parentNode.data, macroCall, hiddenMacroParameters);
    }
  },

  maybeSetParameterValue = function(parameter, macroCall) {
    if (parameter.id === '$content') {
      // We set the parameter value even if it's empty because we want the macro content to have the priority of a
      // macro parameter that is explicitly set. This means that the macro content text area will be displayed on top
      // of the macro parameters that don't have a value set.
      if (typeof macroCall.content === 'string') {
        parameter.value = macroCall.content;
      }
    } else {
      var parameterCall = macroCall.parameters[parameter.id.toLowerCase()];
      // Check if the macro parameter is set.
      if (parameterCall !== null && parameterCall !== undefined) {
        parameter.value = (typeof parameterCall === 'object' && typeof parameterCall.name === 'string') ?
          parameterCall.value : parameterCall;
      }
    }
  },

  /**
   * The following macro parameters should be hidden:
   * - parameters marked as "display hidden" in the descriptor and not having any value set
   * - deprecated parameters that don't have a value set
   * - parameters that can be edited in-place (as long as they are not mandatory and without a value set)
   */
  maybeHideParameter = function(parameter, macroCall, hiddenMacroParameters) {
    // We can't hide mandatory parameters that don't have a value set.
    parameter.hidden = isHiddenParameterAndNoValue(parameter) || isDeprecatedParameterUnset(parameter) ||
        (hiddenMacroParameters.indexOf(parameter.id) >= 0 && !isMandatoryParameterEmpty(parameter));
  },

  isHiddenParameterAndNoValue = function(macroParameter) {
    // Don't display parameters that are marked hidden in the macro descriptor, unless they have a value set in which
    // case we need to display it so that the user can see it/modify it.
    return macroParameter.hidden && macroParameter.value === undefined;
  },

  isDeprecatedParameterUnset = function(macroParameter) {
    return macroParameter.deprecated && macroParameter.value === undefined;
  },

  isMandatoryParameterEmpty = function(macroParameter) {
    return macroParameter.mandatory && (typeof macroParameter.value !== 'string' || macroParameter.value.length === 0);
  },

  compareMacroParameterTreeNodes = function(alice, bob) {
    return compareMacroParameters(alice.data, bob.data);
  },

  macroParameterComparators = [
    // Put the hidden macro parameters first to prevent the macro parameter separator (More) from showing them.
    function (alice, bob) {
      return bob.hidden - alice.hidden;
    },
    // Then mandatory parameters..
    function (alice, bob) {
      return bob.mandatory - alice.mandatory;
    },
    // ..and parameters with value.
    function (alice, bob) {
      var aliceHasValue = alice.hasOwnProperty('value') && alice.value !== false;
      var bobHasValue = bob.hasOwnProperty('value') && bob.value !== false;
      return bobHasValue - aliceHasValue;
    },
    // Put advanced parameters last.
    function (alice, bob) {
      return !!alice.advanced - !!bob.advanced;
    },
    // Finally, use the macro descriptor order (lower index first).
    function (alice, bob) {
      return alice.index - bob.index;
    }
  ],

  compareMacroParameters = function(alice, bob) {
    for (var i = 0; i < macroParameterComparators.length; i++) {
      var result = macroParameterComparators[i](alice, bob);
      if (result !== 0) {
        return result;
      }
    }
    return 0;
  },

  addParameterSeparator = function(childNodes) {
    // TODO: The the complexity can onlu be lowered. Once below the default maxcomplexity (10 at the time of writing), 
    //  the jshint annotation can be removed.
    /*jshint maxcomplexity:14 */
    // Skip hidden nodes (they should be at the start).
    var offset = 0;
    while (offset < childNodes.length && childNodes[offset].data.hidden) {
      offset++;
    }
    var i = offset;
    // Show all the mandatory parameters and those that have been set.
    while (i < childNodes.length && (childNodes[i].data.mandatory ||
        (childNodes[i].data.hasOwnProperty('value') && childNodes[i].data.value !== false))) {
      i++;
    }
    var limit = 3;
    // Show simple parameters until we reach the limit.
    while (i < childNodes.length && i < (offset + limit) && !childNodes[i].data.advanced) {
      i++;
    }
    // If there are no mandatory parameters and no parameter is set and all parameters are advanced then we're forced
    // to show some advanced parameters.
    if (i === offset) {
      i = Math.min(childNodes.length, offset + limit);
    }
    // Show a 'more' link to toggle the remaining parameters.
    if (i > offset && i < childNodes.length) {
      childNodes.splice(i, 0, {type: 'more'});
    // Show a message for the empty list of parameters.
    } else if (childNodes.length === 0) {
      childNodes.push({type: 'empty'});
    }
  };

  return {
    sort:  sortMacroParameterTree
  };
});

/**
 * Macro Parameter Tree Displayer
 */
define('macroParameterTreeDisplayer', ['jquery', 'l10n!macroEditor'], function($, translations) {
  'use strict';

  var displayMacroParameterTree = function(macroParameterTree, requiredSkinExtensions) {
    var output = $('<div></div>');
    output.append(macroParameterTree.children.map(displayMacroParameterTreeNode));
    output.find('.more').on('click', toggleMacroParameters).click();
    output.find('a[role="tab"]').on('click', function(event) {
      event.preventDefault();
      $(this).tab('show');
    });
    // Load the pickers.
    $(document).loadRequiredSkinExtensions(requiredSkinExtensions);
    return output.children();
  },

  displayMacroParameterTreeNode = function(node) {
    switch (node.type) {
      case 'group-multiple': return displayMultipleChoiceGroup(node);
      case 'group-single': return displaySingleChoiceGroup(node);
      case 'parameter': return displayMacroParameter(node.data);
      case 'more': return $('<li class="more"><span class="arrow arrow-down"></span> <a href="#more"></a></li>')
        .find('a').text(translations.get('more')).end();
      case 'empty': return $('<li class="empty"></li>').text(translations.get('noParameters'));
    }
  },

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

  displayMultipleChoiceGroup = function(groupNode) {
    var output = $(macroParameterGroupTemplate).addClass('multiple-choice');
    fillNodeTab(groupNode, output.find('.macro-parameter-group-name'), output.find('.macro-parameter-group-members'));
    toggleMacroParameterGroupVisibility(output);
    return output;
  },

  // The given node can be a group or a parameter.
  fillNodeTab = function(node, tab, tabPanel) {
    var id = 'macroParameterTreeNode-' + node.data.id;
    tab.attr({
      'href': '#' + id,
      'aria-controls': id
    }).text(node.data.name);
    // If the given node is a parameter (no children) then we handle it as a group with a single parameter.
    var childNodes = node.children || [node];
    tabPanel.attr('id', id).append(childNodes.map(displayMacroParameterTreeNode));
    // Hide the parameter name if there is only one child node and its name matches the name used on the tab.
    if (childNodes.length === 1 && childNodes[0].data && childNodes[0].data.name === node.data.name) {
      tabPanel.find('.macro-parameter-name').hide();
    }
  },

  displaySingleChoiceGroup = function(groupNode) {
    var output = $(macroParameterGroupTemplate).addClass('single-choice').attr('data-feature', groupNode.data.feature);
    var tabs = output.find('ul[role="tablist"]');
    var tabTemplate = tabs.children().remove();
    var tabPanels = output.find('.tab-content');
    var tabPanelTemplate = tabPanels.children().remove();
    groupNode.children.forEach(function(childNode, index) {
      var tab = tabTemplate.clone().appendTo(tabs);
      var tabPanel = tabPanelTemplate.clone().appendTo(tabPanels);
      // Some of the child nodes might be hidden so we will activate the first visible tab at the end.
      tab.add(tabPanel).removeClass('active').toggleClass('hidden', !!childNode.data.hidden);
      fillNodeTab(childNode, tab.children().first(), tabPanel);
    });
    // Activate the first visible tab.
    var activeTab = tabs.children().not('.hidden').first();
    var activeTabPanel = tabPanels.children().not('.hidden').first();
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

  displayMacroParameter = function(parameter) {
    var output = $(macroParameterTemplate);
    output.attr('data-id', parameter.id).attr('data-type', parameter.type);
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
  ['jquery', 'modal', 'l10n!macroEditor', 'macroService', 'macroParameterTreeBuilder', 'macroParameterTreeSorter',
    'macroParameterTreeDisplayer', 'bootstrap'],
  // jshint maxparams:false
  function($, $modal, translations, macroService, macroParameterTreeBuilder, macroParameterTreeSorter,
    macroParameterTreeDisplayer)
{
  'use strict';

  var macroEditorTemplate =
    '<div>' +
      '<div class="macro-name"></div>' +
      '<div class="macro-description"></div>' +
      '<ul class="macro-parameters"></ul>' +
    '</div>',

  createMacroEditor = function(macroCall, macroDescriptor) {
    var macroEditor = $(macroEditorTemplate);
    macroEditor.find('.macro-name').text(macroDescriptor.name);
    macroEditor.find('.macro-description').text(macroDescriptor.description);
    var macroParameterTree = macroParameterTreeBuilder.build(macroDescriptor);
    macroParameterTreeSorter.sort(macroParameterTree, macroCall, this.data('hiddenMacroParameters'));
    macroEditor.find('.macro-parameters').append(macroParameterTreeDisplayer.display(macroParameterTree,
      macroDescriptor.requiredSkinExtensions));
    this.removeClass('loading').data('macroDescriptor', macroDescriptor).append(macroEditor.children());
    $(document).trigger('xwiki:dom:updated', {'elements': this.toArray()});
  },

  maybeCreateMacroEditor = function(requestNumber, macroCall, macroDescriptor) {
    // Check if the macro descriptor corresponds to the last request.
    if (this.prop('requestNumber') === requestNumber) {
      createMacroEditor.call(this, macroCall, macroDescriptor);
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
      name: macroDescriptor.id.id,
      content: undefined,
      parameters: {}
    };
    // Note that we include the empty content in the macro call (instead of leaving it undefined) because we want to
    // generate {{macro}}{{/macro}} instead of {{macro/}} when the macro supports content.
    if (typeof formData.$content === 'string' && macroDescriptor.contentDescriptor) {
      macroCall.content = formData.$content;
    }
    for (var parameterId in formData) {
      // The parameter descriptor map keys are lower case for easy lookup (macro parameter names are case insensitive).
      var parameterDescriptor = macroDescriptor.parameterDescriptorMap[parameterId.toLowerCase()];
      if (parameterDescriptor) {
        var value = formData[parameterId];
        if (Array.isArray(value)) {
          value = isBooleanType(parameterDescriptor.type) ? value[0] : value.join();
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
        var emptyMandatoryParams = macroEditor.find('.macro-parameter.mandatory ').filter(function() {
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
    macroEditor.data('hiddenMacroParameters', input.hiddenMacroParameters || []);
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
      modal.find('.modal-content')[0].addEventListener('submit', function(event) {
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
