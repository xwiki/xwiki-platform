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

  // Declare the configuration namespace.
  CKEDITOR.config['xwiki-upload'] = CKEDITOR.config['xwiki-upload'] || {
    __namespace: true
  };

  CKEDITOR.plugins.add('xwiki-upload', {
    requires: 'uploadfile,uploadimage,xwiki-marker,xwiki-resource',

    init: function(editor) {
      preventParallelUploads(editor);
      listenToUploadedAttachments(editor);
    },

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

  // Prevent parallel uploads in order to reduce the load on the server.
  var preventParallelUploads = function(editor) {
    var uploadQueue = $.Deferred().resolve();
    editor.on('fileUploadRequest', function(event) {
      var xhr = event.data.fileLoader.xhr;
      // We need to know when the upload ends (load, error or abort) in order to perform the next upload.
      var uploadPromise = $.Deferred();
      xhr.addEventListener('loadend', uploadPromise.resolve.bind(uploadPromise));
      // Overwrite the original send function to 'wait' for the previous upload to end.
      var originalSend = xhr.send;
      xhr.send = function() {
        var data = arguments;
        // Wait for the previous upload in the queue to end before sending the new upload.
        uploadQueue = uploadQueue.then(function() {
          originalSend.apply(xhr, data);
          return uploadPromise.promise();
        });
      };
    // Make sure our listener is called as early as possible because:
    // * the loadend event listener is not called if added after the upload request was opened (initialized);
    //   see https://developer.mozilla.org/en-US/docs/Web/API/XMLHttpRequest/Using_XMLHttpRequest#monitoring_progress
    // * another event listener might send the upload request before we overwrite the send function;
    //   see https://ckeditor.com/docs/ckeditor4/latest/guide/dev_file_upload.html#request-2
    }, null, null, 1);
  };

  var  listenToUploadedAttachments = function (editor) {
    // Inject header for FileUploader to know which upload mechanism to use.
    editor.on('fileUploadRequest', function (event) {
      if (editor.config['xwiki-upload']) {
        var xhr = event.data.fileLoader.xhr;
        xhr.setRequestHeader( 'X-XWiki-Temporary-Attachment-Support',
          editor.config['xwiki-upload'].isTemporaryAttachmentSupported);
      }
    });
    // Inject a new input field when an attachment is added so that the save request knows
    // which are the new attachments.
    editor.on('fileUploadResponse', function (event) {
      if (editor.config['xwiki-upload'].isTemporaryAttachmentSupported) {
        var input = $('<input>').attr({
          'type': 'hidden',
          'name': 'uploadedFiles',
          'value': event.data.fileLoader.fileName
        });
        input.insertAfter($(editor.element.$));
      }
    });
  };
})();
