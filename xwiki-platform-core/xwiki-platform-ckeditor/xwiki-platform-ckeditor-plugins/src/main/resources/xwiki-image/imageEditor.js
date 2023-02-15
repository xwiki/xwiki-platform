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

define('imageEditorTranslationKeys', [], [
  'modal.changeImage.button',
  'modal.loadFail.message',
  'modal.title',
  'modal.insertButton',
  'modal.initialization.fail'
]);

define('imageStyleClient', ['jquery'], function($) {
  'use strict';
  var cachedResultDefault;
  var cachedResult;
  // Load the styles once and use a cached version if the styles are requested again.
  function loadImageStylesDefault() {
    if (cachedResultDefault === undefined) {

      return Promise.resolve($.getJSON(XWiki.contextPath + $("#defaultImageStylesRestURL").val(),
        $.param({'documentReference': XWiki.Model.serialize(XWiki.currentDocument.documentReference)})))
        .then(function(defaultStyle) {
          cachedResultDefault = defaultStyle;
          return defaultStyle;
        });
    } else {
      return Promise.resolve(cachedResultDefault);
    }
  }

  function loadImageStyles() {
    if (cachedResult === undefined) {
      return Promise.resolve($.getJSON(XWiki.contextPath + $("#imageStylesRestURL").val(),
        $.param({'documentReference': XWiki.Model.serialize(XWiki.currentDocument.documentReference)}))
        .then(function(defaultStyles) {
          cachedResult = defaultStyles;
          return defaultStyles;
        }));
    } else {
      return Promise.resolve(cachedResult);
    }
  }

  return {
    loadImageStyles: loadImageStyles,
    loadImageStylesDefault: loadImageStylesDefault
  };
});

