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
          var element = CKEDITOR.dom.element.createFromHtml(widget.template.output(), editor.document);
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
    init: function(editor) {
      this.initImageDialogWidget(editor);
    },
    initImageDialogWidget: function(editor) {
      var imageWidget = editor.widgets.registered.image;
      this.overrideImageWidget(editor, imageWidget);

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
        this.setData('imageStyle', this.parts.image.getAttribute('data-xwiki-image-style') || '');

        this.setData('border', this.parts.image.getAttribute('data-xwiki-image-style-border'));
        this.setData('alignment', this.parts.image.getAttribute('data-xwiki-image-style-alignment'));
        this.setData('textWrap', this.parts.image.getAttribute('data-xwiki-image-style-text-wrap'));
      };

      var originalData = imageWidget.data;
      imageWidget.data = function() {

        /**
         * Update the given attribute at two locations in the widget, the image tag and the widget itself.
         *
         * @param widget the widget to update
         * @param key the attribute key
         * @param value the attribute value
         */
        function setAttribute(widget, key, value) {
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
          widget.parts.image.removeAttribute(key);
          widget.wrapper.removeAttribute(key);
        }

        // Caption
        // TODO: Add support for editing the caption directly from the dialog (see CKEDITOR-435)

        // Style
        if (this.data.imageStyle) {
          setAttribute(this, 'data-xwiki-image-style', this.data.imageStyle);
        } else {
          removeAttribute(this, 'data-xwiki-image-style');
        }

        if (this.data.border) {
          setAttribute(this, 'data-xwiki-image-style-border', this.data.border);
        } else {
          removeAttribute(this, 'data-xwiki-image-style-border');
        }

        // If alignment is undefined, try to convert from the legacy align data property.
        var mapping = {left: 'start', right: 'end', center: 'center'};
        this.data.alignment = this.data.alignment || mapping[this.data.align] || 'none';

        // The old align needs to be undefined otherwise it's not removed when re-inserting the image after the edition,
        // add deprecated attributes to the image.
        this.data.align = 'none';
        
        if (this.data.alignment && this.data.alignment !== 'none') {
          setAttribute(this, 'data-xwiki-image-style-alignment', this.data.alignment);
        } else {
          removeAttribute(this, 'data-xwiki-image-style-alignment');
        }

        if (this.data.textWrap) {
          setAttribute(this, 'data-xwiki-image-style-text-wrap', this.data.textWrap);
        } else {
          removeAttribute(this, 'data-xwiki-image-style-text-wrap');
        }

        originalData.call(this);
      };
    }
  });

})();
