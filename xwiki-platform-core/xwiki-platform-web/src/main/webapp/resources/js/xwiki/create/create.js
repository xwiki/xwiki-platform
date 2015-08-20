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

/**
 * Handles client-side input validation and interraction for the create action UI.
 **/
require(['jquery', 'xwiki-events-bridge'], function(jQuery) {
  // Augment the XWiki public object with the createPage object.
  XWiki = window.XWiki || {};
  XWiki.createPage = {
    spaceValidator : {},
    pageValidator : {},
    currentSpaceInclusionParameters : {},

    titleInput : {},
    spaceReferenceInput : {},
    nameInput : {},
    locationContainer : {},

    /**
     * Initialize and run.
     **/
    initialize : function() {
      // Initialize the space and page input validators.
      this.initSpaceValidator();
      this.initPageValidator();

      // Initialize the form's inputs.
      this.titleInput = jQuery('#title');
      this.spaceReferenceInput = jQuery('#spaceReference');
      this.nameInput = jQuery('#name');
      // Note: Using getElementByID to avoid the clash with the 'hierarchy' velocity macro
      this.locationContainer = jQuery(document.getElementById('hierarchy'));

      // Update the Page Name input as the title input is being typed.
      this.titleInput.on('input', this.updateLocationAndNameFromTitleInput.bind(this));
      this.nameInput.on('input', this.updateLocationFromNameInput.bind(this));
      this.spaceReferenceInput.on('input xwiki:suggest:selected', this.updateLocationFromSpaceReference.bind(this));

      // Update the location with whatever the initial value of the title is.
      this.updateLocationFromTitleInput();

      // Clean the output of the hierarchy macro when it should display a top level document.
      if (this.spaceReferenceInput.val().length == 0) {
        this.updateLocationFromSpaceReference();
      }

      // Validate the form.
      this.spaceValidator.validate();
      this.pageValidator.validate();

      // Show the location edit options when pressing the pencil button.
      var locationEdit = jQuery('#location-edit');
      var locationEditToggle = jQuery('#location-edit-toggle');
      locationEditToggle.on('click', function(event) {
        event.preventDefault();
        // Note: Using toggleClass() instead of toggle() because using the 'hidden' class
        // allows us to have the element hidden by default more easily from Velocity.
        locationEdit.toggleClass('hidden');
      });

      // If the form is not valid on submission and the location edit is hidden, make sure to display it so that
      // validation errors are also displayed.
      var self = this;
      jQuery('form').on("submit", function(event) {
        var isValid = LiveValidation.massValidate([self.spaceValidator, self.pageValidator]);
        if (!isValid && locationEdit.hasClass('hidden')) {
          locationEditToggle.click();
        }
      });
    },

    /**
      * Create Name Field Validator.
      */
    initPageValidator : function() {
      var pageInput = $('name');
      this.pageValidator = new LiveValidation(pageInput, { validMessage: "$services.localization.render('core.validation.valid.message')" });
      // We use a custom validation in order to handle the default value on browsers that don't support the placeholder attribute.
      this.pageValidator.displayMessageWhenEmpty = true;
      this.pageValidator.add(Validate.Custom, {
        failureMessage: "$services.localization.render('core.validation.required.message')",
        against: function(value) {
          return !pageInput.hasClassName('empty') && typeof value == 'string' && value.strip().length > 0;
        }
      });
    },

    /**
      * Create Space Reference Field Validator.
      */
    initSpaceValidator : function() {
      var spaceReferenceInput = $('spaceReference');
      var terminalCheckbox = $('terminal');
      this.spaceValidator = new LiveValidation($('spaceReference'), { validMessage: "$services.localization.render('core.validation.valid.message')" });
      this.spaceValidator.displayMessageWhenEmpty = true;
      this.spaceValidator.add(Validate.Custom, {
        failureMessage: "$services.localization.render('core.validation.required.message')",
        against: function(value) {
          if (terminalCheckbox.checked) {
            // Space reference is required for terminal documents
            return typeof value == 'string' && value.strip().length > 0;
          } else {
            // Space reference can be empty for non-terminal documents
            return true;
          }
        }
      });

      // Trigger validation when the terminal status changes.
      terminalCheckbox.observe('change', function() {
        this.spaceValidator.validate();
      }.bind(this));

      // Update the allowed spaces based on the selected template provider.
      var self = this;
      jQuery('input[name="templateprovider"]').each(function(index) {
        this.on('change', function() {
          self.updateSpaceValidatorFromTemplateProviderInput(this);
        });
      });

      // Make sure the spaceValidator is properly initialized when loading the page.
      var initiallyCheckedTemplateProviderInput = jQuery('input[name="templateprovider"][checked="checked"]');
      if (initiallyCheckedTemplateProviderInput.length == 2) {
        // When a templateprovider is specified, we end up with 2 "checked" fields, so we choose the second one, which is the actually selected one.
        initiallyCheckedTemplateProviderInput = initiallyCheckedTemplateProviderInput[1];
      }
      this.updateSpaceValidatorFromTemplateProviderInput(initiallyCheckedTemplateProviderInput);
    },

    /**
     * Update the spaceValidator with the list of allowed spaces obtained from the specified input.
     *
     * @param input the input from which to read the data attributes containing the list of allowed spaces and error
     *              message.
     **/
    updateSpaceValidatorFromTemplateProviderInput(input) {
      var allowedSpaces = JSON.parse(jQuery(input).attr('data-allowed-spaces'));
      var message = jQuery(input).attr('data-allowed-spaces-message');
      this.updateSpaceValidator(allowedSpaces, message);
    },

    /**
      * Update Space Field Validator, allows to specify a list of allowed spaces for the field.
      */
    updateSpaceValidator : function(values, failureMessage) {
      if(values.length > 0) {
        this.currentSpaceInclusionParameters = {
          within: values,
          failureMessage: failureMessage
        };
        this.spaceValidator.add(Validate.Inclusion, this.currentSpaceInclusionParameters);
      } else {
        this.spaceValidator.remove(Validate.Inclusion, this.currentSpaceInclusionParameters);
      }
      this.spaceValidator.validate();
    },

    /**
     * Compute a page name from a given title.
     **/
    getPageName : function(title) {
      // Note: By default, we are just using the unaltered title as page name.
      // Something more elaborate can be done here, should anyone need it.
      var result = title;
      return result;
    },

    /**
     * Update the last element in the location preview.
     * 
     * @param value the value to use
     */
    updateLocationLastElement : function(value) {
      var lastElement = this.locationContainer.children('.preview')[0];
      if (lastElement) {
        jQuery(lastElement).text(value);
      } else {
        this.locationContainer.append('<li class="preview active">' + value + '</li>');
      }
    },

    /**
     * Event handler for the title input that updates both the location preview's last element and the name input.
     **/
    updateLocationAndNameFromTitleInput : function(event) {
      // Update the location preview.
      this.updateLocationFromTitleInput();

      // Update the name field.
      var title = this.titleInput.val();
      var name = this.getPageName(title);
      this.nameInput.val(name);
      // Trigger page name validation.
      this.pageValidator.validate();
    },

    /**
     * Update the location with the value from the title input.
     **/
    updateLocationFromTitleInput : function() {
      var title = this.titleInput.val();
      this.updateLocationLastElement(title);
    },

    /**
     * Event handler for the name input that updates the location preview's last element.
     **/
    updateLocationFromNameInput : function(event) {
      var title = this.titleInput.val();

      // Only update the location from the name when there is no title provided.
      if (!title) {
        var name = this.nameInput.val();
        this.updateLocationLastElement(name);
      }
    },

    /**
     * Computes the URL that retrieves the location preview HTML code for a given space reference.
     *
     * @param spaceReferenceString the space reference string to use
     * @return the URL String
     **/
    getSpaceHierarchyURL : function(spaceReferenceString) {
      var resolvedSpaceReference = XWiki.Model.resolve(spaceReferenceString, XWiki.EntityType.SPACE);

      var spacePath = resolvedSpaceReference.getReversedReferenceChain().map(function(entityReference) {
        return encodeURIComponent(entityReference.name);
      }).join('/');

      var url = XWiki.Document.URLTemplate;
      url = url.replace('__space__', spacePath);
      url = url.replace('__page__', '');
      url = url.replace('__action__', 'get');
      url += '?xpage=hierarchy_reference';

      return url;
    },

    /**
     * Event handler for the space reference input that updates the location preview's space part.
     **/
    updateLocationFromSpaceReference : function(event, data) {
      var spaceReference;
      if (data) {
        // Suggest widget selection event.
        spaceReference = data.value;
      } else {
        // Regular input event.
        spaceReference = this.spaceReferenceInput.val();
      }

      if (spaceReference.length == 0) {
        // Special handling for top level documents because the hierarchy_reference.vm template is not useful here.
        // We can not build an URL without the space, so we do the work directly on the DOM.
        XWiki.createPage.locationContainer.find('li').each(function(index) {
          // Remove all items except the first (home icon).
          if (index > 0) {
            jQuery(this).remove();
          }
        });

        // Update the document part of the new location.
        this.updateLocationFromNameInput();
      } else {
        var spaceHierarchyURL = this.getSpaceHierarchyURL(spaceReference);
        jQuery.get(spaceHierarchyURL, function(data) {
          // Update the space reference part of the new location.
          var newLocationContainer = jQuery(data);
          this.locationContainer.replaceWith(newLocationContainer);
          this.locationContainer = newLocationContainer;

          // Update the document part of the new location.
          this.updateLocationFromNameInput();
        }.bind(this));
      }
    }

  };

  // Initialize now or when the DOM finishes loading.
  ((XWiki.isInitialized && XWiki.createPage.initialize()) || jQuery(document).on('xwiki:dom:loaded', XWiki.createPage.initialize));
});