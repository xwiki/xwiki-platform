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
  CKEDITOR.plugins.add('xwiki-upload', {
    requires: 'uploadfile,uploadimage,xwiki-marker,xwiki-resource',

    afterInit: function(editor) {
      this.overrideUploadWidget(editor, 'uploadfile', true, function(html, upload) {
        // There is no link/anchor widget so we need to set the attributes instead of wrapping the link with the
        // rendering markers, as we do for the uploaded images.
        return html.replace(/<a\b/i, "<a " + serializeAttributes({
          'data-linktype': 'attachment',
          'data-freestanding': true,
          'data-wikigeneratedlinkcontent': upload.fileName,
          'data-reference': upload.responseData.resourceReference
        }));
      });

      this.overrideUploadWidget(editor, 'uploadimage', false, function(html, upload) {
        // The image widget looks for these markers.
        var startMarker = '<!--startimage:' + CKEDITOR.tools.escapeComment(
          upload.responseData.resourceReference) + '-->';
        var stopMarker = '<!--stopimage-->';
        // We need to overwrite the HTML because the upload image widget sets the image width and height and we don't
        // want to save them unless the user resizes the image explicitly.
        // See CKEDITOR-223: Image larger than the screen inserted with drag & drop gets distorted (explicit image size
        // is added by default)
        var image = '<img src="' + upload.url + '"/>';
        return startMarker + image + stopMarker;
      });

      // Chrome uses 'image.png' as file name when pasting images, instead of leaving the file name empty, and this
      // prevents us from using the configured default file name.
      // See CKEDITOR-169: Image upload by copy & paste overwrites previous ones
      // See https://github.com/ckeditor/ckeditor-dev/issues/664
      var oldCreate = editor.uploadRepository.create;
      editor.uploadRepository.create = function(file, name) {
        // jshint camelcase:false
        var defaultFileName = editor.config.fileTools_defaultFileName;
        if (!name && file && file.type === 'image/png' && file.name === 'image.png' && defaultFileName) {
          name = defaultFileName + '.png';
        }
        return oldCreate.call(this, file, name);
      };
    },

    /**
     * Overrides the specified upload widget definition to include the XWiki rendering markers and attributes required
     * in order to generate the proper wiki syntax.
     */
    overrideUploadWidget: function(editor, uploadWidgetId, typedResourceReference, filter) {
      var uploadWidget = editor.widgets.registered[uploadWidgetId];
      if (!uploadWidget) {
        return;
      }

      filter = filter || function(html, upload) {
        return html;
      };

      var originalOnUploaded = uploadWidget.onUploaded;
      uploadWidget.onUploaded = function(upload) {
        this._upload = upload;
        originalOnUploaded.call(this, upload);
      };

      var originalReplaceWith = uploadWidget.replaceWith;
      uploadWidget.replaceWith = function(data, mode) {
        var upload = this._upload;
        delete this._upload;
        if (upload) {
          upload.responseData.resourceReference.typed = typedResourceReference;
          upload.responseData.resourceReference = CKEDITOR.plugins.xwikiResource
            .serializeResourceReference(upload.responseData.resourceReference);
          data = filter(data, upload);
        }
        originalReplaceWith.call(this, data, mode);
      };
    }
  });

  var serializeAttributes = function(attributes) {
    var pairs = [];
    for (var name in attributes) {
      if (attributes.hasOwnProperty(name)) {
        var value = attributes[name];
        pairs.push(name + "=\"" + CKEDITOR.tools.htmlEncodeAttr(value) + "\"");
      }
    }
    return pairs.join(' ');
  };
})();
