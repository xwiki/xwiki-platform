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
define('imageSelectorTranslationKeys', [], [
  'modal.title',
  'modal.loadFail.message',
  'modal.selectButton',
  'modal.fileUpload.success',
  'modal.fileUpload.fail',
  'modal.fileUpload.abort',
  'modal.initialization.fail'
]);


define('imageSelector', ['jquery', 'modal', 'resource', 'l10n!imageSelector'],
  function($, $modal, resource, translations) {
    'use strict';

    function getEntityReference(referenceStr) {
      var reference;
      if (referenceStr.startsWith("attachment:")) {
        var separatorIndex = referenceStr.indexOf(':');
        reference = referenceStr.substr(separatorIndex + 1);
      } else {
        reference = referenceStr;
      }

      return XWiki.Model.resolve(reference, XWiki.EntityType.ATTACHMENT);
    }

    /**
     * Can be called by the image selector tab UIXs. Indicates the list of  slected images.
     * Passing an empty array indicates that not images are currently selected.
     * Note: Currently, only the first image of the list is taken into account.
     *
     * @param imageReferences the selected image references
     */
    function updateSelectedImageReferences(imageReferences) {
      imageReferences = imageReferences || [];
      if(imageReferences.length > 0) {
        // TODO:  Support the selection of several images (see CKEDITOR-445).
        var imageReference = imageReferences[0];
        modal.data('imageReference', {
          value: resource.convertEntityReferenceToResourceReference(getEntityReference(imageReference))
        });
        $('.image-selector-modal button.btn-primary').prop('disabled', false);
      } else {
        modal.data('imageReference', {});
        $('.image-selector-modal button.btn-primary').prop('disabled', true);  
      }
    }

    function initialize(modal) {
      if (!modal.data('initialized')) {
        var url = new XWiki.Document(XWiki.Model.resolve('CKEditor.ImageSelectorService', XWiki.EntityType.DOCUMENT))
          .getURL('get');
        $.get(url, $.param({language: $('html').attr('lang')}))
          .done(function(html, textState, jqXHR) {
            var imageSelector = $('.image-selector');
            var requiredSkinExtensions = jqXHR.getResponseHeader('X-XWIKI-HTML-HEAD');
            $(document).loadRequiredSkinExtensions(requiredSkinExtensions);
            imageSelector.html(html);
            imageSelector.removeClass('loading');

            modal.data('initialized', true);
          }).fail(function(error) {
          console.log('Failed to retrieve the image selection form.', error);
          new XWiki.widgets.Notification(translations.get('modal.initialization.fail'), 'error');
          modal.data('initialized', true);
        });
      }
    }

    // Defined once the modal is initialized.
    var modal;

    // Initialize the modal.
    var createModal = $modal.createModalStep({
      'class': 'image-selector-modal',
      title: translations.get('modal.title'),
      acceptLabel: translations.get('modal.selectButton'),
      content: '<div class="image-selector loading"></div>',
      onLoad: function() {
        modal = this;
        var selectButton = modal.find('.modal-footer .btn-primary');
        // Make the modal larger.
        modal.find('.modal-dialog').addClass('modal-lg');

        modal.on('shown.bs.modal', function() {
          initialize(modal);
        });
        selectButton.on('click', function() {
          var imageData = modal.data('input').imageData || {};
          imageData.resourceReference = modal.data('imageReference').value;
          if (imageData.resourceReference) {
            imageData.resourceReference.typed = false;
          }
          var output = {
            imageData: imageData,
            editor: modal.data('input').editor,
            newImage: modal.data('input').newImage
          };
          modal.data('output', output).modal('hide');
        });
      }
    });


    /**
     * Initialize a loader for the provided upload field. Three optional callbacks can be provided in the options 
     * object:
     * - onSuccess is called when the upload is successful, the entity reference is passed as argument.
     * - onError is called when the upload fails
     * - onAbort is called when the upload is aborted
     * @param uploadField the upload field to create the loader for
     * @param options an option object with callback actions
     */
    function createLoader(uploadField, options) {
      options = options || {};
      var editor = modal.data('input').editor;
      var loader = editor.uploadRepository.create(uploadField);
      loader.on('uploaded', function(evt) {
        var resourceReference = evt.sender.responseData.message.resourceReference;
        var entityReference = resource.convertResourceReferenceToEntityReference(resourceReference);
        if (options.onSuccess) {
          options.onSuccess(entityReference);
        }
      });

      loader.on('error', function(error) {
        console.log('Failed to upload a file', error);
        if (options.onError) {
          options.onError();
        }
      });
      loader.on('abort', function(error) {
        console.log('Failed to upload a file', error);
        if (options.onAbort) {
          options.onAbort();
        }
      });

      loader.loadAndUpload(editor.config.filebrowserUploadUrl);
    }
    
    return {
      open: createModal,
      updateSelectedImageReferences: updateSelectedImageReferences,
      createLoader: createLoader
    };
  });
