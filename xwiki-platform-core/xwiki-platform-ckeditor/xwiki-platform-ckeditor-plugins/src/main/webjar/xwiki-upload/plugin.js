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
    requires: 'uploadfile,uploadimage,notification,xwiki-marker,xwiki-resource',

    init: function(editor) {
      preventParallelUploads(editor);
      listenToUploadedAttachments(editor);
      if (editor.config['xwiki-upload']?.uploadImagesFromPastedHTML !== false) {
        handleImagesFromPastedHTML(editor);
      }
    },

    afterInit: function(editor) {
      this.overrideUploadWidget(editor, 'uploadfile', true, function(html, upload) {
        // There is no link/anchor widget so we need to set the attributes instead of wrapping the link with the
        // rendering markers, as we do for the uploaded images.
        return html.replace(/<a\b/i, "<a " + serializeAttributes({
          'data-linktype': 'attachment',
          'data-freestanding': true,
          'data-wikigeneratedlinkcontent': upload.fileName,
          'data-reference': upload.responseData.serializedResourceReference
        }));
      });

      this.overrideUploadWidget(editor, 'uploadimage', false, function(html, upload) {
        // Check if the uploaded image is already wrapped by a image widget.
        const widget = editor.widgets.getByElement(this.wrapper.getParent());
        if (widget?.name === 'image' && widget.parts.image.equals(this.parts.img)) {
          // Update the image widget data.
          widget.setData({
            resourceReference: upload.responseData.resourceReference,
            src: upload.url
          });

          // We need to add the image back after the upload image widget is removed. We use a placeholder to know where
          // to insert the image back.
          const placeholder = editor.document.createElement('span');
          placeholder.insertBefore(this.wrapper);
          setTimeout(() => {
            // The upload image widget has been detached from the DOM but the image element still has some leftover
            // attributes specific to the upload image widget. We perform the cleanup by destroying the upload image
            // widget. We set the wrapper to the placeholder knowing that the destroy method is replacing the wrapper
            // with the widget element (the image) at the end. Note that the image source was updated when we set the
            // image widget data above.
            this.wrapper = placeholder;
            // Don't keep any widget specific attributes when destroying the widget.
            widget.parts.image.data('cke-widget-keep-attr', 0);
            editor.widgets.destroy(this);
          }, 0);

          // Simply remove the upload image widget.
          return "";
        } else {
          // Insert the image widget markers so that the uploaded image is upcasted to an image widget.
          const startMarker = '<!--startimage:' + CKEDITOR.tools.escapeComment(
            upload.responseData.serializedResourceReference) + '-->';
          const stopMarker = '<!--stopimage-->';
          // We need to overwrite the HTML because the upload image widget sets the image width and height and we don't
          // want to save them unless the user resizes the image explicitly.
          // See CKEDITOR-223: Image larger than the screen inserted with drag & drop gets distorted (explicit image
          // size is added by default)
          const image = '<img src="' + upload.url + '"/>';
          return startMarker + image + stopMarker;
        }
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
          upload.responseData.serializedResourceReference = CKEDITOR.plugins.xwikiResource
            .serializeResourceReference(upload.responseData.resourceReference);
          data = filter.call(this, data, upload);
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
        if (editor.config.formId) {
          input.attr('form', editor.config.formId);
        }
        input.insertAfter($(editor.element.$));
      }
    });
  };

  function handleImagesFromPastedHTML(editor) {
    // Maps the URL of a pasted image to the corresponding File (Blob) object. Avoids downloading the same image
    // multiple times.
    const imageFileStore = {};

    /**
     * In order to upload an image we need access to the File object. But we can't get the File object directly from the
     * image HTML element (even if that image is already displayed to the user). We need to fetch the image using its
     * source URL. But this is asynchronous (even if the image is already in the browser cache) so we can't do it from
     * the paste event listener. Instead, we allow the image to be inserted in the content with its "external" URL but
     * we mark it to be uploaded later on, after the paste event listeners are called.
     *
     * @param {CKEDITOR.editor} editor the CKEditor instance for which to mark the pasted images
     */
    function markImagesToUploadFromPastedHTML(editor) {
      // Make sure the attribute we use to mark pasted images is not saved.
      editor.dataProcessor?.htmlFilter?.addRules({
        elements: {
          img: function(image) {
            delete image.attributes['data-xwiki-pasted-image'];
          }
        }
      }, {priority: 14, applyToAll: true});

      editor.on('paste', function(event) {
        const data = event.data;
        // Check if the pasted content was copied from outside the editor, is HTML and contains images.
        if (data.dataTransfer.getTransferType() === CKEDITOR.DATA_TRANSFER_EXTERNAL && data.type === 'html' &&
            data.dataValue.includes('<img')) {
          // The clipboard can contain multiple data types. For instance, when using the "Copy Image" option from the
          // browser's context menu, the clipboard is filled with both the image HTML markup and the image File object.
          // CKEditor prefers the HTML markup but we can still access the File object.
          const pastedImageFiles = getPastedImageFiles(data.dataTransfer);
          const container = document.createElement('div');
          container.innerHTML = data.dataValue;
          let imageCount = 0;
          container.querySelectorAll('img[src]').forEach(image => {
            // Ignore the images that don't have the source URL specified (we can't do anything about them) or that are
            // using the data URI scheme (they are handled separately).
            if (image.src && !image.src.startsWith('data:')) {
              // Mark the pasted image in order to upload it afterwards.
              image.dataset.xwikiPastedImage = true;
              if (!imageFileStore[image.src] && pastedImageFiles.length) {
                // Unfortunatelly, there's no explicit mapping between the HTML image elements an the image file objects
                // (from the clipboard). We assume that:
                // * the HTML image elements and the image files are in the same order
                // * the image files are not duplicated if the same image URL is used multiple times in the HTML
                imageFileStore[image.src] = pastedImageFiles.shift();
              }
              imageCount++;
            }
          });
          if (imageCount > 0) {
            data.dataValue = container.innerHTML;
          }
        }
      });
    }

    function getPastedImageFiles(dataTransfer) {
      const imageFiles = [];
      for (let i = 0; i < dataTransfer.getFilesCount(); i++) {
        const file = dataTransfer.getFile(i);
        if (file.type.startsWith('image/')) {
          imageFiles.push(file);
        }
      }
      return imageFiles;
    }

    function loadAndUploadImagesFromPastedHTML(editor) {
      editor.on('afterPaste', function(event) {
        // By waiting before uploading the images, we allow the user to cancel the upload, in case they want to keep the
        // images with the "external" URLs. The user can also undo the upload and source replacement.
        loadPastedImages(editor).then(waitBeforeUploading).then(uploadPastedImages).catch((e) => {
          console.error('Failed to upload images from pasted HTML: ', e);
        });
      });
    }

    async function loadPastedImages(editor) {
      let notification;
      // See https://ckeditor.com/docs/ckeditor4/latest/api/CKEDITOR_dom_nodeList.html
      const pastedImages = editor.editable()?.find('img[data-xwiki-pasted-image]');
      const pastedImagesCount = pastedImages.count();
      const loadedImages = [];
      if (pastedImagesCount) {
        notification = editor.showNotification(
          editor.localization.get('xwiki-upload.downloadingPastedImages', 0, pastedImagesCount),
          'progress'
        );
        // Make sure we don't handle the same image multiple times, because the user can paste again while the
        // previously pasted images are being downloaded.
        pastedImages.toArray().forEach(pastedImage => pastedImage.removeAttribute('data-xwiki-pasted-image'));
        for (let i = 0; i < pastedImagesCount; i++) {
          // The user can abort the download by closing the notification.
          if (!notification.isVisible()) {
            throw new Error('Downloading pasted images was aborted.');
          }
          try {
            // We download the pasted images sequentially, in order to give more time to the user to decide if they want
            // to abort the process or not.
            loadedImages.push(await loadPastedImage(pastedImages.getItem(i)));
          } catch (e) {
            // The image failed to be downloaded, usually due to CORS restrictions. Move to the next image.
          } finally {
            const loadedCount = i + 1;
            notification.update({
              message: editor.localization.get('xwiki-upload.downloadingPastedImages', loadedCount, pastedImagesCount),
              progress: loadedCount / pastedImagesCount,
              // Switch the notification type to 'warning' if we were not able to download all the pasted images.
              type: loadedCount < pastedImagesCount || loadedImages.length === pastedImagesCount ?
                'progress' : 'warning'
            });
          }
        }
      }

      return {
        editor,
        pastedImages,
        loadedImages,
        notification
      };
    }

    /**
     * Fetches the image file based on its source URL, if it's not already in the image file store.
     *
     * @param {CKEDITOR.dom.element} pastedImage the image DOM element that was pasted
     * @returns the pasted image DOM element, after the image was loaded
     */
    async function loadPastedImage(pastedImage) {
      const imageURL = pastedImage.$.src;
      if (!imageFileStore[imageURL]) {
        // This is subject to CORS so it may fail if the server is not properly configured.
        const data = await fetch(imageURL);
        imageFileStore[imageURL] = await data.blob();
      }
      return pastedImage;
    }

    async function waitBeforeUploading({editor, pastedImages, loadedImages, notification}) {
      if (loadedImages.length < pastedImages.count() && notification?.isVisible()) {
        // Update the notification to inform the user that some images failed to be downloaded.
        const failedCount = pastedImages.count() - loadedImages.length;
        notification.update({
          message: editor.localization.get('xwiki-upload.imageDownloadFailed', failedCount, pastedImages.count()),
          // Reset the progress because we're reusing the notification.
          progress: undefined,
          type: 'warning'
        });
        // Show the warning message for 3 seconds, before starting to upload the images that were successfully
        // downloaded.
        await new Promise(resolve => setTimeout(resolve, 3000));
      }

      if (loadedImages.length) {
        // Wait 5 seconds before uploading, to allow the user to cancel the upload.
        await new Promise((resolve, reject) => {
          const duration = 5;
          let secondsLeft = duration;
          const interval = setInterval(() => {
            if (!notification.isVisible()) {
              clearInterval(interval);
              reject(new Error('Uploading pasted images was aborted.'));
            } else if (secondsLeft < 0) {
              clearInterval(interval);
              resolve();
            } else {
              notification.update({
                message: editor.localization.get('xwiki-upload.starting', secondsLeft),
                progress: (duration - secondsLeft) / duration
              });
              secondsLeft--;
            }
          }, 1000);
        });
      } else {
        // Nothing to upload.
        notification?.hide();
      }

      return {editor, pastedImages, loadedImages, notification};
    }

    function uploadPastedImages({editor, loadedImages, notification}) {
      if (loadedImages.length) {
        let uploadedCount = 0;
        let failedCount = 0;
        updateUploadImageProgress(notification, uploadedCount, failedCount, loadedImages.length);
        loadedImages.forEach(image => {
          // We upload the pasted images in parallel.
          uploadPastedImage(editor, image).then(() => {
            updateUploadImageProgress(notification, ++uploadedCount, failedCount, loadedImages.length);
          }, (e) => {
            failedCount++;
            console.log('Failed to upload pasted image: ', e);
          });
        });
      }
    }

    /**
     * Replaces the image widget instance (associated with the given image DOM element) with an upload image widget
     * instance that handles the image upload (showing the progress), before being itself replaced with a new image
     * widget instance that uses the attached image.
     *
     * @param {CKEDITOR.editor} editor the CKEditor instance
     * @param {CKEDITOR.dom.element} image the image DOM element to update after the image is uploaded
     */
    async function uploadPastedImage(editor, image) {
      const imageFile = imageFileStore[image.$.src];
      const uploadImageWidgetDef = editor.widgets.registered.uploadimage;
      if (!uploadImageWidgetDef.supportedTypes ||
          CKEDITOR.fileTools.isTypeSupported(imageFile, uploadImageWidgetDef.supportedTypes)) {
        // Start uploading the image.
        const fileName = getFileName(image.$.src);
        const loader = editor.uploadRepository.create(imageFile, fileName, uploadImageWidgetDef.loaderType);
        loader.upload(uploadImageWidgetDef.uploadUrl, uploadImageWidgetDef.additionalRequestParameters);

        // Create the upload image widget.
        CKEDITOR.fileTools.markElement(image, 'uploadimage', loader.id);
        editor.widgets.initOn(image);
        if (!uploadImageWidgetDef.skipNotifications) {
          CKEDITOR.fileTools.bindNotifications(editor, loader);
        }

        // Wait for the upload to finish (successfully or not).
        await new Promise((resolve, reject) => {
          loader.on('uploaded', resolve);
          loader.on('abort', reject);
          loader.on('error', reject);
        });
      }
    }

    function getFileName(fileURL) {
      fileURL = new URL(fileURL, window.location.href);
      const path = fileURL.pathname.split('/');
      return path[path.length - 1];
    }

    function updateUploadImageProgress(notification, uploadedCount, failedCount, totalCount) {
      notification.update({
        message: editor.localization.get('xwiki-upload.uploadingPastedImages', uploadedCount, totalCount),
        progress: uploadedCount / totalCount,
        // Update the notification type to 'warning' if some images failed to be uploaded.
        type: failedCount > 0 ? 'warning' : (uploadedCount < totalCount ? 'progress' : 'success')
      });
      if (uploadedCount >= totalCount) {
        // We handled all the pasted images.
        if (failedCount > 0) {
          notification.update({
            message: editor.localization.get('xwiki-upload.imageUploadFailed', failedCount, totalCount),
            // Reset the progress because we're reusing the notification.
            progress: undefined,
            type: 'warning'
          });
        }
        // Keep the notification visible for 3 seconds.
        setTimeout(() => {
          notification.hide();
        }, 3000);
      }
    }

    markImagesToUploadFromPastedHTML(editor);
    loadAndUploadImagesFromPastedHTML(editor);
  }
})();
