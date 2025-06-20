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
    
    // Counter of the image selector instances. Each new instantiation increments this counter.
    var counter = 0;
    
    function scopedImageSelector(documentReference) {

      var index = counter;
      counter = counter + 1;
      
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

      function getCurrentTabId() {
        return modal.find(".image-selector .tab-pane.active").attr('id');
      }

      // Internal map of the tab ids and their corresponding image references.
      var mapTabReference = {};

      function setImageReferenceValue(value) {
        if (value) {
          modal.data('imageReference', {
            value: value
          });
          $('.image-selector-modal button.btn-primary').prop('disabled', false);
        } else {
          modal.data('imageReference', {});
          $('.image-selector-modal button.btn-primary').prop('disabled', true);
        }
      }

      /**
       * Can be called by the image selector tab UIXs. Indicates the list of selected images. Passing an empty array
       * indicates that not images are currently selected. The images can either be a string that will be parsed to a
       * resource reference, or a reference object (e.g., "{type: 'icon', reference: 'accept'}");
       * Note: Currently, only the first image of the list is taken into account.
       *
       * @param imageReferences the selected image references
       */
      function updateSelectedImageReferences(imageReferences) {
        imageReferences = imageReferences || [];
        var imageReferenceValue;
        if (imageReferences.length > 0) {
          // TODO:  Support the selection of several images (see CKEDITOR-445).
          var imageReference = imageReferences[0];
          var value;
          if (typeof imageReference === 'string') {
            value = resource.convertEntityReferenceToResourceReference(getEntityReference(imageReference));
          } else {
            value = imageReference;
          }
          // Always make the image reference untyped to stay compatible with syntaxes that does not support them
          // (e.g., xwiki/2.1). Except for icons that must be typed.
          value.typed = imageReference.type === 'icon';
          imageReferenceValue = value;
        }

        // Save the value in a map to be able to retrieve it later in case of tab change.
        mapTabReference[getCurrentTabId()] = imageReferenceValue;

        setImageReferenceValue(imageReferenceValue);
      }

      function initialize(modal) {
        if (!modal.data('initialized')) {
          var url = new XWiki.Document(XWiki.Model.resolve('CKEditor.ImageSelectorService', XWiki.EntityType.DOCUMENT))
            .getURL('get');
          $.get(url, $.param({
            language: $('html').attr('lang'),
            index: index,
            documentReference: documentReference
          })).done(function (html, textState, jqXHR) {
            var imageSelector = modal.find('.image-selector');
            var requiredSkinExtensions = jqXHR.getResponseHeader('X-XWIKI-HTML-HEAD');
            // It's important to insert the html content before loading the corresponding scripts. Otherwise, it's 
            // possible for scripts to be loaded too fast and to be unable to access the expected html.
            imageSelector.html(html);
            $(document).loadRequiredSkinExtensions(requiredSkinExtensions);
            $(document).trigger('xwiki:dom:updated', {'elements': imageSelector.toArray()});
            imageSelector.removeClass('loading');

            // Update the selection with the value of the current tab on tab change.
            // This needs to be done before the tab is actually switched as otherwise there is a risk that the saved
            // value overrides a value updated in the short laps of time between the show.bs.tab and shown.bs.tab
            // events. This is unlikely to happen in practice but happens quite often with automated tests (e.g.,
            // selenium).
            imageSelector.on('show.bs.tab', function (e) {
              // Retrieve the id of the to be shown tab.
              const nextTabId = $(e.target).attr("aria-controls");
              setImageReferenceValue(mapTabReference[nextTabId]);
            });

            modal.data('initialized', true);
          }).fail(function (error) {
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
        onLoad: function () {
          modal = this;
          var selectButton = modal.find('.modal-footer .btn-primary');
          // Make the modal larger.
          modal.find('.modal-dialog').addClass('modal-lg');

          modal.on('shown.bs.modal', function () {
            if (modal.data('initialized') == true) {
              // If the modal was already use, we make sure it's properly reset
              updateSelectedImageReferences(null);
            }
            initialize(modal);
          });
          selectButton.on('click', function () {
            var imageData = modal.data('input').imageData || {};
            imageData.resourceReference = modal.data('imageReference').value;
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
        loader.on('uploaded', function (evt) {
          var resourceReference = evt.sender.responseData.message.resourceReference;
          var entityReference = resource.convertResourceReferenceToEntityReference(resourceReference);
          if (options.onSuccess) {
            options.onSuccess(entityReference);
          }
        });

        loader.on('error', function (error) {
          console.log('Failed to upload a file', error);
          if (options.onError) {
            options.onError();
          }
        });
        loader.on('abort', function (error) {
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
    }

    function getDocumentReference(params) {
      return XWiki.Model.serialize(params.editor.config.sourceDocument.documentReference);
    }

    var mapScopes = {};

    function createModal(params) {
      var documentReference = getDocumentReference(params);
      if (!mapScopes[documentReference]) {
        mapScopes[documentReference] = scopedImageSelector(documentReference);
      }
      return mapScopes[documentReference].open(params);
    }

    function updateSelectedImageReferences(imageReferences, element) {
      // If this method is called when the corresponding tab is not active (e.g., a slow asynchronous query leads
      // to a call to this method while the tab is not active anymore), then the update is skipped.
      if (element.parents(".tab-pane").hasClass('active')) {
        var documentReference = getDocumentReference(element.parents('.image-selector-modal').data('input'));
        mapScopes[documentReference].updateSelectedImageReferences(imageReferences);
      }
    }

    function createLoader(uploadField, options, element) {
      var documentReference = getDocumentReference(element.parents('.image-selector-modal').data('input'));
      mapScopes[documentReference].createLoader(uploadField, options);
    }

    return {
      open: createModal,
      updateSelectedImageReferences: updateSelectedImageReferences,
      createLoader: createLoader,
      getDocumentReference: function (element) {
        return getDocumentReference(element.parents('.image-selector-modal').data('input'));
      }
    };
  });
