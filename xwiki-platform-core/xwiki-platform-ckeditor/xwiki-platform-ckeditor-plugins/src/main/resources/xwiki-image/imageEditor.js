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
  'modal.updateButton',
  'modal.initialization.fail',
  'modal.outscaleWarning'
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

  function getImageStyle(styleId) {
    return loadImageStyles().then(function (results) {
      var filteredResults = results.imageStyles.filter(function (imageStyle) {
        return imageStyle.type === styleId;
      });
      if (filteredResults.length === 0) {
        return Promise.reject("Unable to find an image style with id [" + styleId + "]");
      } else {
        return Promise.resolve(filteredResults[0]);
      }
    });
  }

  return {
    loadImageStyles: loadImageStyles,
    loadImageStylesDefault: loadImageStylesDefault,
    getImageStyle: getImageStyle
  };
});

define('imageEditor', ['jquery', 'modal', 'imageStyleClient', 'l10n!imageEditor'],
  function($, $modal, imageStyleClient, translations) {
    'use strict';
    
    // Used to store the image styles once loaded by initImageStyleField.
    var imageStylesCache;
  // Used to store the default style configuration once loaded by loadImageStylesDefault.
    var defaultStyleCache;

    function initImageStyleField(modal) {
      return new Promise(function(resolve, reject) {
        var imageStylesField = $('#imageStyles');
        // Check for the presence of the images styles field. This field is not present when the wiki does not 
        // support the image styles.
        if (imageStylesField.length > 0) {
          imageStyleClient.loadImageStylesDefault()
            .then(function(defaultStyle) {
              defaultStyleCache = defaultStyle;
              var settings = {
                preload: true,
                persist: true,
                onChange: function(value) {
                  updateAdvancedFromStyle(value, modal);
                },
                load: function(typedText, callback) {
                  imageStyleClient.loadImageStyles().then(function(values) {
                    var imageStyles = values.imageStyles.map(function(value) {
                      var type = value.type;
                      // We don't persist the type (i.e., the empty string) when the default style is forced.
                      // To do so, we replace the type of the default style with the empty string.
                      if (defaultStyle.forceDefaultStyle === "true" && value.identifier === defaultStyle.defaultStyle) {
                        type = '';
                      }
                      return {
                        label: value.prettyName,
                        value: type
                      };
                    });
                    // The '---' style is only introduced when the default style is not forced, meaning that users
                    // are free to configure the image without constraints.
                    if (defaultStyle.forceDefaultStyle !== "true") {
                      imageStyles.unshift({label: '---', value: ''});
                    }
                    // Save the image styles in cache so that it can be used by other parts of the core. For instance,
                    // to update the currently selected style if it is unknown.
                    imageStylesCache = imageStyles;
                    callback(imageStyles);
                    
                    // Search for the type of the default image style by its identifier.
                    var filteredValues = values.imageStyles.filter(function (style) {
                      return style.identifier === defaultStyle.defaultStyle;
                    });
                    var defaultType = "";
                    if (defaultStyle.forceDefaultStyle !== "true" && filteredValues.length > 0) {
                      defaultType = filteredValues[0].type;
                    }
                    // Sets the default value once the values are loaded.
                    imageStylesField.data('selectize').addItem(defaultType || '');
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
        var modalInput = modal.data('input');
        // New image is set to true only of it is already explicitly set as such.
        var newImage = modalInput.newImage === true;
        modal.data('output', {
          action: 'selectImage',
          editor: modalInput.editor,
          imageData: imageData,
          newImage: newImage
        }).modal('hide');
      });
    }

    /**
     * Asynchronously load the currently selected image. If this method is called several times for the same image, a
     * single http request is made.
     *
     * @param modal the current modal
     * @returns {*} a promise containing the image object
     */
    function loadImage(modal) {
      var img = new Image();

      var promise = $.Deferred();

      img.onload = function () {
        promise.resolve(this);
      };

      img.onerror = function () {
        promise.reject(this);
      };
      
      var data = modal.data('input');
      // Resolve the image url and assign it to a transient image object to be able to access its width and height.
      if(!data.imageData.resourceReference) {
        // In case of pasted image, the resource reference is undefined. We initialize it the first time the image
        // is edited after being pasted.
        data.imageData.resourceReference = {
          'type': 'url',
          'reference': data.imageData.src
        };
      }
      img.src = getImageResourceURL(data.imageData.resourceReference, data.editor);
      return promise;
    }

    /**
     * Check the dimensions of the current image and raise and error if the dimensions are larger than the dimensions 
     * of the original image.
     * 
     * @param modal the current modal
     */
    function checkDimensions(modal) {
      loadImage(modal).then(function (image) {
        var widthField = modal.find('[name="imageWidth"]');
        var heightField = modal.find('[name="imageHeight"]');
        var errorField = modal.find('.outscaleWarning');
        var width = image.width;
        var height = image.height;
        var newWidth = widthField.val();
        var newHeight = heightField.val();

        var widthOutscaled = newWidth > width;
        var heightOutscaled = newHeight > height;

        var hasWarningClass = 'has-warning';
        if (widthOutscaled) {
          widthField.parent('label').addClass(hasWarningClass);
        } else {
          widthField.parent('label').removeClass(hasWarningClass);
        }
        if (heightOutscaled) {
          heightField.parent('label').addClass(hasWarningClass);
        } else {
          heightField.parent('label').removeClass(hasWarningClass);
        }

        if (widthOutscaled || heightOutscaled) {
          errorField.parent('.has-warning').removeClass('hidden');
          errorField.text(translations.get('modal.outscaleWarning', width + 'x' + height + 'px'));
        } else {
          errorField.parent('.has-warning').addClass('hidden');
        }
      });
    }

    function updateLockStatus(modal) {
      // When loading an image for the first time in an edit session, it is locked by default.
      if (modal.data('input').imageData.isLocked === undefined || modal.data('input').newImage) {
        modal.data('input').imageData.isLocked = true;
      }
      updateLockAccordingToStatus(modal);
    }

    function updateLockAccordingToStatus(modal) {
      var imageSizeLocked = modal.find('.image-size-locked');
      var imageSizeUnlocked = modal.find('.image-size-unlocked');
      var hiddenClass = 'hidden';
      var isLocked = modal.data('input').imageData.isLocked;
      if (isLocked) {
        imageSizeLocked.removeClass(hiddenClass);
        imageSizeUnlocked.addClass(hiddenClass);
      } else {
        imageSizeLocked.addClass(hiddenClass);
        imageSizeUnlocked.removeClass(hiddenClass);
      }
      return isLocked;
    }

    function addToggleImageWidthLock(modal) {
      var imageWidthField = modal.find('[name="imageWidth"]');
      var imageHeightField = modal.find('[name="imageHeight"]');

      modal.find('.image-size-lock').on('click', function () {
        // Toggle the lock status and refresh the display.
        modal.data('input').imageData.isLocked = !modal.data('input').imageData.isLocked;
        updateLockAccordingToStatus(modal);
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
        return loadImage(modal).then(function (image) {
          var width = image.width;
          var height = image.height;

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

          return result;

        }, function () {
          // Don't compute a dimension when the url cannot be resolved to an image.
          return undefined;
        });
      }

      /**
       * Update the size fields using the image ratio. Disabled the image size fields until the computation is done
       * (since the image must be fetched, which is can take an undetermined time due to image weight and network
       * speed).
       * @param field the name of the change field, can be either 'width' or 'height'
       * @param inputField the field to read the value from
       * @param targetField the field to update with the computed dimension (height for the width field, and width for
       *  the height field)
       * @param a promise, resolved when the size is updated
       */
      function updateSize(field, inputField, targetField) {
        var inputValue = inputField.val();
        if (modal.data('input').imageData.isLocked) {
          imageWidthField.prop('disabled', true);
          imageHeightField.prop('disabled', true);
          return $.when(updateRatio(field, inputValue)).then(function (value) {
            if (value !== undefined) {
              targetField.val(value);
            }
            imageWidthField.prop('disabled', false);
            imageHeightField.prop('disabled', false);
            // In Edge, when the fields are disabled, the focus is lost and needs to be restored after the size is 
            // updated.
            inputField.focus();
          });
        } else {
          return $.Deferred().resolve();
        }
      }

      imageWidthField.on('input', function () {
        updateSize('width', $(this), imageHeightField).then(function () {
          checkDimensions(modal);
        });
      });

      imageHeightField.on('input', function () {
        updateSize('height', $(this), imageWidthField).then(function () {
          checkDimensions(modal);
        });
      });
    }

    /**
     * Keeps alignment and text-wrap consistent on the given modal.
     *
     * @param modal the modal to keep consistent
     */
    function addToggleAlignmentTextWrap(modal) {
      var alignmentRadios = modal.find('[name="alignment"]');
      var textWrapCheckbox = modal.find('[name="textWrap"]');
      var noneAlignment = modal.find('[name="alignment"][value="none"]');

      alignmentRadios.change(function () {
        if (this.value !== 'none') {
          textWrapCheckbox.prop("checked", false);
        }
      });

      textWrapCheckbox.change(function () {
        if (this.checked) {
          noneAlignment.prop("checked", true);
        }
      });
    }

    function initializeCaption(modal) {
      var params = modal.data('input');
      var editor = params.editor;
      var feature = editor.widgets.registered.image.features.caption;
      var captionsAllowed = editor.filter.checkFeature(feature);
      var captionDd = $('#imageCaptionActivation').parents('dd');
      var captionDt = captionDd.prev();
      if(captionsAllowed) {
        captionDd.removeClass('hidden');
        captionDt.removeClass('hidden');
      } else {
        captionDd.addClass('hidden');
        captionDt.addClass('hidden');
        modal.data('input').imageData.hasCaption = false;
      }
    } 

    // Fetch modal content from a remote template the first time the image dialog editor is opened.
    function initialize(modal) {
      var params = modal.data('input');

      // Update the button label according to the image state (new or updated image).
      if (params.newImage) {
        modal.find('.modal-footer .btn-primary').text(translations.get('modal.insertButton'));
      } else {
        modal.find('.modal-footer .btn-primary').text(translations.get('modal.updateButton'));
      }
      
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
            addToggleAlignmentTextWrap(modal);
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
        }),
        // Pass the data back to the widget so that it is re-loaded in the same state during the editing session.
        isLocked: modal.data('input').imageData.isLocked
      };
    }

    function getImageResourceURL(resourceReference, editor, params) {
      var resourceURL = CKEDITOR.plugins.xwikiResource.getResourceURL(resourceReference, editor);
      // Only set the params for resource reference that can support it. Sending the custom params to external urls does
      // not make sense as they are not able to interpret them.
      if (resourceReference.type !== 'url') {
        resourceURL = resourceURL + '&' + $.param(params || {});
      }
      return resourceURL;
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

        var searchedImageStyle = imageStyle;
        // Use the constraints of the default style when the style value is the empty string and forceDefaultStyle
        // is set to true, as the default style is not persisted (i.e., the empty string).
        var forceDefaultStyle = defaultStyleCache.forceDefaultStyle === "true";
        if(forceDefaultStyle && imageStyle === '') {
          // Convert the identifier to a type in case of default image style.
          searchedImageStyle = (imageStylesConfig.imageStyles || []).find(function(imageStyleConfig) {
            return imageStyleConfig.type !== '' && imageStyleConfig.identifier === defaultStyleCache.defaultStyle;
          }).type;
        }
        
        // Search the image style config by its type.
        var config = (imageStylesConfig.imageStyles || []).find(function(imageStyleConfig) {
          return imageStyleConfig.type !== '' && imageStyleConfig.type === searchedImageStyle;
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

    function updateImageStyleFormField(imageData) {
      if (imageData.imageStyle || imageData.imageStyle === '') {
        var style = imageData.imageStyle;
        // Fallback to the default value if the currently defined style is unknown.
        if (imageStylesCache !== undefined) {
          if (!imageStylesCache.some(function (imageStyle) {
            return imageStyle.value === imageData.imageStyle;
          })) {
            style = '';
          }
        }
        $('#imageStyles')[0].selectize.setValue(style);
      }
    }

    // Update the form according to the modal input data.
    // 
    function updateForm(modal) {
      var imageData = modal.data('input').imageData || {};

      // Placed early as the value of imageData.hasCaption can be forced to false if the current syntax does not
      // support captions.
      initializeCaption(modal);

      // Switch back to the default tab
      $('.image-editor a[href="#standard"]').tab('show');

      // Style
      updateImageStyleFormField(imageData);

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
      // Uncheck text wrap in case of inconsistency (i.e., alignment takes precedence over text-wrap). 
      if (imageData.alignment && imageData.alignment !== 'none') {
        imageData.textWrap = false;
      }
      $('#advanced [name="textWrap"]').prop('checked', imageData.textWrap);

      //  Override with the style values only if it's a new image.
      updateAdvancedFromStyle($('#imageStyles')[0].selectize.getValue(), modal);
      // Initial check of the image dimensions on load.
      checkDimensions(modal);
      updateLockStatus(modal);
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
