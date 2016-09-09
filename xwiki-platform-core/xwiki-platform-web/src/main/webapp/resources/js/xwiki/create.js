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
require(['jquery', 'xwiki-meta'], function($, xm) {
  $(document).ready(function() {

    var form = $('form#create');
    var nameField = form.find('input.location-name-field');
    var parentReferenceField = form.find('input.location-parent-field');
    var terminalCheckbox = form.find('input[name="tocreate"]');

    /**
     * Compute the page name of the target
     */
    var computeTargetPageName = function() {
      var documentReference = new XWiki.DocumentReference();
      // Test if the parent reference field exists
      if (parentReferenceField.length > 0) {
        var parentReference = parentReferenceField.val();
        if (parentReference == '') {
          // If there is no parent, then the document cannot be terminal.
          // There is a live validation rule for that and it displays the proper error, but we need this check here too
          // to avoid sending the form anyway.
          if (terminalCheckbox.prop('checked')) {
            return false;
          }
          documentReference = new XWiki.DocumentReference(xm.wiki, nameField.val(), 'WebHome');
        } else {
          var spaceReference = XWiki.Model.resolve(parentReference, XWiki.EntityType.SPACE);
          if (terminalCheckbox.prop('checked')) {
            documentReference = new XWiki.EntityReference(nameField.val(), XWiki.EntityType.DOCUMENT, spaceReference);
          } else {
            var parent = new XWiki.EntityReference(nameField.val(), XWiki.EntityType.SPACE, spaceReference);
            documentReference = new XWiki.EntityReference('WebHome', XWiki.EntityType.DOCUMENT, parent);
          }
        }
      } else {
        if ($('.modal-popup').length > 0) {
          // We are in the 'create page' popup
          var spaceReference = XWiki.Model.resolve($('#spaceReference').val(), XWiki.EntityType.SPACE);
          documentReference = new XWiki.EntityReference($('#name').val(), XWiki.EntityType.DOCUMENT, spaceReference);
        } else {
          // We are in the create page action, but with a page name already filled
          if (terminalCheckbox.prop('checked')) {
            documentReference = xm.documentReference.parent;
          } else {
            documentReference = xm.documentReference;
          }
        }
      }

      return XWiki.Model.serialize(documentReference.relativeTo(new XWiki.WikiReference(xm.wiki)));
    };

    /**
     * Add an hidden input that indicates which template to use
     */
    var setTemplateProvider = function (value) {
      // Note: the input might already exists if the user press the "back" button after having submitted the form once.
      var templateProvider = $('#templateprovider');
      if (templateProvider.length == 0) {
        // So we create it only if it does not already exist
        templateProvider = $('<input type="hidden" name="templateprovider" id="templateprovider"/>').appendTo(form);
      }
      templateProvider.attr('value', value);
    };

    /**
     * Set the correct template or redirect to the correct page when the form is submitted
     */
    form.submit(function() {
      // Get the type of the page to create
      var typeField = $('input[name="type"]:checked');
      var type = typeField.attr('data-type');

      // A blank document
      if (type == 'blank') {
        // The 'templateprovider' field is needed by the CreateAction, even empty
        setTemplateProvider('');
        return true;
      }

      // A document from a template
      if (type == 'template') {
        var templateName = typeField.val();
        setTemplateProvider(templateName);
        return true;
      }

      // An office document: we redirect to the office importer
      // TODO: handle this use-case with an extension point
      if (type == 'office') {
        // Verify that the target page name has been filled (only if the location picker is displayed).
        if (nameField.length > 0 && nameField.val().trim().length == 0) {
          return false;
        }
        // The office importer is a wiki page which takes the 'page' parameter.
        // So we compute this parameter and we redirect the form to the Office Importer document.
        var targetName = computeTargetPageName();
        if (targetName != false) {
          window.location = new XWiki.Document(new XWiki.DocumentReference(xm.wiki, ['XWiki'], 'OfficeImporter')).getURL('view', 'page=' + encodeURIComponent(targetName));
        }
        return false;
      }

    });

    /*
     * Terminal checkbox value updating when switching between document types.
     */
    var updateTerminalCheckboxFromTemplateProviderInput = function(input) {
      var pageShouldBeTerminalString = input.attr('data-terminal');
      var pageShouldBeTerminal = false;
      if (pageShouldBeTerminalString) {
        pageShouldBeTerminal = $.parseJSON(input.attr('data-terminal'));
      }
      // Set the default value for the page type.
      terminalCheckbox.prop('checked', pageShouldBeTerminal);
    };

    // Flag used to reset the parent reference to the current space reference when switching from a template provider
    // that employs restrictions back to one that does not, so that the value restricted by the previous provider
    // does not linger on to the ones without restrictions.
    var shouldResetParentReference = false;

    /*
     * Parent Reference value updating when switching between document types.
     */
    var updateParentReferenceFromTemplateProviderInput = function(input) {
      var currentParentReferenceValue = parentReferenceField.val();

      // Get the list of allowed spaces.
      var allowedSpacesData = input.attr('data-allowed-spaces');
      var allowedSpaces = [];
      if (allowedSpacesData) {
        allowedSpaces = $.parseJSON(input.attr('data-allowed-spaces'));
      }

      var newParentReferenceValue = currentParentReferenceValue;
      if (allowedSpaces.length > 0) {
        // Utility method to determine if a value is in the list of restrictions or is a child of an existing restriction.
        var isRestrictionOrChildOfRestriction = function(value) {
          for (var i=0; i<allowedSpaces.length; i++) {
            var allowedSpace = allowedSpaces[i];
            var prefix = allowedSpace + ".";
            if (value.indexOf(prefix) == 0 || value == allowedSpace) {
              // When the current value is a child of an existing restriction or actually in the list, use it.
              return true;
            }
          }

          return false;
        }

        // Determine what new value we will set for the parent reference, which needs to respect the restrictions.
        if (isRestrictionOrChildOfRestriction(currentParentReferenceValue)) {
          newParentReferenceValue = currentParentReferenceValue;
        } else if (isRestrictionOrChildOfRestriction(xm.space)) {
          newParentReferenceValue = xm.space;
          // Remember to reset when switching back to a provider with no restrictions.
          shouldResetParentReference = true;
        } else {
          // The new value is "randomly" chosen as the first value in the list.
          newParentReferenceValue = allowedSpaces[0];
          // Remember to reset when switching back to a provider with no restrictions.
          shouldResetParentReference = true;
        }
      } else if (shouldResetParentReference) {
        // No restrictions, reset to the default (current space) parent reference.
        newParentReferenceValue = xm.space;
        // Clear the flag.
        shouldResetParentReference = false;
      }

      // Set the new value, if different from what already exists.
      if (currentParentReferenceValue != newParentReferenceValue) {
        parentReferenceField.val(newParentReferenceValue);
        // Trigger the input event so that validation can be performed.
        parentReferenceField.trigger("input");
      }
    };

    // Update the allowed spaces based on the selected template provider.
    form.find('.xwiki-select').on('xwiki:select:updated', function (event) {
      var type = $('input[name="type"]:checked');
      updateTerminalCheckboxFromTemplateProviderInput(type);
      updateParentReferenceFromTemplateProviderInput(type);
    });
  });
});
