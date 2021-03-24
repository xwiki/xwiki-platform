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
require(['jquery', 'resource', 'resourcePicker'], function ($, $resource) {
  'use strict';

  // Declare the configuration namespace.
  CKEDITOR.config['xwiki-resource'] = CKEDITOR.config['xwiki-resource'] || {
    __namespace: true
  };

  CKEDITOR.plugins.add('xwiki-resource', {
    requires: 'xwiki-marker,xwiki-dialog,xwiki-localization'
  });

  CKEDITOR.plugins.xwikiResource = {
    parseResourceReference: function(serializedResourceReference) {
      var parts = serializedResourceReference.split('|-|');
      var resourceReference = {
        typed: parts[0] === 'true',
        type: parts[1],
        reference: parts[2],
        parameters: {}
      };
      if (parts.length > 3) {
        var serializedParameters = parts.slice(3).join('|-|');
        CKEDITOR.plugins.xwikiMarker.parseParameters(serializedParameters, resourceReference.parameters);
      }
      if (resourceReference.type === 'mailto') {
        // HACK: Add support for mailto parameters (e.g. subject and body).
        var queryStringIndex = resourceReference.reference.lastIndexOf('?');
        if (queryStringIndex >= 0) {
          var queryString = resourceReference.reference.substr(queryStringIndex + 1);
          resourceReference.reference = resourceReference.reference.substr(0, queryStringIndex);
          CKEDITOR.tools.extend(resourceReference.parameters, parseQueryString(queryString), true);
        }
      }
      return resourceReference;
    },

    serializeResourceReference: function(resourceReference) {
      var components = [
        !!resourceReference.typed,
        resourceReference.type,
        resourceReference.reference
      ];
      var parameters = resourceReference.parameters;
      if (resourceReference.type === 'mailto' && resourceReference.reference.indexOf('?') < 0) {
        // HACK: Add support for mailto parameters (e.g. subject and body).
        parameters = CKEDITOR.tools.extend({}, parameters);
        var queryStringParameters = {};
        ['subject', 'body'].forEach(function(id) {
          var value = parameters[id];
          if (value !== null && value !== undefined) {
            queryStringParameters[id] = value;
          }
          delete parameters[id];
        });
        var queryString = $.param(queryStringParameters);
        if (queryString.length > 0) {
          // The replace is a workaround for a jquery issue: https://github.com/jquery/jquery/issues/2658
          components[2] += '?' + queryString.replace(/\+/g, '%20');
        }
      }
      var serializedParameters = CKEDITOR.plugins.xwikiMarker.serializeParameters(parameters);
      if (serializedParameters) {
        components.push(serializedParameters);
      }
      return components.join('|-|');
    },

    createResourcePicker: function(pickerDefinition) {
      var basePickerDefinition = {
        //
        // Standard fields
        //
        id: 'resourceReference',
        type: 'html',
        html: '<div>' +
                '<label class="cke_dialog_ui_labeled_label"></label>' +
                '<input type="text" />' +
              '</div>',
        // Focus the resource picker when the dialog is loaded.
        tabIndex: 0,
        onLoad: function() {
          // Register the event listeners and create the resource picker. We register the changeResourceType listener
          // before creating the resource picker because we need to catch the first changeResourceType event in order to
          // update the label (it won't be fired again when we set the value of the resource input unless the resource
          // type is different).
          $(this.getElement().$).on('changeResourceType', $.proxy(this, 'onResourceTypeChange'))
            .find('input').resourcePicker({resourceTypes: this.resourceTypes});
          // We register the selectResource event listener after creating the resource picker because we don't care
          // about the first selectResource event since we're going to trigger another one anyway by setting the value
          // of the resource input when the dialog is shown.
          $(this.getElement().$).on('selectResource', $.proxy(this, 'onSelectResource'));
          // Fix the tab-key navigation.
          var resourceTypeDropDownToggle = this.getElement().findOne('.dropdown-toggle');
          var resourceTypeButton = this.getElement().findOne('button.resourceType');
          var resourceReferenceInput = this.getElement().findOne('input.resourceReference');
          var tabIndex = this.tabIndex;
          [resourceTypeDropDownToggle, resourceTypeButton, resourceReferenceInput].forEach(function(field) {
            var dialog = this;
            if (tabIndex >= 0 && tabIndex < dialog._.focusList.length) {
              dialog.addFocusable(field, tabIndex);
              field._focusable = dialog._.focusList[tabIndex];
            } else {
              dialog.addFocusable(field);
              field._focusable = dialog._.focusList[dialog._.focusList.length - 1];
            }
            field.on('focus', function() {
              dialog._.currentFocusIndex = this._focusable.focusIndex;
            });
          }, this.getDialog());
          // Fix reference input id.
          var id = CKEDITOR.tools.getNextId();
          resourceReferenceInput.setAttribute('id', id);
          this.getElement().findOne('label').setAttribute('for', id);
        },
        validate: function() {
          var resourceReference = this.getValue();
          if (resourceReference.reference === '') {
            // Check if the selected resource type supports empty references.
            var resourceTypeConfig = $resource.types[resourceReference.type] || {};
            if (resourceTypeConfig.allowEmptyReference !== true) {
              return this.getDialog().getParentEditor().localization.get('xwiki-resource.notSpecified',
                this.getLabelElement().getText());
            }
          }
          return true;
        },
        getValue: function() {
          var resourcePickerInput = this.getResourcePickerInput();
          // Make sure the resource picker has updated the resource picker input. This is needed in Internet Explorer
          // where the dialog buttons are not selectable and so the change event is not fired before the click event.
          $(resourcePickerInput.$).trigger('beforeGetValue');
          var serializedResourceReference = resourcePickerInput.getValue();
          var separatorIndex = serializedResourceReference.indexOf(':');
          var resourceReference = {
            type: serializedResourceReference.substr(0, separatorIndex),
            reference: serializedResourceReference.substr(separatorIndex + 1)
          };
          if (this.selectedResource && this.selectedResource.reference.type === resourceReference.type &&
              this.selectedResource.reference.reference === resourceReference.reference) {
            // Preserve the typed field if the resource type and reference have not changed.
            resourceReference.typed = this.selectedResource.reference.typed;
          }
          return resourceReference;
        },
        setValue: function(resourceReference) {
          // Reset the resource picker if no resource reference is provided.
          resourceReference = resourceReference || {
            type: this.resourceTypes[0],
            reference: '',
            // Make sure the picker doesn't try to resolve the empty reference.
            isNew: true
          };
          var serializedResourceReference = (resourceReference.type || '') + ':' + (resourceReference.reference || '');
          $(this.getResourcePickerInput().$).val(serializedResourceReference).trigger('selectResource', {
            reference: resourceReference
          });
        },
        //
        // Custom fields
        //
        getResourcePickerInput: function() {
          return this.getElement().findOne('input');
        },
        getLabelElement: function() {
          return this.getElement().findOne('.cke_dialog_ui_labeled_label');
        },
        onResourceTypeChange: function(event, data) {
          // Update the label.
          var resourceTypeConfig = $resource.types[data.newValue] || {label: data.newValue};
          this.getLabelElement().setText(resourceTypeConfig.label);
        },
        onSelectResource: function(event, resource) {
          this.selectedResource = resource;
        }
      };
      pickerDefinition = pickerDefinition || {};
      // We may need to access the base definition in order to override some operation.
      pickerDefinition.base = basePickerDefinition;
      return CKEDITOR.tools.extend(pickerDefinition, basePickerDefinition);
    },

    getResourceURL: function(resourceReference, editor) {
      if (['url', 'path', 'unc', 'unknown'].indexOf(resourceReference.type) >= 0) {
        return resourceReference.reference;
      } else if (['mailto', 'data'].indexOf(resourceReference.type) >= 0) {
        return resourceReference.type + ':' + resourceReference.reference;
      } else {
        var dispatcherURL = editor.config['xwiki-resource'].dispatcher;
        dispatcherURL += dispatcherURL.indexOf('?') < 0 ? '?' : '&';
        return dispatcherURL + $.param(resourceReference);
      }
    },

    updateResourcePickerOnFileBrowserSelect: function(dialogDefinition, resourcePickerElementId, fileBrowserElementId) {
      var fileBrowserElement = dialogDefinition.getContents(fileBrowserElementId[0]).get(fileBrowserElementId[1]);
      if (fileBrowserElement && fileBrowserElement.filebrowser) {
        var fileBrowserConfig = fileBrowserElement.filebrowser;
        if (typeof fileBrowserConfig === 'string') {
          fileBrowserConfig = {target: fileBrowserConfig};
          fileBrowserElement.filebrowser = fileBrowserConfig;
        }
        var oldOnSelect = fileBrowserConfig.onSelect;
        fileBrowserConfig.onSelect = function(fileURL, data) {
          if (data.resourceReference) {
            // Update the resource picker.
            this.getDialog().setValueOf(resourcePickerElementId[0], resourcePickerElementId[1], data.resourceReference);
          }
          if (typeof oldOnSelect === 'function') {
            return oldOnSelect.call(this, fileURL, data);
          }
        };
      }
    },

    bindResourcePicker: function(element, resourcePickerId, onlyTheReferencePart) {
      // Use the resource picker value when the given element is committed.
      var oldCommit = element.commit;
      element.commit = function() {
        var resourceReference = this.getDialog().getValueOf(resourcePickerId[0], resourcePickerId[1]);
        if (onlyTheReferencePart  === true) {
          this.setValue(resourceReference.reference);
        } else {
          this.setValue(CKEDITOR.plugins.xwikiResource.getResourceURL(resourceReference,
            this.getDialog().getParentEditor()));
        }
        if (typeof oldCommit === 'function') {
          oldCommit.apply(this, arguments);
        }
      };
      // We validate the resource reference instead.
      delete element.validate;
      // Hide the element. We show the resource picker instead.
      element.hidden = true;
    }
  };

  var parseQueryString = function(queryString) {
    var parameters = {};
    // Replace plus signs in the query string with spaces.
    queryString = queryString.replace(/\+/g, ' ');
    queryString.split('&').forEach(function(pair) {
      var parts = pair.split('=');
      var name = decodeURIComponent(parts[0]);
      var value = parts.length > 1 ? decodeURIComponent(parts[1]) : '';
      // We know the query string supports multiple parameter values but we don't have this need right now.
      parameters[name] = value;
    });
    return parameters;
  };
});
