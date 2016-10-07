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

// Location Tree Picker
require(["$!services.webjars.url('org.xwiki.platform:xwiki-platform-tree-webjar', 'require-config.min.js', {'evaluate': true})"], function() {
  require(['tree'], function($) {
    $('.location-picker').each(function() {
      var picker = $(this);
      var trigger = picker.find('.location-action-pick');
      var modal = picker.find('.modal');
      var treeElement = modal.find('.location-tree');
      var selectButton = modal.find('.modal-footer .btn-primary');

      trigger.click(function(event) {
        event.preventDefault();
        modal.modal();
      });

      modal.on('shown.bs.modal', function(event) {
        // Open to the specified node only once. Preserve the tree state otherwise.
        var openToNodeId = trigger.attr('data-openTo');
        if (openToNodeId && openToNodeId !== modal.data('openTo')) {
          modal.data('openTo', openToNodeId);
        } else {
          openToNodeId = false;
        }
        var tree = $.jstree.reference(treeElement);
        if (!tree) {
          // Initialize the tree and hook the event listeners.
          tree = treeElement.xtree({
            core: {
              multiple: treeElement.data('multiple') === 'true'
            }
          }).one('ready.jstree', function(event, data) {
            openToNodeId && data.instance.openTo(openToNodeId);
          }).on('changed.jstree', function(event, data) {
            selectButton.prop('disabled', data.selected.size() === 0);
          }).on('dblclick', '.jstree-anchor', function() {
            selectButton.click();
          });
        } else if (openToNodeId) {
          tree.deselect_all();
          tree.close_all();
          tree.openTo(openToNodeId);
        }
      });

      selectButton.click(function() {
        modal.modal('hide');
        modal.triggerHandler('xwiki:locationTreePicker:select', {
          'tree': $.jstree.reference(treeElement)
        });
      });
    });
  });
});

// Document Tree Picker
require(['jquery', 'xwiki-meta'], function($, xm) {
  $('.location-picker').each(function() {
    var picker = $(this);
    // The wiki field can be either a select (drop down) or an input (text or hidden).
    var wikiField = picker.find('.location-wiki-field');
    var parentField = picker.find('input.location-parent-field');

    picker.find('.location-action-pick').click(function(event) {
      var selectedValue = parentField.val();
      if (selectedValue) {
        var wiki = wikiField.val() || xm.wiki;
        var spaceReference = XWiki.Model.resolve(selectedValue, XWiki.EntityType.SPACE, [wiki]);
        var documentReference = new XWiki.EntityReference('WebHome', XWiki.EntityType.DOCUMENT, spaceReference);
        var openToNodeId = 'document:' + XWiki.Model.serialize(documentReference);
        $(this).attr('data-openTo', openToNodeId);
      }
    });

    picker.find('.modal').on('xwiki:locationTreePicker:select', function(event, data) {
      var selectedNodeId = data.tree.get_selected()[0];
      var separatorIndex = selectedNodeId.indexOf(':');
      var nodeType = selectedNodeId.substr(0, separatorIndex);
      var nodeStringReference = selectedNodeId.substr(separatorIndex + 1);
      var nodeReference = XWiki.Model.resolve(nodeStringReference, XWiki.EntityType.byName(nodeType));
      var wikiReference = nodeReference.extractReference(XWiki.EntityType.WIKI);
      wikiField.val(wikiReference ? wikiReference.name : '');
      var spaceReference = nodeReference.extractReference(XWiki.EntityType.SPACE);
      var localSpaceReference = spaceReference ? XWiki.Model.serialize(spaceReference.relativeTo(wikiReference)) : '';

      // Set the selected value and trigger and update of the location preview.
      parentField.val(localSpaceReference);
      // Notify interested listeners that we have new input. Note: PrototypeJS listeners will not be notified.
      parentField.triggerHandler('input');
    });
  });
});

