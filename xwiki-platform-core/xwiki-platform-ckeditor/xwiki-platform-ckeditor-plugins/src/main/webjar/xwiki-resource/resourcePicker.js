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
define('resourcePickerTranslationKeys', [], [
  'attach.hint',
  'doc.hint',
  'dropdown.toggle.title'
]);

define('resourcePicker', [
  'jquery', 'resource', 'l10n!resourcePicker', 'bootstrap3-typeahead'
], function($, $resource, translations) {
  'use strict';

  var resourcePickerTemplate =
    '<div class="resourcePicker">' +
      '<div class="input-group">' +
        '<input type="text" class="resourceReference" />' +
        '<div class="input-group-btn">'+
          '<button type="button" class="resourceType btn btn-default">' +
            '<span class="icon">' +
          '</button>' +
    '<button type="button" class="btn btn-default dropdown-toggle" data-toggle="dropdown">' +
    '<span class="sr-only"></span>' +
            '<span class="caret"></span>' +
          '</button>' +
          '<ul class="resourceTypes dropdown-menu dropdown-menu-right"></ul>' +
        '</div>' +
      '</div>' +
      '<div class="resourceDisplay"></div>' +
    '</div>';

  var createResourcePicker = function(element, options) {
    options = options || {};

    var resourcePicker = $(resourcePickerTemplate);
    resourcePicker.find('button.dropdown-toggle').first().attr('title', translations.get('dropdown.toggle.title'));
    resourcePicker.find('button.dropdown-toggle .sr-only').first().text(translations.get('dropdown.toggle.title'));
    resourcePicker.data("options", options);
    element.on('selectResource', onSelectResource).hide().after(resourcePicker);

    // Compute the list of supported resource types.
    var resourceTypes = $(element).attr('data-resourceTypes') || options.resourceTypes || [];
    if (typeof resourceTypes === 'string') {
      resourceTypes = resourceTypes.split(/\s*,\s*/);
    }

    // Initialize the layout.
    if (resourceTypes.length === 0) {
      resourcePicker.find('.input-group').attr('class', 'input-wrapper').children('.input-group-btn').hide();
    } else if (resourceTypes.length === 1) {
      var resourceTypeButton = resourcePicker.find('.resourceType');
      // Move the resource type button at the end in order to have round borders, and hide the resource type drop down.
      resourceTypeButton.appendTo(resourceTypeButton.parent()).prevAll().hide();
    }

    // Populate the Resource Type drop down.
    resourceTypes.forEach(addResourceType, resourcePicker);

    addResourcePickerBehaviour(resourcePicker);

    // Initialize the resource selection.
    element.trigger('selectResource');

    return resourcePicker;
  };

  var addResourceType = function(resourceTypeId) {
    var resourceType = $resource.types[resourceTypeId] || {
      label: resourceTypeId,
      icon: 'glyphicon glyphicon-question-sign'
    };
    // There is at least one resource type. Make sure it's visible.
    this.find('.input-wrapper').attr('class', 'input-group').children('.input-group-btn').show();
    var resourceTypeList = this.find('.resourceTypes');
    if (resourceTypeList.children().length === 1) {
      // There are multiple resource types. We need to show the resource type list.
      var resourceTypeButton = resourceTypeList.next('.resourceType');
      resourceTypeButton.prependTo(resourceTypeButton.parent()).nextAll().show();
    }
    var resourceTypeElement = $('<li><a href="#"><span class="icon"></span></a></li>');
    resourceTypeElement.appendTo(resourceTypeList).find('a').attr({
      'data-id': resourceTypeId,
      'data-placeholder': resourceType.placeholder
    }).children('.icon').addClass(resourceType.icon)
      .after(document.createTextNode(' ' + resourceType.label));
  };

  var addResourcePickerBehaviour = function(resourcePicker) {
    // Resource type behaviour.
    resourcePicker.on('click', '.resourceTypes a', maybeChangeResourceType)
      .on('changeResourceType', clearOrRestoreSelectedReference)
      .on('changeResourceType', updateResourceSuggester);

    // Dedicated resource pickers.
    var resourceTypeButton = resourcePicker.find('button.resourceType');
    resourceTypeButton.on('click', openPicker);

    var resourceDisplay = resourcePicker.find('.resourceDisplay');
    var resourceReferenceInput = resourcePicker.find('input.resourceReference');
    resourceDisplay.on('click', '.remove', function(event) {
      resourceDisplay.addClass('hidden').empty();
      resourceReferenceInput.change();
    });
    resourceDisplay.on('click', 'a', function(event) {
      // Prevent the users from leaving the edit mode by mistake.
      event.preventDefault();
    });
    resourceReferenceInput.on('change', function(event) {
      // Update the original resource reference input if there's no resource displayed.
      if (resourceDisplay.hasClass('hidden')) {
        // We don't fire the selectResource event because we don't need to update the resource picker display.
        resourcePicker.prev('input').val(resourceTypeButton.val() + ':' + resourceReferenceInput.val());
      }
    });
    // Note that we don't register the event listener directly on the text input because all the keydown event listeners
    // are being removed when we destroy the typeahead (suggest). The good part is that we don't have to test if the
    // list of suggestions is visible because there is another listener below that does this and stops the event
    // propagation.
    resourcePicker.on('keydown', 'input.resourceReference', function(event) {
      // Trigger the change event when pressing the Enter key in order to update the original resource reference input,
      // because the Enter key is often used as a shortcut for submitting the typed value.
      if (event.which === 13) {
        $(event.target).change();
      }
    });
    // Make sure the original resource reference input is updated before its value is retrieved.
    resourcePicker.prev('input').on('beforeGetValue', function() {
      resourceReferenceInput.change();
    });
  };

  var maybeChangeResourceType = function(event) {
    event.preventDefault();
    var selectedResourceType = $(this);
    var resourcePicker = selectedResourceType.closest('.resourcePicker');
    var resourceTypeButton = resourcePicker.find('button.resourceType');
    var oldValue = resourceTypeButton.val();
    var newValue = selectedResourceType.attr('data-id');
    if (newValue === oldValue) {
      return;
    }
    resourceTypeButton.val(newValue);
    var disabled = typeof $resource.pickers[newValue] !== 'function';
    resourceTypeButton.prop('disabled', disabled);
    if (disabled) {
      resourceTypeButton.attr('title', selectedResourceType.text().trim());
      // Just in case someone checks the disabled attribute instead of the property.
      resourceTypeButton.attr('disabled', 'disabled');
    } else {
      resourceTypeButton.attr('title', translations.get(newValue + '.hint'));
    }
    resourceTypeButton.find('.icon').attr('class', selectedResourceType.find('.icon').attr('class'));
    resourcePicker.find('.resourceReference').attr('placeholder', selectedResourceType.attr('data-placeholder'));
    resourcePicker.trigger('changeResourceType', {
      oldValue: oldValue,
      newValue: newValue
    });
  };

  var openPicker = function(event) {
    var resourceTypeButton = $(this);
    var resourcePicker = resourceTypeButton.closest('.resourcePicker');
    var resourceReference = getResourceReference(resourcePicker.prev('input'));
    // Use the selected resource if it matches the currently selected resource type, otherwise use the resource
    // reference typed in the resource picker input.
    if (resourceReference.type !== resourceTypeButton.val()) {
      resourceReference = {
        type: resourceTypeButton.val(),
        reference: resourcePicker.find('input.resourceReference').val()
      };
    }
    var base = (resourcePicker.data("options") || {}).base;
    $resource.pickers[resourceReference.type](resourceReference, base)
      .done(selectResource.bind(resourcePicker));
  };

  var selectResource = function(resource) {
    // Update the original resource reference input and also the resource picker display because the resource was
    // selected from outside (e.g. from a dedicated picker or from a list of suggestions).
    this.prev('input').val(resource.reference.type + ':' + resource.reference.reference)
      .trigger('selectResource', resource);
  };

  var onSelectResource = function(event, resource) {
    resource = resource || {
      reference: getResourceReference($(event.target))
    };
    var resourcePicker = $(event.target).nextAll('div.resourcePicker').first();
    selectResourceType(resourcePicker, resource.reference.type);
    selectResourceReference(resourcePicker, resource.reference);
  };

  var getResourceReference = function(input) {
    var resourceReference = input.val();
    var separatorIndex = resourceReference.indexOf(':');
    var resourceType = resourceReference.substr(0, separatorIndex);
    resourceReference = resourceReference.substr(separatorIndex + 1);
    if (resourceType.length === 0) {
      if (resourceReference.length === 0) {
        // Use the first resource type available.
        resourceType = input.next('.resourcePicker').find('.resourceTypes a').first().attr('data-id');
      }
      resourceType = resourceType || 'unknown';
    }
    return {
      type: resourceType,
      reference: resourceReference,
      // Don't display any reference if the text input is empty.
      isNew: separatorIndex < 0 && resourceReference.length === 0
    };
  };

  var selectResourceType = function(resourcePicker, resourceTypeId) {
    var resourceTypeButton = resourcePicker.find('.resourceType');
    if (resourceTypeId === resourceTypeButton.val()) {
      // The specified resource type is already selected.
      return;
    }
    var resourceTypeAnchor = resourcePicker.find('a').filter(function() {
      return $(this).attr('data-id') === resourceTypeId;
    });
    if (resourceTypeAnchor.length === 0) {
      // Unsupported resource type. We need to add it to the list before selecting it.
      addResourceType.call(resourcePicker, resourceTypeId);
      resourceTypeAnchor = resourcePicker.find('a').filter(function() {
        return $(this).attr('data-id') === resourceTypeId;
      });
    }
    resourceTypeAnchor.click();
  };

  var selectResourceReference = function(resourcePicker, resourceReference) {
    var resourceReferenceInput = resourcePicker.find('.resourceReference');
    var resourceDisplayContainer = resourcePicker.find('.resourceDisplay');
    var displayer = $resource.displayers[resourceReference.type];
    if (typeof displayer === 'function' && !resourceReference.isNew && (resourceReference.reference.length > 0 ||
        $resource.types[resourceReference.type].allowEmptyReference)) {
      resourceReferenceInput.val('');
      resourceDisplayContainer.empty().addClass('loading').removeClass('hidden');
      displayer(resourceReference).done(function(resourceDisplay) {
        // Empty the container before appending the resource display because we don't cancel the previous (unfinished)
        // display requests. The displayer could handle this itself but we would need to pass additional information
        // (something to identify the resource picker that made the display request).
        resourceDisplayContainer.empty().removeClass('loading').attr({
          'data-resourceType': resourceReference.type,
          'data-resourceReference': resourceReference.reference
        }).append(resourceDisplay).removeClass('hidden');
      }).fail(function() {
        resourceReferenceInput.val(resourceReference.reference);
        resourceDisplayContainer.addClass('hidden');
      });
    } else {
      resourceDisplayContainer.addClass('hidden').empty();
      resourceReferenceInput.val(resourceReference.reference);
    }
  };

  var clearOrRestoreSelectedReference = function(event, data) {
    var resourcePicker = $(this);
    var resourceDisplayContainer = resourcePicker.find('.resourceDisplay');
    if (resourceDisplayContainer.attr('data-resourceType') === data.newValue &&
        !resourceDisplayContainer.is(':empty')) {
      // Restore the selected resource.
      resourceDisplayContainer.removeClass('hidden');
      resourcePicker.prev('input').val(resourceDisplayContainer.attr('data-resourceType') + ':' +
        resourceDisplayContainer.attr('data-resourceReference'));
    } else {
      // Clear the selected resource.
      // Hide the resource display but keep its content because we want to restore it later.
      resourceDisplayContainer.addClass('hidden');
      // Update the hidden reference input based on what is available on the resource picker input.
      resourcePicker.find('input.resourceReference').change();
    }
  };

  var updateResourceSuggester = function(event, data) {
    var resourcePicker = $(this);
    var resourceReferenceInput = resourcePicker.find('input.resourceReference');
    resourceReferenceInput.typeahead('destroy');
    var suggester = $resource.suggesters[data.newValue];
    if (suggester) {
      var base = (resourcePicker.data("options") || {}).base;
      resourceReferenceInput.on('keydown', stopPropagationIfShowingSuggestions).typeahead({
        afterSelect: selectResource.bind(resourcePicker),
        delay: 500,
        displayText: function(resource) {
          // HACK: The string returned by this function is passed to the highlighter where we need to access all the
          // resource properties in order to be able to display the resource suggestion.
          // jshint -W053
          var reference = new String(resource.reference.reference);
          reference.__resource = resource;
          return reference;
        },
        highlighter: !suggester.display || function(resourceReference) {
          return suggester.display(resourceReference.__resource);
        },
        matcher: function(resource) {
          // By default, we assume the suggestions are already filtered.
          return true;
        },
        minLength: 0,
        sorter: function(resources) {
          // By default, we assume the suggestions are already sorted.
          return resources;
        },
        source: function(resourceReference, callback) {
          suggester.retrieve({
            type: data.newValue,
            reference: resourceReference
          }, base).done(callback);
        }
      });
    } else {
      resourceReferenceInput.off('keydown', stopPropagationIfShowingSuggestions);
    }
  };

  var stopPropagationIfShowingSuggestions = function(event) {
    if ((event.which === 27 /* ESC */ || event.which === 13 /* Enter */) &&
        $(this).next('.dropdown-menu').filter(':visible').length > 0) {
      // Stop the event propagation if the key is handled by the suggester.
      // Enter key is used to select a suggestion. Escape key is used to hide the suggestions.
      event.stopPropagation();
    }
  };

  $.fn.resourcePicker = function(options) {
    return this.each(function() {
      if (!$(this).next().is('div.resourcePicker')) {
        createResourcePicker($(this), options);
      }
    });
  };
});

define('resourcePicker.bundle', [
  'resourcePicker',
  'entityResourcePicker',
  'entityResourceSuggester',
  'entityResourceDisplayer'
], function() {});