define('imageEditor', ['jquery', 'modal', 'imageStyleClient', 'l10n!imageEditor'],
  function($, $modal, imageStyleClient, translations) {
    'use strict';

    function initImageStyleField(modal) {
      return new Promise(function(resolve, reject) {
        var imageStylesField = $('#imageStyles');
        // Check for the presence of the images styles field. This field is not present when the wiki does not 
        // support the image styles.
        if (imageStylesField.length > 0) {
          imageStyleClient.loadImageStylesDefault()
            .then(function(defaultStyle) {
              var settings = {
                preload: true,
                persist: true,
                onChange: function(value) {
                  updateAdvancedFromStyle(value, modal);
                },
                load: function(typedText, callback) {
                  imageStyleClient.loadImageStyles().then(function(values) {
                    var imageStyles = values.imageStyles.map(function(value) {
                      return {
                        label: value.prettyName,
                        value: value.type
                      };
                    });
                    imageStyles.unshift({label: '---', value: ''});
                    callback(imageStyles);
                    // Sets the default value once the values are loaded.
                    imageStylesField.data('selectize').addItem(defaultStyle.defaultStyle || '');
                  }, function(err) {
                    reject(err);
                  });
                },
                onLoad: function() {
                  // Wait for the selectize field to be fully loaded before continuing.
                  resolve();
                }
              };

              imageStylesField.xwikiSelectize(settings);
            }, function(err) {
              reject(err);
            });
        } else {
          resolve();
        }
      });
    }

    function addChangeImageButton(insertButton, modal) {
      var selectImageButton = $('<button type="button" class="btn btn-default pull-left"></button>')
        .text(translations.get('modal.changeImage.button'))
        .prependTo(insertButton.parent());
      selectImageButton.on('click', function() {
        var imageData = getFormData(modal);
        modal.data('output', {
          action: 'selectImage',
          editor: modal.data('input').editor,
          imageData: imageData
        }).modal('hide');
      });
    }

    function addToggleImageWidthLock(modal) {
      var imageSizeLocked = modal.find('.image-size-locked');
      var imageSizeUnlocked = modal.find('.image-size-unlocked');
      var imageWidthField = modal.find('[name="imageWidth"]');
      var imageHeightField = modal.find('[name="imageHeight"]');
      var hiddenClass = 'hidden';
      var locked = !imageSizeLocked.hasClass(hiddenClass);
      modal.find('.image-size-lock').on('click', function () {
        imageSizeLocked.toggleClass(hiddenClass);
        imageSizeUnlocked.toggleClass(hiddenClass);
        locked = !locked;
      });

      /**
       * Compute the new size of the image size fields. The size is computed by computing the image size ratio, and
       * applying it on the inputvalue. If the input value has a suffix (e.g., 20px), the number part is extracted and
       * and the ratio is applied to it, and the suffix is concatenated to the result. For instance, for the value 30px
       * and a ratio of 0.5, the result is 15px.
       * @param field the field type ('width' or 'height')
       * @param inputvalue the input value of the updated field (e.g., 100, or 33px)
       * @returns {*|jQuery} a Defered, resolved with the computed value
       */
      function updateRatio(field, inputvalue) {
        var promise = $.Deferred();
        var img = new Image();
        img.onload = function () {
          var width = this.width;
          var height = this.height;

          var extract = /^(\d+)(.*)/.exec(inputvalue);

          var resvalue;
          var suffix;
          if (extract === null) {
            resvalue = '';
            suffix = '';
          } else {

            var value = parseInt(extract[1], 10);
            suffix = extract[2];

            if (field === 'width') {
              resvalue = Math.round(height * (value / width));
            } else {
              // Height
              resvalue = Math.round(width * (value / height));
            }
          }

          var result;
          if (resvalue === 0) {
            result = '';
          } else {
            result = resvalue + suffix;
          }

          promise.resolve(result);

        };
        var data = modal.data('input');
        // Resolve the image url and assign it to a transient image object to be able to access its width and height.
        img.src = getImageResourceURL(data.imageData.resourceReference, data.editor);
        return promise;
      }

      /**
       * Update the size fields using the image ratio. Disabled the image size fields until the computation is done
       * (since the image must be fetched, which is can take an undetermined time due to image weight and network
       * speed).
       * @param field the name of the change field, can be either 'width' or 'height'
       * @param inputField the field to read the value from
       * @param targetField the field to update with the computed dimension (height for the width field, and width for
       *  the height field)
       */
      function updateSize(field, inputField, targetField) {
        var inputValue = inputField.val();
        if (locked) {
          imageWidthField.prop('disabled', true);
          imageHeightField.prop('disabled', true);
          $.when(updateRatio(field, inputValue)).then(function (value) {
            targetField.val(value);
            imageWidthField.prop('disabled', false);
            imageHeightField.prop('disabled', false);
            // In Edge, when the fields are disabled, the focus is lost and needs to be restored after the size is 
            // updated.
            inputField.focus();
          });
        }
      }

      imageWidthField.on('input', function () {
        updateSize('width', $(this), imageHeightField);
      });

      imageHeightField.on('input', function () {
        updateSize('height', $(this), imageWidthField);
      });
    }

    // Fetch modal content from a remote template the first time the image dialog editor is opened.
    function initialize(modal) {
      var params = modal.data('input');
      if (!modal.data('initialized')) {
        var url = new XWiki.Document(XWiki.Model.resolve('CKEditor.ImageEditorService', XWiki.EntityType.DOCUMENT))
          .getURL('get');
        $.get(url, $.param({
          language: $('html').attr('lang'),
          isHTML5: params.editor.config.htmlSyntax === 'annotatedhtml/5.0'
        }))
          .done(function(html, textState, jqXHR) {
            var imageEditor = $('.image-editor');
            var requiredSkinExtensions = jqXHR.getResponseHeader('X-XWIKI-HTML-HEAD');
            $(document).loadRequiredSkinExtensions(requiredSkinExtensions);
            imageEditor.html(html);
            // TODO: Add support for editing the caption directly from the dialog (see CKEDITOR-435)
            initImageStyleField(modal).then(function() {
              imageEditor.removeClass('loading');
              $('.image-editor-modal button.btn-primary').prop('disabled', false);
              updateForm(modal);
              modal.data('initialized', true);
            });

            addToggleImageWidthLock(modal);
          }).fail(function(error) {
            new XWiki.widgets.Notification(translations.get('modal.initialization.fail'), 'error');
            console.log('Failed to retrieve the image edition form.', error);
        });
      } else {
        updateForm(modal);
      }
    }

    function getFormData(modal) {
      var resourceReference = modal.data('input').imageData.resourceReference;
      var width = $("#imageWidth").val();
      var height = $("#imageHeight").val();
      return {
        resourceReference: resourceReference,
        imageStyle: $('#imageStyles').val(),
        alignment: $('#advanced [name="alignment"]:checked').val(),
        border: $('#advanced [name="imageBorder"]').prop('checked'),
        textWrap: $('#advanced [name="textWrap"]').prop('checked'),
        alt: $('#altText').val(),
        hasCaption: !!$("#imageCaptionActivation").prop('checked'),
        // TODO: Add support for editing the caption directly from the dialog (see CKEDITOR-435)
        width: width,
        height: height,
        src: getImageResourceURL(resourceReference, modal.data('input').editor, {
          "parameters[queryString]": $.param({
            width: width,
            height: height
          })
        })
      };
    }

    function getImageResourceURL(resourceReference, editor, params) {
      return CKEDITOR.plugins.xwikiResource.getResourceURL(resourceReference, editor) + '&' + $.param(params || {});
    }

    function updateAdvancedFromStyle(imageStyle, modal) {
      function updateImageSize(config, noStyle, overrideValues) {
        var adjustableSize = config.adjustableSize !== false;
        var imageWidth = $('#imageWidth');
        var imageHeight = $('#imageHeight');
        imageWidth.prop("disabled", !adjustableSize && !noStyle);
        imageHeight.prop("disabled", !adjustableSize && !noStyle);
        if (overrideValues) {
          imageWidth.val(config.defaultWidth);
          imageHeight.val(config.defaultHeight);
        }
      }

      function updateBorder(config, noStyle, overrideValues) {
        var imageBorder = $('#imageBorder');
        imageBorder.prop('disabled', !config.adjustableBorder && !noStyle);
        if (overrideValues) {
          imageBorder.prop('checked', config.defaultBorder);
        }
      }

      function updateAlignment(config, noStyle, overrideValues) {
        var alignment = $('#advanced [name="alignment"]');
        alignment.prop('disabled', !config.adjustableAlignment && !noStyle);
        if (overrideValues) {
          alignment.val([config.defaultAlignment || 'none']);
        }
      }

      function updateTextWrap(config, noStyle, overrideValues) {
        var textWrap = $('#advanced [name="textWrap"]');
        textWrap.prop('disabled', !config.adjustableTextWrap && !noStyle);
        if (overrideValues) {
          textWrap.prop('checked', config.defaultTextWrap);
        }
      }

      imageStyleClient.loadImageStyles().then(function(imageStylesConfig) {
        // Do not update the form if the current style is the same are the one of the current user.
        var overrideValues = modal.data('input').imageData === undefined ||
          modal.data('input').imageData.imageStyle !== imageStyle;

        modal.data('input').imageData = modal.data('input').imageData || {};
        modal.data('input').imageData.imageStyle = imageStyle;

        var config = (imageStylesConfig.imageStyles || []).find(function(imageStyleConfig) {
          return imageStyleConfig.type === imageStyle;
        });
        var noStyle = false;
        if (config === undefined) {
          config = {};
          noStyle = true;
          overrideValues = false;
        }

        // Image size
        updateImageSize(config, noStyle, overrideValues);

        // Border
        updateBorder(config, noStyle, overrideValues);

        // Alignment
        updateAlignment(config, noStyle, overrideValues);

        // Text Wrap
        updateTextWrap(config, noStyle, overrideValues);
      });
    }

    // Update the form according to the modal input data.
    // 
    function updateForm(modal) {
      var imageData = modal.data('input').imageData || {};

      // Switch back to the default tab
      $('.image-editor a[href="#standard"]').tab('show');

      // Style
      if (imageData.imageStyle || imageData.imageStyle === '') {
        $('#imageStyles')[0].selectize.setValue(imageData.imageStyle);
      }

      // Alt
      $('#altText').val(imageData.alt);


      // Caption
      $('#imageCaptionActivation').prop('checked', imageData.hasCaption);
      // TODO: Add support for editing the caption directly from the dialog (see CKEDITOR-435)

      // Image size
      $('#imageWidth').val(imageData.width);
      $('#imageHeight').val(imageData.height);

      // Border
      $('#imageBorder').prop('checked', imageData.border);

      // Alignment
      $('#advanced [name="alignment"]').val([imageData.alignment || 'none']);

      // Text Wrap
      $('#advanced [name="textWrap"]').prop('checked', imageData.textWrap);

      //  Override with the style values only if it's a new image.
      updateAdvancedFromStyle($('#imageStyles')[0].selectize.getValue(), modal);
    }

    return $modal.createModalStep({
      'class': 'image-editor-modal',
      title: translations.get('modal.title'),
      acceptLabel: translations.get('modal.insertButton'),
      content: '<div class="image-editor loading"></div>',
      onLoad: function() {
        var modal = this;
        var insertButton = modal.find('.modal-footer .btn-primary');
        // Make the modal larger.
        modal.find('.modal-dialog').addClass('modal-lg');

        modal.on('shown.bs.modal', function() {
          initialize(modal);
        });
        insertButton.on('click', function() {
          var output = getFormData(modal);
          modal.data('output', output).modal('hide');
        });

        addChangeImageButton(insertButton, modal);
      }
    });
  });
