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
(function() {
  'use strict';

  // Declare the configuration namespace.
  CKEDITOR.config['xwiki-image'] = CKEDITOR.config['xwiki-image'] || {
    __namespace: true
  };

  CKEDITOR.plugins.add('xwiki-image-old', {
    requires: 'xwiki-marker,xwiki-resource,balloontoolbar',

    beforeInit: function(editor) {
      editor.on('widgetDefinition', function(event) {
        var widgetDefinition = event.data;
        if (widgetDefinition.name === "image" && widgetDefinition.dialog === "image2") {
          this.overrideImageWidget(editor, widgetDefinition);
        }
      }, this);
    },

    init: function(editor) {
      editor.plugins['xwiki-marker'].addMarkerHandler(editor, 'image', {
        // startImageComment: CKEDITOR.htmlParser.comment
        // content: CKEDITOR.htmlParser.node[]
        toHtml: function(startImageComment, content) {
          if (content.length === 1 && content[0].name === 'img') {
            var image = content[0];
            // Protect the image rendering markers by saving their data on the image element itself.
            var reference = startImageComment.value.substring('startimage:'.length);
            image.attributes['data-reference'] = CKEDITOR.tools.unescapeComment(reference);
            // Handle free-standing images.
            if (image.hasClass('wikimodel-freestanding')) {
              image.attributes['data-freestanding'] = true;
              // This is an internal class that should not be visible when editing the image.
              image.removeClass('wikimodel-freestanding');
              // The alt attribute is auto-generated for free-standing images. We remove it in order to be consistent
              // with the wiki syntax where it's not specified.
              delete image.attributes.alt;
            }
            // Protect the internal class 'wikigeneratedid' from being moved to the figure.
            if (image.hasClass('wikigeneratedid')) {
              image.attributes['data-wikigeneratedid'] = 'true';
              image.removeClass('wikigeneratedid');
            }
          } else {
            // Unexpected HTML structure inside image markers. Keep the markers.
            return false;
          }
        },
        // element: CKEDITOR.htmlParser.element
        isMarked: function(element) {
          return element.name === 'img' && element.attributes['data-reference'];
        },
        // image: CKEDITOR.htmlParser.element
        toDataFormat: function(image) {
          // Put back the image rendering markers.
          var reference = CKEDITOR.tools.escapeComment(image.attributes['data-reference']);
          var startImageComment = new CKEDITOR.htmlParser.comment('startimage:' + reference);
          var stopImageComment = new CKEDITOR.htmlParser.comment('stopimage');
          startImageComment.insertBefore(image);
          stopImageComment.insertAfter(image);
          delete image.attributes['data-reference'];
          // Handle free-standing images.
          var freeStanding = image.attributes['data-freestanding'] === 'true';
          delete image.attributes['data-freestanding'];
          // Free-standing images don't have white-space in their reference and have only the src attribute set.
          if (freeStanding && !/\s+/.test(reference) && isFreeStandingImage(image)) {
            image.addClass('wikimodel-freestanding');
          }
          // Put the 'wikigeneratedid' class back to the image when the attribute is set.
          var wikiGeneratedId = image.attributes['data-wikigeneratedid'] === 'true';
          delete image.attributes['data-wikigeneratedid'];
          if (wikiGeneratedId) {
            image.addClass('wikigeneratedid');
          }
        }
      });

      editor.balloonToolbars.create({
        buttons: 'Link,Unlink,Image',
        widgets: 'image'
      });
    },

    overrideImageWidget: function(editor, imageWidget) {
      var originalInit = imageWidget.init;
      imageWidget.init = function() {
        originalInit.call(this);
        var serializedResourceReference = this.parts.image.getAttribute('data-reference');
        if (serializedResourceReference) {
          this.setData('resourceReference', CKEDITOR.plugins.xwikiResource
            .parseResourceReference(serializedResourceReference));
        }
      };

      var originalData = imageWidget.data;
      imageWidget.data = function() {
        var resourceReference = this.data.resourceReference;
        if (resourceReference) {
          this.parts.image.setAttribute('data-reference', CKEDITOR.plugins.xwikiResource
            .serializeResourceReference(resourceReference));
        }
        originalData.call(this);
      };

      // Adds support for configuring the allowed content for image caption (currently hard-coded).
      var captionAllowedContent = (editor.config['xwiki-image'] || {}).captionAllowedContent;
      if (captionAllowedContent) {
        imageWidget.editables.caption.allowedContent = captionAllowedContent;
      }

      //
      // Don't remove the width/height if they are specified using percentage.
      // See CKEDITOR-122: CKEditor removes image width when specified as percentage
      //

      // @param {CKEDITOR.htmlParser.element} image
      // @param {String} dimension ('width' or 'height')
      var maybeProtectDimension = function(image, dimension) {
        var value = typeof image.attributes[dimension] === 'string' && image.attributes[dimension].trim();
        if (value.length && value.charAt(value.length - 1) === '%') {
          image[dimension + '%'] = image.attributes[dimension];
          delete image.attributes[dimension];
          return true;
        }
      };

      // @param {CKEDITOR.htmlParser.element} image
      // @param {String} dimension ('width' or 'height')
      var maybeUnprotectDimension = function(image, dimension) {
        if (image[dimension + '%']) {
          image.attributes[dimension] = image[dimension + '%'];
          delete image[dimension + '%'];
        }
      };

      var originalUpcast = imageWidget.upcast;
      // @param {CKEDITOR.htmlParser.element} element
      // @param {Object} data
      imageWidget.upcast = function(element, data) {
        var imagesUsingPercent = [];
        element.forEach(function(childElement) {
          // Note that we're using the Bitwise OR (|) operator because we want to protect both attributes.
          if (element.name === 'img' && (maybeProtectDimension(element, 'width') |
            maybeProtectDimension(element, 'height'))) {
            imagesUsingPercent.push(element);
          }
        }, CKEDITOR.NODE_ELEMENT);
        try {
          return originalUpcast.apply(this, arguments);
        } finally {
          imagesUsingPercent.forEach(function(image) {
            maybeUnprotectDimension(image, 'width');
            maybeUnprotectDimension(image, 'height');
          });
        }
      };
    }
  });

  CKEDITOR.on('dialogDefinition', function(event) {
    // Make sure we affect only the editors that load this plugin.
    if (!event.editor.plugins['xwiki-image-old']) {
      return;
    }

    // Take the dialog window name and its definition from the event data.
    var dialogName = event.data.name;
    var dialogDefinition = event.data.definition;
    if (dialogName === 'image2') {
      replaceWithResourcePicker(dialogDefinition, 'src', {
        resourceTypes: (event.editor.config['xwiki-image'] || {}).resourceTypes || ['attach', 'icon', 'url'],
        setup: function(widget) {
          this.setValue(widget.data.resourceReference);
        },
        commit: function(widget) {
          var oldResourceReference = widget.data.resourceReference || {};
          var newResourceReference = this.getValue();
          if (oldResourceReference.type !== newResourceReference.type ||
            oldResourceReference.reference !== newResourceReference.reference) {
            newResourceReference.typed = newResourceReference.type !== 'attach' &&
              (newResourceReference.type !== 'url' || newResourceReference.reference.indexOf('://') < 0);
            widget.setData('resourceReference', CKEDITOR.tools.extend(newResourceReference, oldResourceReference));
          }
        }
      });
      CKEDITOR.plugins.xwikiResource.updateResourcePickerOnFileBrowserSelect(dialogDefinition,
        ['info', 'resourceReference'], ['Upload', 'uploadButton']);

      // See CKEDITOR-122: CKEditor removes image width when specified as percentage
      allowRelativeDimensions(dialogDefinition);
    }
  });

  var replaceWithResourcePicker = function(dialogDefinition, replacedElementId, pickerDefinition) {
    var path = CKEDITOR.plugins.xwikiDialog.getUIElementPath(replacedElementId, dialogDefinition.contents);
    if (path && path.length > 2) {
      pickerDefinition = CKEDITOR.plugins.xwikiResource.createResourcePicker(pickerDefinition);
      // Bind the replaced element to the resource picker (in order to commit the resource picker value).
      var tabId = path[path.length - 1].element.id;
      CKEDITOR.plugins.xwikiResource.bindResourcePicker(path[0].element, [tabId, pickerDefinition.id], undefined,
        function (url, onlyTheReferencePart, dialog, resourcePickerId) {
          var width = dialog.getValueOf(resourcePickerId[0], "width");
          var height = dialog.getValueOf(resourcePickerId[0], "height");
          if (onlyTheReferencePart && (width || height)) {
            var queryStringParams = {};
            if (width) {
              queryStringParams.width = width;
            }
            if (height) {
              queryStringParams.height = height;
            }
            return url + '&' + $.param({"parameters[queryString]": $.param(queryStringParams)});

          } else {
            return url;
          }
        });
      // Hide the parent.
      path[1].element.hidden = true;
      // Insert the new element before the hidden parent.
      path[2].element.children.splice(path[1].position, 0, pickerDefinition);
    }
  };

  var allowRelativeDimensions = function(imageDialog) {
    var infoTab = imageDialog.getContents('info');
    allowRelativeDimension(infoTab, 'width');
    allowRelativeDimension(infoTab, 'height');
  };

  var regexPercent = /^\s*(\d+\%)\s*$/i;
  var allowRelativeDimension = function(infoTab, dimension) {
    var originalValidate = infoTab.get(dimension).validate;
    infoTab.get(dimension).validate = function() {
      return this.getValue().match(regexPercent) || originalValidate.apply(this, arguments);
    };
  };

  // Free-standing images have only the source attribute set.
  var isFreeStandingImage = function(image) {
    for (var attribute in image.attributes) {
      if (image.attributes.hasOwnProperty(attribute) && attribute !== 'src' && image.attributes[attribute] !== '') {
        return false;
      }
    }
    return true;
  };
})();
