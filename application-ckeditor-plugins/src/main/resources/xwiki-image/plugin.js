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

  function showImageWizard(editor, widget) {
    require(['imageWizard'], function(imageWizard) {
      imageWizard({
        editor: editor,
        imageData: widget.data
      }).done(function(data) {
        if (widget && widget.element) {
          widget.setData(data);
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
      var imagePlugin = this;
      var imageWidget = editor.widgets.registered.image;
      this.overrideImageWidget(editor, imageWidget);

      imageWidget.insert = function() {
        showImageWizard(editor, this);
      };
      imageWidget.edit = function(event) {
        // Prevent the default behavior because we want to use our custom image dialog.
        event.cancel();
        showImageWizard(editor, this);
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
        this.setData('alignment', this.parts.image.getAttribute('data-xwiki-image-style-alignment') || 'none');
        this.setData('textWrap', this.parts.image.getAttribute('data-xwiki-image-style-text-wrap'));
      };

      var originalData = imageWidget.data;
      imageWidget.data = function() {

        // Caption
        // TODO: Add support for editing the caption directly from the dialog (see CKEDITOR-435)

        // Style
        if (this.data.imageStyle) {
          this.parts.image.setAttribute('data-xwiki-image-style', this.data.imageStyle);
        } else {
          this.parts.image.removeAttribute('data-xwiki-image-style');
        }

        if (this.data.border) {
          this.parts.image.setAttribute('data-xwiki-image-style-border', this.data.border);
        } else {
          this.parts.image.removeAttribute('data-xwiki-image-style-border');
        }

        if (this.data.alignment && this.data.alignment !== 'none') {
          this.parts.image.setAttribute('data-xwiki-image-style-alignment', this.data.alignment);
        } else {
          this.parts.image.removeAttribute('data-xwiki-image-style-alignment');
        }

        if (this.data.textWrap) {
          this.parts.image.setAttribute('data-xwiki-image-style-text-wrap', this.data.textWrap);
        } else {
          this.parts.image.removeAttribute('data-xwiki-image-style-text-wrap');
        }

        originalData.call(this);
      };
    }
  });

})();
