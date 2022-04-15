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

    function getSelected(instance) {
      /* jshint camelcase: false */
      return instance.get_selected(false)[0];
    }

    function validateSelection(instance) {
      var selected = getSelected(instance);
      return selected && selected.startsWith("attachment:");
    }

    // TODO: should be loaded modularily and provided by the UIX for the image selector tab.
    function initDocumentTree(modal) {
      $('.attachments-tree').xtree()
        .one('ready.jstree', function(event, data) {
          data.instance.openTo("document:" + XWiki.Model.serialize(XWiki.currentDocument.getDocumentReference()));
        })
        .on('changed.jstree', function(event, data) {
          if (validateSelection(data.instance)) {
            saveSelectedImageReference(getSelected(data.instance), modal);
          } else {
            removeSelectedImageReference(modal);
          }
        });
    }

    function initDocumentUpload(editor, modal) {
      $("#upload form input.button").on('click', function(event) {
        event.preventDefault();
        var loader = editor.uploadRepository.create($("#fileUploadField").prop('files')[0]);
        var imageSelector = $('.image-selector');
        loader.on('uploaded', function(evt) {
          var resourceReference = evt.sender.responseData.message.resourceReference;
          var entityReference = resource.convertResourceReferenceToEntityReference(resourceReference);
          var serialized = XWiki.Model.serialize(entityReference);
          saveSelectedImageReference(serialized, modal);
          imageSelector.removeClass('loading');
          new XWiki.widgets.Notification(translations.get('modal.fileUpload.success'), 'done');
        });

        // Return non-false value will disable fileButton in dialogui,
        // below listeners takes care of such situation and re-enable "send" button.
        loader.on('error', function(error) {
          console.log('Failed to upload a file', error);
          new XWiki.widgets.Notification(translations.get('modal.fileUpload.fail'), 'error');
          imageSelector.removeClass('loading');
        });
        loader.on('abort', function(error) {
          console.log('Failed to upload a file', error);
          new XWiki.widgets.Notification(translations.get('modal.fileUpload.abort'), 'error');
          imageSelector.removeClass('loading');
        });

        imageSelector.addClass('loading');
        loader.loadAndUpload(editor.config.filebrowserUploadUrl);
      });
    }

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

    function saveSelectedImageReference(imageReference, modal) {
      modal.data('imageReference', {
        value: resource.convertEntityReferenceToResourceReference(getEntityReference(imageReference))
      });
      $('.image-selector-modal button.btn-primary').prop('disabled', false);
    }

    function removeSelectedImageReference(modal) {
      modal.data('imageReference', {});
      $('.image-selector-modal button.btn-primary').prop('disabled', true);
    }

    function initialize(modal) {
      var params = modal.data('input');

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
            initDocumentTree(modal);
            initDocumentUpload(params.editor, modal);
            modal.data('initialized', true);
          }).fail(function(error) {
          console.log('Failed to retrieve the image selection form.', error);
          new XWiki.widgets.Notification(translations.get('modal.initialization.fail'), 'error');
          modal.data('initialized', true);
        });
      }
    }

    // Initialize the modal.
    return $modal.createModalStep({
      'class': 'image-selector-modal',
      title: translations.get('modal.title'),
      acceptLabel: translations.get('modal.selectButton'),
      content: '<div class="image-selector loading"></div>',
      onLoad: function() {
        var modal = this;
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
  });
