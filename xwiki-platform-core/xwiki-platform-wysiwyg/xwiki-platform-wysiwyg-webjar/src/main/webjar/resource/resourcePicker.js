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
define('xwiki-wysiwyg-resource-picker-translation-keys', {
  prefix: 'resourcePicker.',
  keys: [
    'attach.hint',
    'doc.hint',
    'dropdown.toggle.title'
  ]
});

define('xwiki-wysiwyg-resource-picker', [
  'jquery',
  'xwiki-wysiwyg-resource',
  'xwiki-l10n!xwiki-wysiwyg-resource-picker-translation-keys',
  'xwiki-selectize'
], function($, $resource, translations) {
  'use strict';

  const resourcePickerTemplate = `
    <div class="resourcePicker">
      <div class="input-group">
        <input type="text" class="resourceReference" />
        <div class="input-group-btn">
          <button type="button" class="resourceType btn btn-default">
            <span class="icon">
          </button>
          <button type="button" class="btn btn-default dropdown-toggle" data-toggle="dropdown">
            <span class="sr-only"></span>
            <span class="caret"></span>
          </button>
          <ul class="resourceTypes dropdown-menu dropdown-menu-right"></ul>
        </div>
      </div>
    </div>`;

  function createResourcePicker(input, options = {}) {
    const resourcePicker = $(resourcePickerTemplate);
    resourcePicker.find('button.dropdown-toggle').attr('title', translations.get('dropdown.toggle.title'));
    resourcePicker.find('button.dropdown-toggle .sr-only').text(translations.get('dropdown.toggle.title'));
    resourcePicker.data("options", options);
    input.on('selectResource', onSelectResource).hide().after(resourcePicker);

    // Compute the list of supported resource types.
    let resourceTypes = $(input).attr('data-resourceTypes') || options.resourceTypes || [];
    if (typeof resourceTypes === 'string') {
      resourceTypes = resourceTypes.split(/\s*,\s*/);
    }

    // Initialize the layout.
    if (resourceTypes.length === 0) {
      resourcePicker.find('.input-group').attr('class', 'input-wrapper').children('.input-group-btn').hide();
    } else if (resourceTypes.length === 1) {
      const resourceTypeButton = resourcePicker.find('.resourceType');
      // Move the resource type button at the end in order to have round borders, and hide the resource type drop down.
      resourceTypeButton.appendTo(resourceTypeButton.parent()).prevAll().hide();
    }

    // Populate the Resource Type drop down.
    resourceTypes.forEach(resourceType => addResourceType(resourcePicker, resourceType));

    addResourcePickerBehaviour(input, resourcePicker);

    // Initialize the resource selection.
    if (input.val() || options.preselectCurrentResource) {
      input.trigger('selectResource', getResource(resourcePicker));
    } else {
      // Prepare the picker for selecting resources of the first available type.
      selectResourceType(resourcePicker, resourceTypes.length > 0 ? resourceTypes[0] : 'unknown');
    }

    return resourcePicker;
  }

  function addResourceType(resourcePicker ,resourceTypeId) {
    const resourceType = $resource.types[resourceTypeId] || {
      label: resourceTypeId,
      icon: $resource.types.unknown.icon,
    };
    // There is at least one resource type. Make sure it's visible.
    resourcePicker.find('.input-wrapper').attr('class', 'input-group').children('.input-group-btn').show();
    const resourceTypeList = resourcePicker.find('.resourceTypes');
    if (resourceTypeList.children().length === 1) {
      // There are multiple resource types. We need to show the resource type list.
      const resourceTypeButton = resourceTypeList.next('.resourceType');
      resourceTypeButton.prependTo(resourceTypeButton.parent()).nextAll().show();
    }
    const resourceTypeElement = $('<li><a href="#"><span class="icon"></span></a></li>');
    resourceTypeElement.appendTo(resourceTypeList).find('a').attr({
      'data-id': resourceTypeId,
      'data-placeholder': resourceType.placeholder
    }).children('.icon').after(document.createTextNode(' ' + resourceType.label))
      .replaceWith(resourceType.icon.render());
  }

  function addResourcePickerBehaviour(input, resourcePicker) {
    // Resource type behaviour.
    resourcePicker.on('click', '.resourceTypes a', maybeChangeResourceType)
      .on('changeResourceType', (event, data) => updateResourceSuggester(resourcePicker, data))
      .on('keydown', stopPropagationWhenUsingSuggest);

    // Dedicated resource pickers.
    const resourceTypeButton = resourcePicker.find('button.resourceType');
    resourceTypeButton.on('click', () => openPicker(resourceTypeButton));

    // Update the picker value when the resource reference input value changes.
    const resourceReferenceInput = resourcePicker.find('input.resourceReference');
    resourceReferenceInput.on('change', () => {
      input.val(resourceTypeButton.val() + ':' + resourceReferenceInput.val());
    });
  }

  function stopPropagationWhenUsingSuggest(event) {
    if (event.which === 13 /* Enter */) {
      if ($(event.target).closest('.resourcePicker').find('input.resourceReference').data('selectize')) {
        // Stop the event propagation if the key is handled by the suggester: enter key is used to select a suggestion.
        event.stopPropagation();
      }
    }
  }

  function maybeChangeResourceType(event) {
    event.preventDefault();
    const selectedResourceType = $(event.target).closest('a');
    const resourcePicker = selectedResourceType.closest('.resourcePicker');
    const resourceTypeButton = resourcePicker.find('button.resourceType');
    const oldValue = resourceTypeButton.val();
    const newValue = selectedResourceType.attr('data-id');
    if (newValue === oldValue) {
      return;
    }
    resourceTypeButton.val(newValue);
    const disabled = typeof $resource.pickers[newValue] !== 'function';
    resourceTypeButton.prop('disabled', disabled);
    if (disabled) {
      resourceTypeButton.attr('title', selectedResourceType.text().trim());
      // Just in case someone checks the disabled attribute instead of the property.
      resourceTypeButton.attr('disabled', 'disabled');
    } else {
      resourceTypeButton.attr('title', translations.get(newValue + '.hint'));
    }
    resourceTypeButton.find('.icon').replaceWith(selectedResourceType.find('.icon').clone());
    resourcePicker.find('.resourceReference').attr('placeholder', selectedResourceType.attr('data-placeholder'));
    resourcePicker.trigger('changeResourceType', {oldValue, newValue});
  }

  function openPicker(resourceTypeButton) {
    const resourcePicker = resourceTypeButton.closest('.resourcePicker');
    let resourceReference = getResource(resourcePicker).reference;
    // Use the selected resource if it matches the currently selected resource type, otherwise use the resource
    // reference typed in the resource picker input.
    if (resourceReference.type !== resourceTypeButton.val()) {
      resourceReference = {
        type: resourceTypeButton.val(),
        reference: resourcePicker.find('input.resourceReference').val()
      };
    }
    $resource.pickers[resourceReference.type](resourceReference, getBaseEntityReference(resourcePicker))
      .done(resource => selectResource(resourcePicker, resource));
  }

  function getBaseEntityReference(resourcePicker) {
    return resourcePicker.data('options')?.base;
  }

  function selectResource(resourcePicker, resource) {
    // Update the original resource reference input which is used to get the resource picker value.
    const input = resourcePicker.prev('input');
    if (resource) {
      // The given resource was selected from a dedicated picker or from a list of suggestions.
      const value = resource.reference.type + ':' + resource.reference.reference;
      input.val(value).trigger('selectResource', resource);
    } else {
      // The resource selection was cleared. No resource is selected.
      input.val('');
    }
  }

  function onSelectResource(event, resource) {
    const resourcePicker = $(event.target).nextAll('div.resourcePicker').first();
    selectResourceType(resourcePicker, resource.reference.type);
    selectResourceReference(resourcePicker, resource);
  }

  function getResource(resourcePicker) {
    const input = resourcePicker.prev('input');
    let resourceReference = input.val();
    const separatorIndex = resourceReference.indexOf(':');
    let resourceType = resourceReference.substr(0, separatorIndex);
    resourceReference = resourceReference.substr(separatorIndex + 1);
    if (resourceType.length === 0) {
      if (resourceReference.length === 0) {
        // Use the first resource type available.
        resourceType = input.next('.resourcePicker').find('.resourceTypes a').first().attr('data-id');
      }
      resourceType = resourceType || 'unknown';
    }
    return  {
      reference: {
        type: resourceType,
        reference: resourceReference
      }
    };
  }

  function selectResourceType(resourcePicker, resourceTypeId) {
    const resourceTypeButton = resourcePicker.find('.resourceType');
    if (resourceTypeId === resourceTypeButton.val()) {
      // The specified resource type is already selected.
      return;
    }
    let resourceTypeAnchor = resourcePicker.find('a').filter(function() {
      return $(this).attr('data-id') === resourceTypeId;
    });
    if (resourceTypeAnchor.length === 0) {
      // Unsupported resource type. We need to add it to the list before selecting it.
      addResourceType(resourcePicker, resourceTypeId);
      resourceTypeAnchor = resourcePicker.find('a').filter(function() {
        return $(this).attr('data-id') === resourceTypeId;
      });
    }
    resourceTypeAnchor.click();
  }

  function selectResourceReference(resourcePicker, resource) {
    const resourceReferenceInput = resourcePicker.find('input.resourceReference');
    const oldValue = resourceReferenceInput.val();
    let newValue = resource.reference.reference;

    const resourceTypeDefinition = $resource.types[resource.reference.type];
    const suggester = $resource.suggesters[resource.reference.type];
    if (suggester && (newValue || resourceTypeDefinition.allowEmptyReference)) {
      // When the resource is changed outside of the resource suggester, we need to make sure the resource suggester can
      // select the new resource. The resource suggester is configured to accept only known values, that either
      // correspond to existing resources or that obey the name strategy.
      resource = suggester.resolve(resource, getBaseEntityReference(resourcePicker));
      newValue = resource.value;

      // Make sure the selected resource has a corresponding option, so that it can be selected by Tom Select. We need
      // to do this because we disable the user provided (custom) values.
      const tomSelect = resourceReferenceInput.data('selectize');
      if (tomSelect && !tomSelect.options.hasOwnProperty(newValue)) {
        tomSelect.addOption(resource);
      }
    }

    if (oldValue !== newValue) {
      resourceReferenceInput.val(newValue).change();
    }
  }

  function saveAndRestoreSelectedReference(resourcePicker, data) {
    const resourceReferenceInput = resourcePicker.find('input.resourceReference');
    const selectedResourceByType = resourcePicker.data('selectedResourceByType') || {};
    resourcePicker.data('selectedResourceByType', selectedResourceByType);

    // Save the currently selected resource reference.
    const oldResourceType = data.oldValue;
    selectedResourceByType[oldResourceType] = resourceReferenceInput.val();

    // Restore the previously selected resource reference.
    const newResourceType = data.newValue;
    resourceReferenceInput.val(selectedResourceByType[newResourceType] || '').change();
  }

  function updateResourceSuggester(resourcePicker, data) {
    // First, destroy the suggester corresponding to the previous resource type.
    const resourceReferenceInput = resourcePicker.find('input.resourceReference');
    resourceReferenceInput.data('selectize')?.destroy();

    // Restore the previously selected reference for the new resource type only after the suggester corresponding to the
    // previous resource type is destroyed, so it doesn't react to the reference change.
    saveAndRestoreSelectedReference(resourcePicker, data);

    const suggester = $resource.suggesters[data.newValue];
    if (!suggester) {
      return;
    }

    // Then, initialize the suggester corresponding to the new resource type.
    resourceReferenceInput.xwikiSelectize({
      // Single selection.
      maxItems: 1,
      // Use a dedicated search field to avoid matching 'WebHome' for document resources.
      searchField: ['searchValue', 'label', 'hint'],
      shouldLoad: () => true,
      load: (resourceReference, callback) => {
        suggester.retrieve({
          type: data.newValue,
          reference: resourceReference
        }, getBaseEntityReference(resourcePicker)).then(callback).catch((error) => {
          console.error("Failed to retrieve resource suggestions:", error);
          callback([]);
        });
      },
      loadSelected: function(resourceReference, callback) {
        suggester.retrieveSelected({
          type: data.newValue,
          reference: resourceReference
        }, getBaseEntityReference(resourcePicker)).then(suggestions => {
          for (let suggestion of suggestions) {
            // Double check that the suggested resource is still selected.
            if (this.items.includes(suggestion.value)) {
              // Update the resource picker value. This ensures that the picker returns a relative reference. We do this
              // here because the change event (see below) is not triggered for the initial selection (so neither when
              // the suggest input is re-initialized after the resource type is changed).
              selectResource(resourcePicker, suggestion);
            }
          }
          callback(suggestions);
        }).catch((error) => {
          console.error("Failed to retrieve the selected resource:", error);
          callback([]);
        });
      }
    });

    const tomSelect = resourceReferenceInput.data('selectize');
    tomSelect.on('change', () => {
      const selectedResource = tomSelect.items.length > 0 ? tomSelect.options[tomSelect.items[0]] : null;
      selectResource(resourcePicker, selectedResource);
    });

    // Remove the create entity suggestions when the user types a new query, if they are not selected.
    tomSelect.on('type', () => {
      tomSelect.clearOptions(option => !option.toCreate);
    });

    // For create entity suggestions, we show the "Create ..." label inside the dropdown, and the future resource name
    // after the suggestion is selected.
    const originalRenderItem = tomSelect.settings.render.item;
    tomSelect.settings.render.item = function(option, ...args) {
      const item = originalRenderItem.call(this, option, ...args);
      if (option?.labelWhenSelected) {
        // Show a different label after the suggestion is selected.
        item.find('.xwiki-selectize-option-label').text(option.labelWhenSelected);
      }
      return item;
    };
  }

  $.fn.resourcePicker = function(options) {
    return this.each(function() {
      if (!$(this).next().is('div.resourcePicker')) {
        createResourcePicker($(this), options);
      }
    });
  };
});

define('xwiki-wysiwyg-resource-picker-bundle', [
  'xwiki-wysiwyg-resource-picker',
  'xwiki-wysiwyg-entity-resource-picker',
  'xwiki-wysiwyg-entity-resource-suggester'
], function() {});