// Live synchronization between the Title, Location, Wiki, Parent and Name (as you type)
require(['jquery', 'xwiki-meta', 'xwiki-events-bridge'], function($, xm) {
  $('.location-picker').each(function() {
    var picker = $(this);
    var titleInput = picker.find('input.location-title-field');
    // The wiki field can be either a select (drop down) or an input (text or hidden).
    var wikiField = picker.find('.location-wiki-field');
    var spaceReferenceInput = picker.find('input.location-parent-field');
    var nameInput = picker.find('input.location-name-field');
    var locationContainer = picker.find('.breadcrumb');
    // Input timeouts used to avoid handling too soon each individual letter, as the user types.
    var inputDelay = 500;
    var spaceReferenceInputTimeout;

    /**
     * Compute a page name from a given title.
     **/
    var getPageName = function(title) {
      // Note: By default, we are just using the unaltered title as page name.
      // Something more elaborate can be done here, should anyone need it.
      return title;
    };

    /**
     * Update the last element in the location preview.
     * 
     * @param value the value to use
     */
    var updateLocationLastElement = function(value) {
      var lastElement = locationContainer.children('.preview');
      if (lastElement.length === 0) {
        lastElement = $(document.createElement('li')).addClass('preview active').appendTo(locationContainer);
      }
      lastElement.text(value);
    };

    /**
     * Event handler for the title input that updates both the location preview's last element and the name input.
     **/
    var updateLocationAndNameFromTitleInput = function(event) {
      // Update the location preview.
      updateLocationFromTitleInput();
      // Update the name field.
      nameInput.val(getPageName(titleInput.val()));
    };

    /**
     * Update the location preview's last element with the value from the title input.
     **/
    var updateLocationFromTitleInput = function() {
      updateLocationLastElement(titleInput.val());
    };

    /**
     * Event handler for the name input that updates the location preview's last element.
     **/
    var updateLocationFromNameInput = function(event) {
      // Only update the location from the name when there is no title provided.
      if (!titleInput.val()) {
        updateLocationLastElement(nameInput.val());
      }
    };

    var updateLocationFromTitleOrNameInput = function() {
      var title = titleInput.val();
      updateLocationLastElement(title ? title : nameInput.val());
    };

    /**
     * Event handler for the space reference input that updates the location preview's space part.
     **/
    var updateLocationFromSpaceReference = function(event, data) {
      var spaceReference;
      if (data) {
        // Suggest widget selection event.
        spaceReference = data.value;
      } else {
        // Regular input event.
        spaceReference = spaceReferenceInput.val();
      }

      // Delay the execution in case the user is still typing.
      clearTimeout(spaceReferenceInputTimeout);
      spaceReferenceInputTimeout = setTimeout(function() {
        updateLocation(wikiField.val(), spaceReference);
      }, inputDelay);
    };

    var updateLocationFromWikiField = function(event) {
      // TODO: Don't reload the entire location when the wiki changes. We should be able to update only the wiki element,
      // but we need to be able to "detect" it (e.g. the breadcrumb should add some CSS classes on the path elements).
      updateLocation(wikiField.val());
    };

    var updateLocation = function(wiki, localSpaceReference) {
      wiki = wiki || wikiField.val();
      localSpaceReference = localSpaceReference || spaceReferenceInput.val();

      // We need to pass a document reference to the hierarchy_reference template and we cannot create a document
      // reference without the space reference. If the space reference is empty we use the current space reference and
      // we remove the extra path elements afterwards from the breadcrumb HTML.
      var spaceReference = XWiki.Model.resolve(localSpaceReference || xm.space, XWiki.EntityType.SPACE);
      var documentReference = new XWiki.EntityReference('WebHome', XWiki.EntityType.DOCUMENT, spaceReference);
      wiki && spaceReference.appendParent(new XWiki.WikiReference(wiki));

      $.post(getCurrentPageURL(), {
        'xpage': 'hierarchy_reference',
        'reference': XWiki.Model.serialize(documentReference)
      }, function(data) {
        // Update the space reference part of the new location.
        var newLocationContainer = $(data);
        locationContainer.replaceWith(newLocationContainer);
        locationContainer = newLocationContainer;

        // Remove all breadcrumb items that don't represent wikis if the space reference was empty.
        localSpaceReference || locationContainer.find('li').not('.wiki').remove();

        // Remove any redundant 'active' elements that we might inherit from the AJAX call, since the only active
        // element will be the page name preview that we create.
        locationContainer.find('.active').removeClass('active');

        // Update the document part of the new location.
        updateLocationFromTitleOrNameInput();
      });
    };

    var getCurrentPageURL = function() {
      var spaceReference = XWiki.Model.resolve(xm.space, XWiki.EntityType.SPACE);
      var spacePath = spaceReference.getReversedReferenceChain().map(function(entityReference) {
        return encodeURIComponent(entityReference.name);
      }).join('/');

      var url = XWiki.Document.URLTemplate;
      url = url.replace('__space__', spacePath);
      url = url.replace('__page__', xm.page);
      url = url.replace('__action__', 'get');

      return url;
    };

    // Synchronize the location fields while the user types.
    titleInput.on('input', updateLocationAndNameFromTitleInput);
    wikiField.change(updateLocationFromWikiField);
    nameInput.on('input', updateLocationFromNameInput);
    spaceReferenceInput.on('input xwiki:suggest:selected', updateLocationFromSpaceReference);

    // Clean the output of the hierarchy macro when it should display a top level document.
    if (!spaceReferenceInput.val()) {
      updateLocationFromSpaceReference();
    }

    // Update the location with whatever the initial value of the title is.
    if (!nameInput.val()) {
      updateLocationAndNameFromTitleInput();
    } else {
      updateLocationFromTitleInput();
    }

    // Show the location edit options when pressing the pencil button.
    var locationEdit = picker.find('.location-edit');
    picker.find('.location-action-edit').click(function(event) {
      event.preventDefault();
      // Note: Using toggleClass() instead of toggle() because using the 'hidden' class
      // allows us to have the element hidden by default more easily from Velocity.
      locationEdit.toggleClass('hidden');
    });
  });
});

