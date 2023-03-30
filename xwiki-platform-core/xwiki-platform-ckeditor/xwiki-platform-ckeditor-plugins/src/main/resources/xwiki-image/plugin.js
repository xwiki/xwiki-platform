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

  function showImageWizard(editor, widget, isInsert) {

    /**
     * Only called to unwrap the centered images inserted by the old image dialog.
     */
    function unwrapFromCentering(element) {
      var imageOrLink = element.findOne('a,img');

      imageOrLink.replace(element);

      return imageOrLink;
    }


    require(['imageWizard'], function(imageWizard) {
      imageWizard({
        editor: editor,
        imageData: widget.data,
        isInsert: isInsert
      }).done(function(data) {
        if (widget && widget.element) {
          widget.setData(data);

          // With the old image dialog, image were wrapped in a p to be centered. We need to unwrap them to make them
          // compliant with the new image dialog.
          if (widget.element.getName() === 'p') {
            widget.element = unwrapFromCentering(widget.element);
            widget.element.setAttribute('data-widget', widget.name);
          }

        } else {
          // Wrap the image in a span to have somewhere to place the resize element.
          var element = CKEDITOR.dom.element.createFromHtml('<span>' + widget.template.output() + '</span>',
            editor.document);
          var wrapper = editor.widgets.wrapElement(element, widget.name);
          var temp = new CKEDITOR.dom.documentFragment(wrapper.getDocument());

          // Append wrapper to a temporary document. This will unify the environment in which #data listeners work when
          // creating and editing widget.
          temp.append(wrapper);

          // Initialize an empty image widget, then update it with the data from the image dialog.
          var widgetInstance = editor.widgets.initOn(element, widget, {});
          widgetInstance.setData(data);
          editor.widgets.finalizeCreation(temp);
        }
      });
    });
  }

  CKEDITOR.plugins.add('xwiki-image', {
    requires: 'xwiki-image-old,xwiki-dialog',
    beforeInit: function(editor) {
      editor.on('widgetDefinition', function(event) {
        var widgetDefinition = event.data;
        if (widgetDefinition.name === "image" && widgetDefinition.dialog === "image2") {
          this.overrideImageWidget(editor, widgetDefinition);
        }
      }, this);
    },
    init: function(editor) {
      this.initImageDialogWidget(editor);
    },
    initImageDialogWidget: function(editor) {
      var imageWidget = editor.widgets.registered.image;

      imageWidget.insert = function() {
        showImageWizard(editor, this, true);
      };
      imageWidget.edit = function(event) {
        // Prevent the default behavior because we want to use our custom image dialog.
        event.cancel();
        showImageWizard(editor, this, false);
      };
    },
    overrideImageWidget: function(editor, imageWidget) {
      CKEDITOR.plugins.registered['xwiki-image-old'].overrideImageWidget(editor, imageWidget);

      /**
       * Update the dom of the widget to place the resize span inside the previously created wrapping span.
       *
       * @param widget the image widget to update
       */
      function moveResizer(widget) {
        if(widget.data.hasCaption) {
          return;
        } 
        var resizeWrapper = editor.document.createElement('span');
        resizeWrapper.addClass('cke_image_resizer_wrapper');
        resizeWrapper.append(widget.parts.image);
        resizeWrapper.append(widget.resizer);

        // Set the data.align to right when it's right so that the mousedown event is tricked into believing the
        // alignment is right.
        // The 'align' value is reset to 'none' on mouseout to prevent it to be persisted.
        widget.resizer.on('mouseover', function () {
          if(widget.data.alignment === 'end') {
            widget.data.align = 'right';
          }
        });
        widget.resizer.on('mouseout', function () {
          widget.data.align = 'none';
        });
        widget.on('data', function () {
          widget.resizer[widget.data.alignment === 'end' ? 'addClass' : 'removeClass']('cke_image_resizer_left');
        });
        
        if(!widget.wrapper.getChild(0).hasClass('cke_widget_element')) {
          // Re-wrap the element in a widget element.
          // This happens when removing the caption of an image. 
          var widgetElement = editor.document.createElement('span');
          widgetElement.addClass('cke_widget_element');
          widgetElement.append(resizeWrapper);
          widget.wrapper.append(widgetElement, true);
          widget.element = widgetElement;
          widget.element.setAttribute('data-widget', widget.name);
        } else {
          widget.element.append(resizeWrapper, true);
        }
      }

      var originalInit = imageWidget.init;
      imageWidget.init = function() {
        originalInit.call(this);

        // Caption
        if (this.parts.caption) {
          this.setData('hasCaption', true);
          // TODO: Add support for editing the caption directly from the dialog (see CKEDITOR-435)
        } else {
          this.setData('hasCaption', false);
        }

        // Style
        var image = this.parts.image;
        if (this.data.hasCaption) {
          image = this.element;
        }
        this.setData('imageStyle', image.getAttribute('data-xwiki-image-style') || '');

        this.setData('border', image.getAttribute('data-xwiki-image-style-border'));
        this.setData('alignment', image.getAttribute('data-xwiki-image-style-alignment'));
        this.setData('textWrap', image.getAttribute('data-xwiki-image-style-text-wrap'));

        moveResizer(this);
      };

      var originalData = imageWidget.data;

      /**
       * Update the attributes of the widget according to the data.
       *
       * @param widget the widget to update
       * @param setAttribute a set attribute method, it is a function with 3 arguments, the widget, the id of the data
       *   to update, and its value
       * @param removeAttribute a remove attribute method, it is a function with 2 arguments, the widget and the id of
       *   the data to remove
       */
      function computeStyleData(widget, setAttribute, removeAttribute) {
        // Style
        if (widget.data.imageStyle) {
          setAttribute(widget, 'data-xwiki-image-style', widget.data.imageStyle);
        } else {
          removeAttribute(widget, 'data-xwiki-image-style');
        }

        if (widget.data.border) {
          setAttribute(widget, 'data-xwiki-image-style-border', widget.data.border);
        } else {
          removeAttribute(widget, 'data-xwiki-image-style-border');
        }

        // If alignment is undefined, try to convert from the legacy align data property.
        var mapping = {left: 'start', right: 'end', center: 'center'};
        widget.data.alignment = widget.data.alignment || mapping[widget.data.align] || 'none';

        // The old align needs to be undefined otherwise it's not removed when re-inserting the image after the edition,
        // add deprecated attributes to the image.
        widget.data.align = 'none';

        if (widget.data.alignment && widget.data.alignment !== 'none') {
          setAttribute(widget, 'data-xwiki-image-style-alignment', widget.data.alignment);
        } else {
          removeAttribute(widget, 'data-xwiki-image-style-alignment');
        }

        if (widget.data.textWrap) {
          setAttribute(widget, 'data-xwiki-image-style-text-wrap', widget.data.textWrap);
        } else {
          removeAttribute(widget, 'data-xwiki-image-style-text-wrap');
        }
      }

      imageWidget.data = function() {
        /**
         * Update the given attribute at two locations in the widget, the image tag and the widget itself.
         *
         * @param widget the widget to update
         * @param key the attribute key
         * @param value the attribute value
         */
        function setAttribute(widget, key, value) {
          widget.parts.image.removeAttribute(key);
          widget.element.removeAttribute(key);
          if(widget.data.hasCaption) {
            widget.element.setAttribute(key, value);
          }
          widget.parts.image.setAttribute(key, value);
          widget.wrapper.setAttribute(key, value);
        }

        /**
         * Remove the given attribute at two locations on the widget, the image tag and the widget itself.
         *
         * @param widget the widget to update
         * @param key the property key to removew
         */
        function removeAttribute(widget, key) {
          widget.element.removeAttribute(key);
          widget.parts.image.removeAttribute(key);
          widget.wrapper.removeAttribute(key);
        }

        function updateFigureDimensions(widget) {
          if (widget.data.hasCaption) {
            // TODO: set a style with a width instead
            setAttribute(widget, 'style', "width: " + widget.data.width + "px");
          } else {
            removeAttribute(widget, 'style');
          }
        }

        // Caption
        // TODO: Add support for editing the caption directly from the dialog (see CKEDITOR-435)

        computeStyleData(this, setAttribute, removeAttribute);
        updateFigureDimensions(this, setAttribute, removeAttribute);

        originalData.call(this);
      };

      var originalUpcast = imageWidget.upcast;
      // @param {CKEDITOR.htmlParser.element} element
      // @param {Object} data
      imageWidget.upcast = function (element, data) {
        var el = originalUpcast.apply(this, arguments);
        if (el && element.name === 'img') {
          // Wrap the image with a span. This span will be used to place the resize span during the init of the image
          // widget.
          var span = new CKEDITOR.htmlParser.element( 'span' );
          el.wrapWith(span);
          el = span;
        }
        return el;
      };

      var originalDowncast = imageWidget.downcast;
      imageWidget.downcast = function (element) {
        var el = originalDowncast.apply(this, arguments);
        if (this.parts.caption === null && el.children[0] && el.children[0].children[0]) {
          el = el.children[0].children[0];
          delete el.attributes['data-widget'];
        }
        return el;
      };
    }
  });

})();
