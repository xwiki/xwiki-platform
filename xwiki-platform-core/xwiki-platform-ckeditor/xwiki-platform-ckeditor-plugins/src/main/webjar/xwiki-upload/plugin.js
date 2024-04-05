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

      // Browses uses 'image.png', or a localized form (e.g., 'graphik.png' in German Firefox) as file name when pasting
      // images, instead of leaving the file name empty, and this prevents us from using the configured default file
      // name. Keeping the same name would override again and again the same attachment, which is problematic when more
      // than one image is pasted to the same page. Instead, we replace the filename by a timestamp and a random number.
      // For instance, 'image.png' becomes '1684887709-691.png'. See https://github.com/ckeditor/ckeditor-dev/issues/664
      var oldCreate = editor.uploadRepository.create;
      
      // duringPaste is set to true on beforePaste and to false on afterPaste.
      // That way, duringPaste is true when editor.uploadRepository.create is called only when a file is pasted.
      var duringPaste = false;

      editor.on('beforePaste', function (event) {
        // Switch to 'true' only if the method used to insert the file in the editor is a pasted (in opposition to a 
        // drag-and-drop which also triggers this event, but with a 'drop' method).
        duringPaste = event.data && event.data.method === 'paste';
      });

      editor.on('afterPaste', function () {
        duringPaste = false;
      });

      editor.uploadRepository.create = function (file, name) {
        // Update the filename only when the file is pasted, otherwise use the provided filename.
        if (duringPaste) {
          const extension = getFileType(file);
          const timestamp = Date.now();
          const min = 100;
          const max = 999;
          const random = Math.floor(Math.random() * (max - min + 1)) + min;
          name = `${timestamp}-${random}.${extension}`;
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

  const base64HeaderRegExp = /^data:(\S*?);base64,/;
  const getFileType = function(fileOrData) {
    if (typeof fileOrData === 'string') {
      // The file is specified as a Data URI. Extract the content type.
      const contentType = fileOrData.match(base64HeaderRegExp)?.[1] || '';
      // TODO: Better have a mapping between content types and file extensions, but for images (which is the most common
      // use case of pasting files as data URI in HTML) this solution is acceptable.
      return contentType.split('/').slice(1).join('/');
    } else if (typeof fileOrData.name === 'string') {
      // Extract the file name extension (everything after the first '.').
      return fileOrData.name.split('.').slice(1).join('.');
    } else {
      console.debug('Unexpected file: ', fileOrData);
      return '';
    }
  };

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
          'value': JSON.parse(event.data.fileLoader.xhr.responseText).fileName
        });
        input.insertAfter($(editor.element.$));
      }
    });
  };
})();