//
// Client-side input validation
//
require(['jquery'], function($) {
  //
  // Generic Validation
  //
  var isSimplePicker = function(picker) {
    return picker.find('.location-actions').length > 0 && picker.find('.location-action-edit').length == 0;
  };

  var createPageValidator = function(picker) {
    var pageInput = picker.find('input.location-name-field');
    if (pageInput.length === 0) {
      return null;
    }

    var titleInput = picker.find('input.location-title-field');
    // The advanced location edit fields are not accessible to simple users.
    var isSimpleUser = titleInput.length > 0 && isSimplePicker(picker);
    var pageValidator = new LiveValidation(pageInput[0], {
      validMessage: "$services.localization.render('core.validation.valid.message')",
      // Show the validation message after the title input for simple users because they can't access the page input.
      insertAfterWhatNode: isSimpleUser ? titleInput[0] : pageInput[0]
    });
    // We use a custom validation in order to handle the default value on browsers that don't support the placeholder
    // attribute.
    pageValidator.displayMessageWhenEmpty = true;
    pageValidator.add(Validate.Custom, {
      failureMessage: "$services.localization.render('core.validation.required.message')",
      against: function(value) {
        return !pageInput.hasClass('empty') && typeof value === 'string' && value.strip().length > 0;
      }
    });

    // The page input is filled automatically when the user types in the title input so we should validate the page
    // input at the same time.
    titleInput.on('input', function() {
      // Validate after the value of the page input is set.
      setTimeout(function() {
        pageValidator.validate();
      }, 0);
    });

    return pageValidator;
  };

  var createSpaceValidator = function(picker) {
    var spaceReferenceInput = picker.find('input.location-parent-field');
    if (spaceReferenceInput.length > 0) {
      var breadcrumbContainer = picker.find('.breadcrumb-container');
      // The advanced location edit fields are not accessible to simple users.
      var isSimpleUser = breadcrumbContainer.length > 0 && isSimplePicker(picker);
      var spaceValidator = new LiveValidation(spaceReferenceInput[0], {
        validMessage: "$services.localization.render('core.validation.valid.message')",
        // Validating automatically only on submit to avoid double validation caused by jQuery-PrototypeJS event
        // triggering incompatibilities when setting the space reference with the tree picker. We are calling validate
        // manually in the 'input' handler to achieve the same behavior as if 'onlyOnBlur' was false.
        onlyOnBlur: true,
        insertAfterWhatNode: isSimpleUser ? breadcrumbContainer[0] : spaceReferenceInput[0]
      });
      spaceValidator.displayMessageWhenEmpty = true;
      return spaceValidator;
    } else {
      return null;
    }
  };

  var setAllowedValues = function(validator, values, failureMessage) {
    // Clean any previous existing values validators, using the previous parameters (if available).
    if (validator._customValuesParams) {
      validator.remove(Validate.Custom, validator._customValuesParams);
      delete validator._customValuesParams;
    }

    // If any values are specified, add a custom validator.
    if (values.length > 0) {
      // Store the parameters so we can later be able to remove them in a future call.
      validator._customValuesParams = {
        failureMessage: failureMessage,
        against: function(value) {
          for (var i=0; i<values.length; i++) {
            var allowedValue = values[i];
            // Must be exactly one of the allowed values of prefixed by the allowed value followed by a dot (i.e. parent space).
            if (allowedValue === value || value.indexOf(allowedValue + '.') === 0) {
              return true;
            }
          }

          // Does not validate for any of the allowed values.
          return false;
        }
      };
      validator.add(Validate.Custom, validator._customValuesParams);
    }
  };

  var addTerminalPageValidation = function(spaceValidator, terminalCheckbox) {
    spaceValidator.add(Validate.Custom, {
      failureMessage: "$services.localization.render('core.validation.required.message')",
      against: function(value) {
        if (terminalCheckbox.prop('checked')) {
          // Space reference is required for terminal documents.
          return typeof value === 'string' && value.strip().length > 0;
        } else {
          // Space reference can be empty for non-terminal documents.
          return true;
        }
      }
    });

    // Trigger validation when the terminal status changes.
    terminalCheckbox.change(function() {
      spaceValidator.validate();
    });
  };

  var synchChildrenWithTerminalPage = function(deepCheckbox, terminalCheckbox) {
    deepCheckbox.change(function() {
      deepCheckbox.prop('checked') && terminalCheckbox.prop('checked', false);
    });
    terminalCheckbox.change(function() {
      terminalCheckbox.prop('checked') && deepCheckbox.prop('checked', false);
    });
  };

  var validators = [];
  $('.location-picker').each(function() {
    var picker = $(this);
    var pickerValidators = [];
    // Create the generic space and page input validators.
    var spaceValidator = createSpaceValidator(picker);
    if (spaceValidator) {
      pickerValidators.push(spaceValidator);
      picker.data('spaceValidator', spaceValidator);
    }
    var pageValidator = createPageValidator(picker);
    if (pageValidator) {
      pickerValidators.push(pageValidator);
      picker.data('pageValidator', pageValidator);
    }
    validators.push.apply(validators, pickerValidators);

    var locationEdit = picker.find('.location-edit');
    var locationEditToggle = picker.find('.location-action-edit');

    // If the form is not valid on submission and the location edit is hidden, make sure to display it so that
    // validation errors are also displayed.
    picker.closest('form').submit(function(event) {
      var isValid = LiveValidation.massValidate(pickerValidators);
      if (!isValid && locationEdit.hasClass('hidden')) {
        locationEditToggle.click();
      }
    });

    // Call validate() manually on the spaceValidator when we get input on the space reference field, because
    // LiveValidation (PrototypeJS) does not get notified about jQuery triggered events so we have to handle it
    // ourselves.
    var spaceReferenceInput = picker.find('input.location-parent-field');
    spaceReferenceInput.on('input', function() {
      spaceValidator.validate();
    });
  });

  //
  // Custom validation for the Create Page UI.
  //

  $('form#create').each(function() {
    var form = $(this);
    var picker = form.find('.location-picker');
    var spaceValidator = picker.data('spaceValidator');

    addTerminalPageValidation(spaceValidator, form.find('#terminal'));

    var updateSpaceValidatorFromTemplateProviderInput = function(input) {
      var restrictionsAreSuggestions = (input.attr('data-restrictions-are-suggestions') == "true");

      var allowedSpaces = [];
      var allowedSpacesData = input.attr('data-allowed-spaces');
      // Read the alowed spaces specified by the template provider, unless they are just suggestions in which case they
      // should be ignored by validation.
      if (!restrictionsAreSuggestions && allowedSpacesData) {
        allowedSpaces = $.parseJSON(input.attr('data-allowed-spaces'));
      }

      var message = input.attr('data-allowed-spaces-message');

      setAllowedValues(spaceValidator, allowedSpaces, message);
    };

    // Update the allowed spaces based on the selected template provider.
    form.find('.xwiki-select').on('xwiki:select:updated', function (event) {
      var type = $('input[name="type"]:checked');
      // Note: Even though the page type selector can provide elements that are not template providers (i.e.
      // data-type='template'), we still need to clear any previously set validations. The upside of this is that we
      // are also allowing these page types to specify 'allowed spaces', should they need it at some point.
      updateSpaceValidatorFromTemplateProviderInput(type);
      // Validate using the new configuration.
      spaceValidator.validate();
    });

    // Make sure the spaceValidator is properly initialized when loading the page.
    var initiallyCheckedTemplateProviderInput = form.find('input[name="templateprovider"]');
    if (initiallyCheckedTemplateProviderInput.length == 0) {
        // If there is no (hidden) input called 'templateprovider', then we should look at the selected value
        // in the xwiki selector widget
        initiallyCheckedTemplateProviderInput = form.find('.xwiki-select input[name="type"]:checked');
    }

    // Note that there could also be no template provider available, but we rely on jQuery's selectors here to avoid null values.
    updateSpaceValidatorFromTemplateProviderInput(initiallyCheckedTemplateProviderInput);
  });

  //
  // Custom validation for the Copy & Rename Page UI.
  //

  $('form#copy, form#rename').each(function() {
    var form = $(this);
    var picker = form.find('.location-picker');
    var spaceValidator = picker.data('spaceValidator');
    var terminalCheckbox = form.find('input[name="terminal"]');
    var deepCheckbox = form.find('input[name="deep"]');
    var languageSelect = form.find('select[name="language"]');

    addTerminalPageValidation(spaceValidator, terminalCheckbox);
    synchChildrenWithTerminalPage(deepCheckbox, terminalCheckbox);

    languageSelect.change(function() {
      if (languageSelect.val() === 'ALL') {
        deepCheckbox.prop('disabled', false);
      } else {
        deepCheckbox.prop({
          'checked': false,
          'disabled': true
        });
      }
    });
  });
});
