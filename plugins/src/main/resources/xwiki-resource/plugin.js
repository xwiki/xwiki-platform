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
(function (){
  'use strict';
  var $ = jQuery;

  var knownResourceTypes = {
    attach: {
      label: 'Attachment',
      icon: 'glyphicon glyphicon-paperclip',
      placeholder: 'Path.To.Page@attachment.png'
    },
    data: {
      label: 'Data URI',
      icon: 'glyphicon glyphicon-briefcase',
      placeholder: 'image/png;base64,AAAAAElFTkSuQmCC'
    },
    doc: {
      label: 'Page',
      icon: 'glyphicon glyphicon-file',
      placeholder: 'Path.To.Page',
      allowEmptyReference: true
    },
    icon: {
      label: 'Icon',
      icon: 'glyphicon glyphicon-flag',
      placeholder: 'help'
    },
    mailto: {
      label: 'Mail',
      icon: 'glyphicon glyphicon-envelope',
      placeholder: 'user@example.org'
    },
    path: {
      label: 'Path',
      icon: 'glyphicon glyphicon-road',
      placeholder: '/path/to/file'
    },
    unc: {
      label: 'UNC Path',
      icon: 'glyphicon glyphicon-hdd',
      placeholder: '//server/share/path/to/file'
    },
    unknown: {
      label: 'Unknown',
      icon: 'glyphicon glyphicon-question-sign',
      placeholder: ''
    },
    url: {
      label: 'URL',
      icon: 'glyphicon glyphicon-globe',
      placeholder: 'http://www.example.org/image.png'
    },
    user: {
      label: 'User',
      icon: 'glyphicon glyphicon-user',
      placeholder: 'alias'
    }
  };

  // Empty plugin required for dependency management.
  CKEDITOR.plugins.add('xwiki-resource', {
    requires: 'xwiki-marker,xwiki-dialog'
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
          components[2] += '?' + queryString;
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
                '<label class="cke_dialog_ui_labeled_label">Location</label>' +
                '<div class="resourcePicker input-group">' +
                  '<input type="text" class="resourceReference" />' +
                  '<div class="input-group-btn">'+
                    '<button type="button" class="resourceType btn btn-default">' +
                      '<span class="icon">' +
                    '</button>' +
                    '<button type="button" class="btn btn-default dropdown-toggle" data-toggle="dropdown">' +
                      '<span class="caret"></span>' +
                    '</button>' +
                    '<ul class="dropdown-menu dropdown-menu-right"></ul>' +
                  '</div>' +
                '</div>' +
              '</div>',
        onLoad: function() {
          // Fix the tab-key navigation.
          var resourceTypeDropDownToggle = this.getElement().findOne('.dropdown-toggle');
          [resourceTypeDropDownToggle, this.getTypeInput(), this.getReferenceInput()].forEach(function(field) {
            var dialog = this;
            dialog.addFocusable(field, 0);
            field._focusable = dialog._.focusList[0];
            field.on('focus', function() {
              dialog._.currentFocusIndex = this._focusable.focusIndex;
            });
          }, this.getDialog());
          // Fix reference input id.
          var id = CKEDITOR.tools.getNextId();
          this.getReferenceInput().setAttribute('id', id);
          this.getElement().findOne('label').setAttribute('for', id);
          // Populate the Resource Type drop down.
          this.resourceTypes.forEach(this.addResourceType, this);
          // Add the JavaScript behaviour.
          var domElement = $(this.getElement().$);
          addResourcePickerBehaviour(domElement);
          // Listen to resource type changes (in order to be able to show different options for different resource types).
          domElement.on('changeResourceType', $.proxy(this, 'onResourceTypeChange'));
        },
        validate: function() {
          var reference = this.getReferenceInput().getValue();
          if (reference === '') {
            // Check if the selected resource type supports empty references.
            var resourceType = knownResourceTypes[this.getTypeInput().getValue()];
            if (resourceType.allowEmptyReference !== true) {
              return 'The location is not specified.';
            }
          }
          return true;
        },
        getValue: function() {
          return {
            type: this.getTypeInput().getValue(),
            reference: this.getReferenceInput().getValue()
          };
        },
        setValue: function(resourceReference) {
          // Reset the input if no resource reference is provided.
          resourceReference = resourceReference || {type: this.resourceTypes[0]};
          this.selectResourceType(resourceReference.type);
          this.getReferenceInput().setValue(resourceReference.reference || '');
        },
        //
        // Custom fields
        //
        dropDownItemTemplate: new CKEDITOR.template('<li><a href="#" data-id="{id}" ' +
          'data-placeholder="{placeholder}"><span class="icon {icon}"></span> {label}</a></li>'),
        getTypeInput: function() {
          return this.getElement().findOne('.resourceType');
        },
        getReferenceInput: function() {
          return this.getElement().findOne('.resourceReference');
        },
        selectResourceType: function(resourceTypeId) {
          if (resourceTypeId === this.getTypeInput().getValue()) {
            // The specified resource type is already selected.
            return;
          }
          var resourceTypeAnchor = $(this.getElement().$).find('a').filter(function() {
            return $(this).attr('data-id') === resourceTypeId;
          });
          if (resourceTypeAnchor.length === 0) {
            // Unsupported resource type. We need to add it to the list before selecting it.
            this.addResourceType(resourceTypeId);
            resourceTypeAnchor = $(this.getElement().$).find('a').filter(function() {
              return $(this).attr('data-id') === resourceTypeId;
            });
          }
          resourceTypeAnchor.click();
        },
        addResourceType: function(resourceTypeId) {
          var resourceType = knownResourceTypes[resourceTypeId] || {
            label: resourceTypeId,
            icon: 'glyphicon glyphicon-question-sign'
          };
          resourceType.id = resourceTypeId;
          var dropDownMenu = this.getElement().findOne('.dropdown-menu');
          dropDownMenu.appendHtml(this.dropDownItemTemplate.output(resourceType));
        },
        onResourceTypeChange: function(event, data) {
          // Do nothing by default.
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

  var addResourcePickerBehaviour = function(picker) {
    var resourceReferenceInput = picker.find('input.resourceReference');
    var resourceTypeButton = picker.find('button.resourceType');
    var resourceTypeIcon = resourceTypeButton.find('.icon');
    picker.find('.dropdown-menu').on('click', 'a', function(event) {
      event.preventDefault();
      var selectedResourceType = $(event.target);
      resourceReferenceInput.attr('placeholder', selectedResourceType.attr('data-placeholder'));
      var oldValue = resourceTypeButton.val();
      var newValue = selectedResourceType.attr('data-id');
      resourceTypeButton.val(newValue);
      resourceTypeButton.attr('title', selectedResourceType.text().trim());
      var disabled = selectedResourceType.attr('href') === '#';
      resourceTypeButton.prop('disabled', disabled);
      if (disabled) {
        // It seems that setting the property is not enough. CKEditor checks for a non-empty value.
        resourceTypeButton.attr('disabled', 'disabled');
      }
      resourceTypeIcon.attr('class', selectedResourceType.find('.icon').attr('class'));
      picker.triggerHandler('changeResourceType', {oldValue: oldValue, newValue: newValue});
    });
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
})();
