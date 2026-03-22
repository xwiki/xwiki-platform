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
define('xwiki-wysiwyg-image-selector-translation-keys', {
  prefix: 'imageSelector.modal.',
  keys: [
    'title',
    'loadFail.message',
    'selectButton',
    'fileUpload.success',
    'fileUpload.fail',
    'fileUpload.abort',
    'initialization.fail'
  ]
});

define('xwiki-wysiwyg-image-selector', [
  'jquery',
  'xwiki-wysiwyg-modal',
  'xwiki-wysiwyg-resource',
  'xwiki-l10n!xwiki-wysiwyg-image-selector-translation-keys'
], function($, $modal, resource, translations) {
  'use strict';
  
  // Counter of the image selector instances. Each new instantiation increments this counter.
  let counter = 0;
  
  function scopedImageSelector(documentReference) {

    const index = counter;
    counter = counter + 1;
    
    function getEntityReference(referenceStr) {
      let reference;
      if (referenceStr.startsWith("attachment:")) {
        const separatorIndex = referenceStr.indexOf(':');
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
    const mapTabReference = {};

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
      let imageReferenceValue;
      if (imageReferences.length > 0) {
        // TODO:  Support the selection of several images (see CKEDITOR-445).
        const imageReference = imageReferences[0];
        let value;
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
        const url = new XWiki.Document(XWiki.Model.resolve('XWiki.WYSIWYG.ImageSelectorService',
          XWiki.EntityType.DOCUMENT)).getURL('get');
        $.get(url, $.param({
          language: $('html').attr('lang'),
          index,
          documentReference
        })).done(function (html, textState, jqXHR) {
          const imageSelector = modal.find('.image-selector');
          // It's important to insert the html content before loading the corresponding scripts. Otherwise, it's 
          // possible for scripts to be loaded too fast and to be unable to access the expected html.
          imageSelector.html(html);
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
          new XWiki.widgets.Notification(translations.get('initialization.fail'), 'error');
          modal.data('initialized', true);
        });
      }
    }

    // Defined once the modal is initialized.
    let modal;

    // Initialize the modal.
    const createModal = $modal.createModalStep({
      'class': 'image-selector-modal',
      title: translations.get('title'),
      acceptLabel: translations.get('selectButton'),
      content: '<div class="image-selector loading"></div>',
      onLoad: function () {
        modal = this;
        const selectButton = this.find('.modal-footer .btn-primary');
        // Make the modal larger.
        this.find('.modal-dialog').addClass('modal-lg');

        this.on('shown.bs.modal', () => {
          if (this.data('initialized') === true) {
            // If the modal was already use, we make sure it's properly reset
            updateSelectedImageReferences(null);
          }
          initialize(this);
        });
        selectButton.on('click', () => {
          const input = this.data('input');
          const imageData = input.imageData || {};
          imageData.resourceReference = this.data('imageReference').value;
          const output = {
            captionAllowed: input.captionAllowed,
            currentDocument: input.currentDocument,
            getImageResourceURL: input.getImageResourceURL,
            imageData,
            isHTML5: input.isHTML5,
            newImage: input.newImage,
            upload: input.upload,
          };
          this.data('output', output).modal('hide');
        });
      }
    });

    return {
      open: createModal,
      updateSelectedImageReferences,
      createLoader: (...args) => modal.data('input').upload(...args)
    };
  }

  function getDocumentReference(params) {
    return XWiki.Model.serialize(params.currentDocument || XWiki.currentDocument.documentReference);
  }

  const mapScopes = {};

  function createModal(params) {
    const documentReference = getDocumentReference(params);
    if (!mapScopes[documentReference]) {
      mapScopes[documentReference] = scopedImageSelector(documentReference);
    }
    return mapScopes[documentReference].open(params);
  }

  function updateSelectedImageReferences(imageReferences, element) {
    // If this method is called when the corresponding tab is not active (e.g., a slow asynchronous query leads
    // to a call to this method while the tab is not active anymore), then the update is skipped.
    if (element.parents(".tab-pane").hasClass('active')) {
      const documentReference = getDocumentReference(element.parents('.image-selector-modal').data('input'));
      mapScopes[documentReference].updateSelectedImageReferences(imageReferences);
    }
  }

  function createLoader(uploadedFile, options, element) {
    const documentReference = getDocumentReference(element.parents('.image-selector-modal').data('input'));
    mapScopes[documentReference].createLoader(uploadedFile, options);
  }

  return {
    open: createModal,
    updateSelectedImageReferences,
    createLoader,
    getDocumentReference: function (element) {
      return getDocumentReference(element.parents('.image-selector-modal').data('input'));
    }
  };
});
