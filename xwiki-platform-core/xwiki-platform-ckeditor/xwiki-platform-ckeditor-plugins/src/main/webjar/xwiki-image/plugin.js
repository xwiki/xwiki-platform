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
(function() {
  'use strict';

  function disableResizer(widget) {
    require(['imageStyleClient'], function (imageStyleClient) {
      widget.resizer.removeClass('hidden');
      var styleId = widget.data.imageStyle;
      if (styleId) {
        imageStyleClient.getImageStyle(styleId)
          .then(function (imageStyle) {
            if (imageStyle.adjustableSize === false) {
              widget.resizer.addClass('hidden');
            }
          }, function () {
            console.debug("Failed to resolve image style [" + styleId + "]");
          });
      } else {
        // In case of empty imageStyle, check if this is because the default style is forced.
        // In this case, allow resizing according to the default style configuration.
        imageStyleClient.loadImageStylesDefault().then((defaultStyle) => {
          if (defaultStyle.forceDefaultStyle === "true") {
            imageStyleClient.loadImageStyles().then((values) => {
              var forcedStyle = values.imageStyles.filter((style) => style.identifier === defaultStyle.defaultStyle)[0];
              if (forcedStyle.adjustableSize === false) {
                widget.resizer.addClass('hidden');
              }
            });
          }
        });
      }
    });
  }

  // The setImageData argument can be used to skip the Image Wizard.
  function showImageWizard(editor, widget, isInsert, setImageData) {

    /**
     * Only called to unwrap the centered images inserted by the old image dialog.
     */
    function unwrapFromCentering(element) {
      var imageOrLink = element.findOne('a,img');

      imageOrLink.replace(element);

      return imageOrLink;
    }


    require(['imageWizard'], function(imageWizard) {
      imageWizard({
        editor: editor,
        imageData: Object.assign({}, widget.data),
        isInsert: isInsert,
        setImageData: setImageData
      }).done(function(data) {
        if (widget && widget.element) {
          widget.setData(data);

          disableResizer(widget);

          // With the old image dialog, image were wrapped in a p to be centered. We need to unwrap them to make them
          // compliant with the new image dialog.
          if (widget.element.getName() === 'p') {
            widget.element = unwrapFromCentering(widget.element);
            widget.element.setAttribute('data-widget', widget.name);
          }

        } else {
          // Wrap the image in a span to have somewhere to place the resize element.
          var element = CKEDITOR.dom.element.createFromHtml('<span>' + widget.template.output() + '</span>',
            editor.document);
          var wrapper = editor.widgets.wrapElement(element, widget.name);
          var temp = new CKEDITOR.dom.documentFragment(wrapper.getDocument());

          // Append wrapper to a temporary document. This will unify the environment in which #data listeners work when
          // creating and editing widget.
          temp.append(wrapper);

          // Initialize an empty image widget, then update it with the data from the image dialog.
          var widgetInstance = editor.widgets.initOn(element, widget, {});
          widgetInstance.setData(data);
          editor.widgets.finalizeCreation(temp);
        }
      });
    });
  }

  CKEDITOR.plugins.add('xwiki-image', {
    requires: 'xwiki-image-old,xwiki-dialog',
    beforeInit: function(editor) {
      editor.on('widgetDefinition', function(event) {
        var widgetDefinition = event.data;
        if (widgetDefinition.name === "image" && widgetDefinition.dialog === "image2") {
          this.overrideImageWidget(editor, widgetDefinition);
        }
      }, this);
    },
    init: function(editor) {

      // Register the the img:: autocomplete using the ckeditor mentions plugin.
      editor.config.mentions = editor.config.mentions || [];

      editor.config.mentions.push({
        marker: 'img::',
        pattern: /img::\S{0,30}$/,
        minChars: 0,
        itemsLimit: 5,
        itemTemplate: '<li data-id="{id}">' +

          // Allow different display for non image entries.
          '<div class="ckeditor-autocomplete-item-head {extraClass}">' +

            '<span class="ckeditor-autocomplete-item-{type}-wrapper">' +
              '<span class="{iconClass}" aria-hidden="true"></span>' +
              '<img src="{imgSrc}" aria-hidden="true"/>' +
            '</span>' +

            '<div class="ckeditor-autocomplete-item">' +
              '<span class="ckeditor-autocomplete-item-label">{label}</span>' +
              '<div class="ckeditor-autocomplete-item-badges">' +
                '<span class="badge ckeditor-autocomplete-item-badge">{badge}</span>' +
              '</div>' +
              '<div class="ckeditor-autocomplete-item-hint">{space}</div>' +
            '</div>' +
          '</div>' +
          '</li>',
        feed: function (opts, callback) {
          require(['attachmentService'], function (attachmentService) {


            const attachmentToItem = function (attachment) {
              var badge = "";
              if (attachment.isLocal) {
                badge = editor.localization.get('xwiki-image.slash.currentPageBadge');
              }
              if (XWiki.currentWiki !== attachment.wiki) {
                badge = editor.localization.get('xwiki-image.slash.externalBadge');
              }

              return {
                id: attachment.id,
                label: attachment.filename[0],
                iconClass: "",
                badge: badge,
                space: (XWiki.currentWiki === attachment.wiki ? "" : attachment.wiki + " : ") + attachment.location,
                type: "preview",
                imgSrc: CKEDITOR.plugins.xwikiResource.getResourceURL({
                  type: "attach",
                  typed: false,
                  reference: attachment.id
                }, editor),
                reference: attachment.id,
                extraClass: "",
              };
            };

            const loadingItem = {
              id: "_loading",
              extraClass: "ckeditor-autocomplete-centered",
              label: editor.localization.get('xwiki-image.slash.loading'),
              iconClass: "fa fa-spinner fa-pulse fa-fw",
              type: "icon",
              space: "",
              badge: "",
              imgSrc: "",
              query: opts.query,
            };

            const uploadItem = {
              id: "_uploadImage",
              label: editor.localization.get('xwiki-image.slash.upload'),
              iconClass: "fa fa-upload",
              extraClass: "ckeditor-autocomplete-centered",
              space: "",
              badge: "",
              imgSrc: "",
              type: "icon",
            };

            // Upload image item should be the first in the list.
            const topItems = [];
            if (uploadItem.label.toLowerCase().startsWith(opts.query.toLowerCase())) {
              topItems.push(uploadItem);
            }


            const isImage = function (attachment) {
              return attachment.mimetype.some(function (mime) {
                return mime.startsWith("image/");
              });
            };

            callback(topItems.concat([loadingItem]));

            // Start all the Solr Queries.
            const localQuery = attachmentService.getAttachments(`*${opts.query}*`, false, false, editor, "desc");
            const globalQuery = attachmentService.getAttachments(`*${opts.query}*`, true, false, editor, "desc");

            const wildcardLocalQuery = attachmentService.getAttachments("*", true, false, editor, "desc");
            const wildcardGlobalQuery = attachmentService.getAttachments("*", true, false, editor, "desc");

            // Display the wildcard query result first because it is more likely to be cached.
            const wildcardQuery = Promise.all(
              // Display local results higher in the list.
              [wildcardLocalQuery, wildcardGlobalQuery]
            ).then(function (values) {
              // Show a loading entry until we show the result from the specific query.
              const wildcardItems = [...topItems, loadingItem];

              // Store the attachments so we can display them once we get the specific query results.
              const wildcardAttachments = [];

              // Keep track of the items already in the list to avoid duplicates.
              const wildcardIncludedIds = {};
              values.flat().forEach(function (attachment) {
                if (isImage(attachment) &&
                  !wildcardIncludedIds[attachment.id] &&
                  // Filter based on the filename since we're using a wildcard query
                  attachment.filename[0].toLowerCase().includes(opts.query.toLowerCase())) {
                  wildcardIncludedIds[attachment.id] = true;
                  wildcardAttachments.push(attachment);
                  wildcardItems.push(attachmentToItem(attachment));
                }
              });

              // Show the wildcard query's results.
              callback(wildcardItems);

              return wildcardAttachments;
            });

            // Display all the results once the solr requests are finished.
            Promise.all([
              // Display the specific query results higher in the list.
              localQuery, globalQuery, wildcardQuery
            ]).then(function (values) {

              // We do not do a copy of the array since it won't be used afterwards anyways.
              const finalItems = topItems;

              // Keep track of the items already in the list to avoid duplicates.
              const includedIds = {};
              values.flat().forEach(function (attachment) {
                if (isImage(attachment) && !includedIds[attachment.id]) {
                  includedIds[attachment.id] = true;
                  finalItems.push(attachmentToItem(attachment));
                }
              });

              // Show the final results.
              callback(finalItems);
            });
          });
        },
        outputTemplate: function (item) {
          const imageWidget = editor.widgets.registered.image;

          // Do not do anything when the loading entry is submitted.
          if (item.id === "_loading") {
            return "img::" + item.query;
          }

          require(['jquery', 'resource'], function ($, resource) {
            if (item.id === "_uploadImage") {

              // Reuse attachment suggest code to show the file picker. Provides the xwiki-file-picker module.
              const requiredSkinExtensions = `<script src=` +
                `'${XWiki.contextPath}/${XWiki.servletpath}` +
                `skin/resources/uicomponents/suggest/suggestAttachments.js'` +
                `defer='defer'></script>`;
              $(CKEDITOR.document.$).loadRequiredSkinExtensions(requiredSkinExtensions);

              require(['attachmentService', 'xwiki-file-picker'], (attachmentService, filePicker) => {
                // Open the file picker.
                filePicker.pickLocalFiles({
                  accept: "image/*",
                  multiple: false
                }).then(files => {
                  if (files.length) {
                    // Simulate a paste event, as if the selected image files were pasted in the editor.
                    editor.fire('paste', {
                      method: 'paste',
                      dataValue: '',
                      dataTransfer: new CKEDITOR.plugins.clipboard.dataTransfer({
                        files,
                        types: ['Files'],
                      })
                    });
                    editor.once('fileUploadResponse', () => {
                      // Clear the cache so the new image can appear on next usage of the image quick action.
                      attachmentService.clearCache();
                    });
                  }
                });
              });

              return;
            }

            const resourceReference = {...resource.convertEntityReferenceToResourceReference(
              XWiki.Model.resolve(item.reference, XWiki.EntityType.ATTACHMENT),
              editor.config.sourceDocument.documentReference),
              // Image references are always attachments.
              typed: false
            };

            // Insert the selected image
            imageWidget.insert({
              setImageData: {
                resourceReference,
                src: CKEDITOR.plugins.xwikiResource.getResourceURL(resourceReference, editor)
              }
            });
          });

          return "";
        }
      });

      this.initImageDialogWidget(editor);
    },
    initImageDialogWidget: function(editor) {
      var imageWidget = editor.widgets.registered.image;

      imageWidget.insert = function(args) {
        showImageWizard(editor, this, true, args && args.setImageData);
      };
      imageWidget.edit = function(event) {
        // Prevent the default behavior because we want to use our custom image dialog.
        event.cancel();
        showImageWizard(editor, this, false);
      };
    },
    overrideImageWidget: function(editor, imageWidget) {
      CKEDITOR.plugins.registered['xwiki-image-old'].overrideImageWidget(editor, imageWidget);

      /**
       * Initializes the resize wrapper for the widget.
       *
       * @param widget the widget to initialize
       * @returns {HTMLSpanElement} returns undefined of the resize wrapper was already initialized, otherwise returns
       *   the wrapper span element
       */
      function initResizeWrapper(widget) {
        var resizeWrapper;
        if (widget.element.find('.cke_image_resizer_wrapper', true).count() === 0) {
          resizeWrapper = editor.document.createElement('span');
          resizeWrapper.addClass('cke_image_resizer_wrapper');
          resizeWrapper.append(widget.parts.image);
          resizeWrapper.append(widget.resizer);
        }
        return resizeWrapper;
      }

      /**
       * Update the dom of the widget to place the resize span inside the previously created wrapping span.
       *
       * @param widget the image widget to update
       */
      function moveResizer(widget) {
        if(widget.data.hasCaption) {
          return;
        } 
        var resizeWrapper = initResizeWrapper(widget);

        // Set the data.align to right when it's right so that the mousedown event is tricked into believing the
        // alignment is right.
        // The 'align' value is reset to 'none' on mouseout to prevent it to be persisted.
        widget.resizer.on('mouseover', function () {
          if(widget.data.alignment === 'end') {
            widget.data.align = 'right';
          }
        });
        widget.resizer.on('mouseout', function () {
          widget.data.align = 'none';
        });
        widget.on('data', function () {
          widget.resizer[widget.data.alignment === 'end' ? 'addClass' : 'removeClass']('cke_image_resizer_left');
        });

        // The image has been wrapped in the image resize wrapper. If the image was the widget element then we need to
        // create another widget element to hold the image resize wrapper (which includes the image).
        if (!widget.wrapper.getFirst(node => node.type === CKEDITOR.NODE_ELEMENT)?.hasClass('cke_widget_element')) {
          const oldWidgetElement = widget.element;
          const widgetElementAttrs = [
            'data-cke-widget-data',
            'data-cke-widget-upcasted', 
            'data-cke-widget-keep-attr',
            'data-widget',
          ];

          // Create a new widget element (copying some attributes from the old widget element).
          const newWidgetElement = editor.document.createElement('span');
          newWidgetElement.addClass('cke_widget_element');
          newWidgetElement.setAttributes(widgetElementAttrs.reduce((attrs, name) => {
            attrs[name] = oldWidgetElement.getAttribute(name);
            return attrs;
          }, {}));
          if (resizeWrapper) {
            newWidgetElement.append(resizeWrapper);
          }

          // Replace the widget element.
          widget.wrapper.append(newWidgetElement, true);
          widget.element = newWidgetElement;

          // Cleanup the old widget element.
          oldWidgetElement.removeClass('cke_widget_element');
          oldWidgetElement.removeAttributes(widgetElementAttrs);
        } else if (resizeWrapper) {
          // Simply append the image resize wrapper to the widget element.
          widget.element.append(resizeWrapper, true);
        }
      }

      /**
       * Remove the old way to center an image when found.
       *
       * @param el the element to downcast
       * @param alignment the current alignment of the widget
       */
      function downcastLegacyCenter(el, alignment) {
        if (el.name === 'p' && alignment === 'center') {
          const styles = CKEDITOR.tools.parseCssText(el.attributes.style || '');
          if (styles['text-align'] === 'center') {
            delete styles['text-align'];
          }
          if (CKEDITOR.tools.isEmpty(styles)) {
            el.attributes.style = undefined;
          } else {
            el.attributes.style = CKEDITOR.tools.writeCssText(styles);
          }
        }
      }

      var originalInit = imageWidget.init;
      imageWidget.init = function() {
        originalInit.call(this);

        // Caption
        if (this.parts.caption) {
          this.setData('hasCaption', true);
          // TODO: Add support for editing the caption directly from the dialog (see CKEDITOR-435)
        } else {
          this.setData('hasCaption', false);
        }

        // Style
        var image = this.parts.image;
        if (this.data.hasCaption) {
          image = this.element;
        }
        this.setData('imageStyle', image.getAttribute('data-xwiki-image-style') || '');

        this.setData('border', image.getAttribute('data-xwiki-image-style-border'));
        this.setData('alignment', image.getAttribute('data-xwiki-image-style-alignment'));
        this.setData('textWrap', image.getAttribute('data-xwiki-image-style-text-wrap'));

        moveResizer(this);
        disableResizer(this);
      };

      var originalData = imageWidget.data;

      /**
       * Update the attributes of the widget according to the data.
       *
       * @param widget the widget to update
       * @param setAttribute a set attribute method, it is a function with 3 arguments, the widget, the id of the data
       *   to update, and its value
       * @param removeAttribute a remove attribute method, it is a function with 2 arguments, the widget and the id of
       *   the data to remove
       */
      function computeStyleData(widget, setAttribute, removeAttribute) {
        // Style
        if (widget.data.imageStyle) {
          setAttribute(widget, 'data-xwiki-image-style', widget.data.imageStyle);
        } else {
          removeAttribute(widget, 'data-xwiki-image-style');
        }

        if (widget.data.border) {
          setAttribute(widget, 'data-xwiki-image-style-border', widget.data.border);
        } else {
          removeAttribute(widget, 'data-xwiki-image-style-border');
        }

        // If alignment is undefined, try to convert from the legacy align data property.
        var mapping = {left: 'start', right: 'end', center: 'center'};
        widget.data.alignment = widget.data.alignment || mapping[widget.data.align] || 'none';

        // The old align needs to be undefined otherwise it's not removed when re-inserting the image after the edition,
        // add deprecated attributes to the image.
        widget.data.align = 'none';

        if (widget.data.alignment && widget.data.alignment !== 'none') {
          setAttribute(widget, 'data-xwiki-image-style-alignment', widget.data.alignment);
        } else {
          removeAttribute(widget, 'data-xwiki-image-style-alignment');
        }

        if (widget.data.textWrap) {
          setAttribute(widget, 'data-xwiki-image-style-text-wrap', widget.data.textWrap);
        } else {
          removeAttribute(widget, 'data-xwiki-image-style-text-wrap');
        }
      }

      imageWidget.data = function() {
        /**
         * Update the given attribute at two locations in the widget, the image tag and the widget itself.
         *
         * @param widget the widget to update
         * @param key the attribute key
         * @param value the attribute value
         */
        function setAttribute(widget, key, value) {
          widget.parts.image.removeAttribute(key);
          widget.element.removeAttribute(key);
          if(widget.data.hasCaption) {
            widget.element.setAttribute(key, value);
          }
          widget.parts.image.setAttribute(key, value);
          widget.wrapper.setAttribute(key, value);
        }

        /**
         * Remove the given attribute at two locations on the widget, the image tag and the widget itself.
         *
         * @param widget the widget to update
         * @param key the property key to removew
         */
        function removeAttribute(widget, key) {
          widget.element.removeAttribute(key);
          widget.parts.image.removeAttribute(key);
          widget.wrapper.removeAttribute(key);
        }

        function updateFigureWidth(widget) {
          var figureStyles;
          if (widget.element.hasAttribute('style')) {
            figureStyles = widget.element.getAttribute('style');
          } else {
            figureStyles = "";
          }
          var newStyles;
          if (widget.oldData && widget.oldData.width) {
            newStyles = figureStyles.split(';').filter(function (item) {
              return item.indexOf("width: " + widget.oldData.width + "px") === -1;
            }).join(';');
          } else if (widget.data.width) {
            newStyles = figureStyles.split(';').filter(function (item) {
              return item.indexOf("width: ") === -1;
            }).join(';');
          } else {
            newStyles = figureStyles;
          }

          if (!newStyles.endsWith(';')) {
            newStyles = newStyles + ';';
          }

          if (newStyles === ';') {
            newStyles = '';
          }

          if (widget.data.hasCaption && widget.data.width) {
            newStyles = newStyles + "width: " + widget.data.width + "px;";
          }

          if(newStyles !== '') {
            widget.element.setAttribute('style', newStyles);
          }
        }

        // Caption
        // TODO: Add support for editing the caption directly from the dialog (see CKEDITOR-435)

        computeStyleData(this, setAttribute, removeAttribute);
        updateFigureWidth(this);

        originalData.call(this);
      };

      const originalDowncast = imageWidget.downcast;
      imageWidget.downcast = function (...args) {
        const alignment = this.data.alignment;
        const element = originalDowncast.apply(this, args);
        downcastLegacyCenter(element, alignment);
        if (!this.parts.caption) {
          // Image widget without caption.
          // Remove the wrapping span used for the resize handle.
          const img = element.name === 'img' ? element : element.findOne('img', true);
          let imageResizeWrapper = img;
          while (imageResizeWrapper && imageResizeWrapper.name !== 'span' && imageResizeWrapper !== element) {
            imageResizeWrapper = imageResizeWrapper.parent;
          }
          if (imageResizeWrapper?.name === 'span' && imageResizeWrapper !== element) {
            imageResizeWrapper.replaceWith(imageResizeWrapper.children[0]);
          }
        }

        return element;
      };
    }
  });

})();
