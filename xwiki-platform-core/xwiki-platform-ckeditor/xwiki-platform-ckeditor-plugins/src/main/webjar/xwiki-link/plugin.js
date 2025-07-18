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

  // Declare the configuration namespace.
  CKEDITOR.config['xwiki-link'] = CKEDITOR.config['xwiki-link'] || {
    __namespace: true
  };

  var $ = jQuery;
  var wikiLinkClassPattern =  /\bwiki(\w*)link\b/i;
  var resourceTypeToLinkType = {
    attach: 'attachment',
    mailto: 'external',
    unc: 'external',
    url: 'external'
  };

  CKEDITOR.plugins.add('xwiki-link', {
    requires: 'xwiki-marker,xwiki-resource,xwiki-localization,balloontoolbar',

    init: function(editor) {
      // Remove the link markers (XML comments used by the XWiki Rendering to detect wiki links) when the content is
      // loaded (toHtml), in order to protect them, and add them back when the content is saved (toDataFormat).
      editor.plugins['xwiki-marker'].addMarkerHandler(editor, 'wikilink', {
        // startLinkComment: CKEDITOR.htmlParser.comment
        // content: CKEDITOR.htmlParser.node[]
        toHtml: function(startLinkComment, content) {
          if (content.length === 1 && content[0].type === CKEDITOR.NODE_ELEMENT && content[0].children.length === 1 &&
              content[0].children[0].name === 'a') {
            // Remove the link wrapper while editing, but keep the link type in order to preserve the link styles.
            var linkWrapper = content[0];
            var link = linkWrapper.children[0];
            linkWrapper.replaceWithChildren();
            // We store the link type in a data attribute because we don't want the user to see it in the link dialog.
            var linkType = (wikiLinkClassPattern.exec(linkWrapper.attributes['class']) || ['', ''])[1];
            link.attributes['data-linktype'] = linkType;
            // Store the link reference.
            var reference = startLinkComment.value.substring('startwikilink:'.length);
            link.attributes['data-reference'] = CKEDITOR.tools.unescapeComment(reference);
            // Handle the auto-generated link content.
            if (link.children.length === 1 && link.children[0].type === CKEDITOR.NODE_ELEMENT &&
                link.children[0].hasClass('wikigeneratedlinkcontent')) {
              // Store the initial (generated) link content in order to be able to detect changes.
              link.attributes['data-wikigeneratedlinkcontent'] = link.children[0].getHtml();
              link.children[0].replaceWithChildren();
            }
            // Handle free-standing links.
            if (link.hasClass('wikimodel-freestanding')) {
              link.attributes['data-freestanding'] = true;
              // This is an internal class that should not be visible when editing the link.
              link.removeClass('wikimodel-freestanding');
            }
          } else {
            // Unexpected HTML structure inside link markers. Keep the markers.
            return false;
          }
        },
        // element: CKEDITOR.htmlParser.element
        isMarked: function(element) {
          return element.name === 'a' && element.attributes['data-reference'];
        },
        // link: CKEDITOR.htmlParser.element
        toDataFormat: function(link) {
          // Add the start/stop link markers.
          var reference = CKEDITOR.tools.escapeComment(link.attributes['data-reference']);
          var startLinkComment = new CKEDITOR.htmlParser.comment('startwikilink:' + reference);
          var stopLinkComment = new CKEDITOR.htmlParser.comment('stopwikilink');
          startLinkComment.insertBefore(link);
          stopLinkComment.insertAfter(link);
          delete link.attributes['data-reference'];
          // Wrap the link.
          link.wrapWith(new CKEDITOR.htmlParser.element('span', {
            'class': 'wiki' + (link.attributes['data-linktype'] || '') + 'link'
          }));
          delete link.attributes['data-linktype'];
          // Handle the auto-generated link content.
          if (link.attributes['data-wikigeneratedlinkcontent'] === link.getHtml()) {
            // The link content was generated and hasn't been modified. Wrap the link content.
            link.add(new CKEDITOR.htmlParser.element('span', {'class': 'wikigeneratedlinkcontent'}), 0);
            var linkContent = link.children[0];
            while (link.children.length > 1) {
              var child = link.children[1];
              child.remove();
              linkContent.add(child);
            }
            // Only the links with auto-generated content can be free-standing.
            if (link.attributes['data-freestanding'] === 'true') {
              link.addClass('wikimodel-freestanding');
            }
          }
          delete link.attributes['data-wikigeneratedlinkcontent'];
          delete link.attributes['data-freestanding'];
        }
      });

      // Register the [ autocomplete using the ckeditor mentions plugin.
      editor.config.mentions = editor.config.mentions || [];
      editor.config.mentions.push({
        followingSpace: true,
        marker: '[',
        minChars: 0,
        itemsLimit: 6,
        feed: function (opts, callback) {
          // We use the source document to compute the feed URL because we want the suggested link references to be
          // relative to the edited document (we want the editor to output relative references as much as possible).
          $.getJSON(editor.config.sourceDocument.getURL('get', $.param({
            sheet: 'CKEditor.LinkSuggestions',
            outputSyntax: 'plain',
            language: XWiki.locale,
            input: opts.query
          })), function (data) {

            const uploadItem = {
              id: "_uploadAttachment",
              iconClass: "fa fa-upload",
              iconURL: "",
              label: editor.localization.get('xwiki-link.upload'),
              hint: editor.localization.get('xwiki-link.uploadHint')
            };


            // Upload attachment item should be the first in the list.
            var callbackData = data;
            if (uploadItem.label.toLowerCase().startsWith(opts.query.toLowerCase())) {
              callbackData = [uploadItem, ...callbackData];
            }

            callback(callbackData);

          });
        },
        itemTemplate:
          `<li data-id="{id}" class="ckeditor-autocomplete-item">
            <div>
              <span class="ckeditor-autocomplete-item-icon-wrapper">` +
                // We have to output both icon types but normally only one is defined and the other is hidden.
                `<img src="{iconURL}"/>
                <span class="{iconClass}"></span>
              </span>
              <span class="ckeditor-autocomplete-item-label">{label}</span>
            </div>
            <div class="ckeditor-autocomplete-item-hint">{hint}</div>
          </li>`,
        outputTemplate: function (item) {
          // Upload a new attachment.
          if (item.id === "_uploadAttachment") {

            // Reuse attachment suggest code to show the file picker. Provides the xwiki-file-picker module.
            const requiredSkinExtensions = `<script src=` +
              `'${XWiki.contextPath}/${XWiki.servletpath}` +
              `skin/resources/uicomponents/suggest/suggestAttachments.js'` +
              `defer='defer'></script>`;
            $(CKEDITOR.document.$).loadRequiredSkinExtensions(requiredSkinExtensions);

            require(['xwiki-file-picker'], function(filePicker) {
              // Open the file picker
              filePicker.pickLocalFiles({
                accept: "*",
                multiple: false
              }).then(function (files) {
                if (files.length) {
                  // Disable the upload image widget temporarily because even if the selected file is an image, we want
                  // to insert a link to the uploaded image attachment.
                  const uploadImageWidgetDef = editor.widgets.registered.uploadimage;
                  if (uploadImageWidgetDef) {
                    uploadImageWidgetDef._supportedTypes = uploadImageWidgetDef.supportedTypes;
                    // Use a regular expression that doesn't match any file type.
                    uploadImageWidgetDef.supportedTypes = /\b\B/;
                  }
                  try {
                    // Simulate a paste event, as if the selected files were pasted in the editor.
                    editor.fire('paste', {
                      method: 'paste',
                      dataValue: '',
                      dataTransfer: new CKEDITOR.plugins.clipboard.dataTransfer({
                        files,
                        types: ['Files'],
                      })
                    });
                  } finally {
                    if (uploadImageWidgetDef) {
                      // Restore the supported image types configuration.
                      uploadImageWidgetDef.supportedTypes = uploadImageWidgetDef._supportedTypes;
                    }
                  }
                }
              });
            });

            return "";
          }
          // Insert the selected link.
          return '<a href="{url}" data-reference="{typed}|-|{type}|-|{reference}">{label}</a>';
        }
      });

    },

    afterInit: function(editor) {
      if (CKEDITOR.plugins.link) {
        addSupportForOpeningLinksInNewTab(editor);
        addLinkBalloonToolBar(editor);
      }
    },

    onLoad: function() {
      overrideLinkPlugin();
    }
  });

  var overrideLinkPlugin = function() {
    var linkPlugin = CKEDITOR.plugins.link;
    if (!linkPlugin) {
      return;
    }
    // Parse the link resource reference before the link dialog is opened.
    if (typeof linkPlugin.parseLinkAttributes === 'function') {
      var oldParseLinkAttributes = linkPlugin.parseLinkAttributes;
      linkPlugin.parseLinkAttributes = function(editor, element) {
        // This is the data passed to the link dialog.
        var data = oldParseLinkAttributes.call(linkPlugin, editor, element);
        if (element) {
          var serializedResourceReference = element.getAttribute('data-reference');
          if (serializedResourceReference) {
            data.resourceReference = CKEDITOR.plugins.xwikiResource.parseResourceReference(serializedResourceReference);
          } else {
            // Fall-back on URL or Path resource reference.
            var reference = element.getAttribute('href') || '';
            var type = reference.indexOf('://') < 0 ? 'path' : 'url';
            data.resourceReference = {type: type, reference: reference};
          }
          var wikiGeneratedLinkContent = element.getAttribute('data-wikigeneratedlinkcontent');
          if (wikiGeneratedLinkContent) {
            data.wikiGeneratedLinkContent = wikiGeneratedLinkContent;
          }
        }
        return data;
      };
    }
    // Serialize the link resource reference after the link dialog is closed.
    if (typeof linkPlugin.getLinkAttributes === 'function') {
      var oldGetLinkAttributes = linkPlugin.getLinkAttributes;
      linkPlugin.getLinkAttributes = function(editor, data) {
        // The data comes from the link dialog.
        var attributes = oldGetLinkAttributes.call(linkPlugin, editor, data);
        var resourceReference = data.resourceReference;
        if (resourceReference) {
          attributes.set['data-reference'] = CKEDITOR.plugins.xwikiResource
            .serializeResourceReference(resourceReference);
          attributes.set['data-linktype'] = resourceTypeToLinkType[resourceReference.type] || '';
        } else {
          attributes.removed.push('data-reference', 'data-linktype');
        }
        if (data.wikiGeneratedLinkContent) {
          attributes.set['data-wikigeneratedlinkcontent'] = data.wikiGeneratedLinkContent;
        } else {
          attributes.removed.push('data-wikigeneratedlinkcontent');
        }
        return attributes;
      };
    }
  };

  CKEDITOR.on('dialogDefinition', function(event) {
    // Make sure we affect only the editors that load this plugin.
    if (!event.editor.plugins['xwiki-link']) {
      return;
    }

    // Take the dialog window name and its definition from the event data.
    var dialogName = event.data.name;
    var dialogDefinition = event.data.definition;
    if (dialogName === 'link') {
      enhanceLinkDialog(dialogDefinition, event.editor);
    }
  });

  var enhanceLinkDialog = function(dialogDefinition, editor) {
    var resourcePicker = createResourcePicker(editor);
    replaceLinkTypeSelect(dialogDefinition, resourcePicker);

    // Bind the value of the email address and url fields to the resource reference field.
    // Hide the email address, url and protocol fields because we're using the resource picker instead.
    var infoTab = dialogDefinition.getContents('info');
    var resourcePlugin = CKEDITOR.plugins.xwikiResource;
    resourcePlugin.bindResourcePicker(infoTab.get('emailAddress'), ['info', resourcePicker.id], true);
    var urlField = infoTab.get('url');
    resourcePlugin.bindResourcePicker(urlField, ['info', resourcePicker.id]);
    infoTab.get('urlOptions').className = 'hidden';

    // Add the link options toggle.
    infoTab.add(createOptionsToggle(editor), 'urlOptions');
    dialogDefinition.minHeight = 100;

    // Add page link options.
    infoTab.add({
      type: 'vbox',
      id: 'docOptions',
      children: [
        createQueryStringField({id: 'docQueryString'}, 'doc', editor),
        createAnchorField({id: 'docAnchor'}, 'doc', editor)
      ]
    });

    // Add attachment link options.
    infoTab.add({
      type: 'vbox',
      id: 'attachOptions',
      children: [
        createQueryStringField({id: 'attachQueryString'}, 'attach', editor)
      ]
    });

    // Bind the mail link options to the corresponding resource reference parameters.
    bindToResourceParameter(infoTab.get('emailSubject'), 'subject', 'mailto');
    bindToResourceParameter(infoTab.get('emailBody'), 'body', 'mailto');

    // Remove the custom focus handler set by the link dialog because we want the first input (which is the resource
    // picker) to be focused when the dialog is opened.
    delete dialogDefinition.onFocus;

    // Use the resource label (e.g. the wiki page title or the attachment file name) as the default link label when the
    // link label text input is left empty.
    overwriteDefaultLinkLabel(dialogDefinition);

    resourcePlugin.updateResourcePickerOnFileBrowserSelect(dialogDefinition,
      ['info', resourcePicker.id], ['upload', 'uploadButton']);
  };

  var replaceLinkTypeSelect = function(dialogDefinition, newElementDefinition) {
    var linkTypeDefinition = dialogDefinition.getContents('info').get('linkType');
    // The resource picker takes care of setting the link type value when the resource type changes.
    delete linkTypeDefinition.setup;
    CKEDITOR.plugins.xwikiDialog.replaceWith(dialogDefinition, 'linkType', {
      type: 'vbox',
      children: [newElementDefinition, linkTypeDefinition],
      onLoad: function() {
        this.getDialog().getContentElement('info', 'linkType').getElement().getParent().hide();
      }
    });
  };

  var createResourcePicker = function(editor) {
    return CKEDITOR.plugins.xwikiResource.createResourcePicker({
      // The resource picker is displayed after the link label input.
      tabIndex: 1,
      resourceTypes: (editor.config['xwiki-link'] || {}).resourceTypes || ['doc', 'attach', 'url', 'mailto'],
      getValue: function() {
        var data = {resourceReference: this.base.getValue.apply(this, arguments)};
        // Collect the resource reference parameters.
        this.getDialog().foreach(function(field) {
          if (field.resourceReferenceParameter === true && typeof field.commit === 'function') {
            field.commit(data);
          }
        });
        return data.resourceReference;
      },
      setup: function(data) {
        // Create a link to a new page if the resource reference is not provided.
        var resourceReference = data.resourceReference || this.getDefaultResourceReference();
        if (resourceReference.type === 'space' && this.resourceTypes.indexOf('space') < 0 &&
            this.resourceTypes.indexOf('doc') >= 0) {
          // Convert the space resource reference to a document resource reference.
          resourceReference = {
            type: 'doc',
            // We know the target document precisely (no ambiguity).
            typed: true,
            reference: resourceReference.reference + '.WebHome'
          };
        }
        this.setValue(resourceReference);
      },
      commit: function(data) {
        var resourceReference = this.getValue();
        if (resourceReference.type === 'url' && resourceReference.reference &&
            resourceReference.reference.indexOf('://') < 0) {
          // The users often omit the protocol / scheme when typing URLs.
          resourceReference.reference = 'https://' + resourceReference.reference;
        }
        if (typeof resourceReference.typed !== 'boolean') {
          resourceReference.typed = resourceReference.type !== 'doc' &&
            (resourceReference.type !== 'url' || resourceReference.reference.indexOf('://') < 0);
        }
        data.resourceReference = resourceReference;
      },
      onResourceTypeChange: function(event, data) {
        this.base.onResourceTypeChange.apply(this, arguments);
        var dialog = this.getDialog();
        // Update the value of the link type select because it is used internally by the link dialog. By default there
        // are three link types available: url, anchor and email. We use only url and email because anchor has been
        // merged with url (url links have the option to specify an anchor).
        dialog.setValueOf('info', 'linkType', data.newValue === 'mailto' ? 'email' : 'url');
        // Show the upload tab only for attachment resources.
        if (data.newValue !== 'attach') {
          dialog.hidePage('upload');
        }
        // Show the corresponding options (and hide the rest).
        this.resourceTypes.forEach(function(resourceType) {
          // We reuse the existing email options.
          var optionsId = (resourceType === 'mailto' ? 'email' : resourceType) + 'Options';
          var options = dialog.getContentElement('info', optionsId);
          if (options) {
            var container = options.getElement().getParent().getParent();
            // Some resource types (e.g. URL) have their options hidden from the dialog definition.
            if (resourceType === data.newValue && !options.getElement().hasClass('hidden')) {
              container.addClass('linkOptions');
            } else {
              container.removeClass('linkOptions').hide();
            }
          }
        });
        dialog.getContentElement('info', 'optionsToggle').sync();
        dialog.layout();
      },
      getDefaultResourceReference: function() {
        // Compute the default reference by cleaning up the link label.
        // Fall-back on the empty string if there's no text selection (e.g. if an image is selected).
        var linkLabel = this.getDialog().getParentEditor().getSelection().getSelectedText() || '';
        // Normalize the white space.
        var defaultReference = linkLabel.trim().replace(/\s+/g, ' ');
        return {
          type: this.resourceTypes[0],
          reference: defaultReference,
          // Make sure the picker doesn't try to resolve the link label as a resource reference.
          isNew: true,
          isInitialValue: true
        };
      }
    });
  };

  var createOptionsToggle = function(editor) {
    return {
      id: 'optionsToggle',
      type: 'html',
      html: '<button class="linkOptionsToggle" type="button">' +
              '<label class="cke_dialog_ui_labeled_label">' +
                '<span class="arrow arrow-right"></span> ' +
                CKEDITOR.tools.htmlEncode(editor.localization.get('xwiki-link.options')) +
              '</label>' +
            '</button>',
      onLoad: function() {
        // Since we do not (and cannot without deeper changes) use the 'button' type, 
        // we need to add this element explicitely to the Dialog focus list.
        // We need to hardcode the position since we do not have access to the setupFocus function to reorder the list
        // relative to native tab order.
        // The four elements before this button are: display link, page selection *3
        this.getDialog().addFocusable(this.getElement() , 4);
        // Without this, the keyboard press on this focusable element will trigger the click twice...
        this.getElement().removeAllListeners();
        // We use the CKEDITOR.dom.element event utilities. This `on` is not related to JQuery.
        this.getElement().on('click', this.toggleLinkOptions, this);
      },
      toggleLinkOptions: function(event) {
        var arrow = this.getArrow();
        var linkOptions = this.getLinkOptions();
        if (arrow.hasClass('arrow-down')) {
          arrow.removeClass('arrow-down').addClass('arrow-right');
          linkOptions.hide();
        } else {
          arrow.removeClass('arrow-right').addClass('arrow-down');
          linkOptions.show();
        }
      },
      sync: function() {
        var arrow = this.getArrow();
        var linkOptions = this.getLinkOptions();
        if (linkOptions) {
          this.getElement().show();
          if (arrow.hasClass('arrow-down')) {
            linkOptions.show();
          } else {
            linkOptions.hide();
          }
        } else {
          this.getElement().hide();
        }
      },
      getArrow: function() {
        return this.getElement().findOne('.arrow');
      },
      getLinkOptions: function() {
        return this.getDialog().getElement().findOne('.linkOptions');
      }
    };
  };

  var createAnchorField = function(definition, resourceType, editor) {
    return createReferenceParameterField('anchor', CKEDITOR.tools.extend(definition || {}, {
      label: editor.localization.get('xwiki-link.anchor')
    }), resourceType);
  };

  var createQueryStringField = function(definition, resourceType, editor) {
    return createReferenceParameterField('queryString', CKEDITOR.tools.extend(definition || {}, {
      label: editor.localization.get('xwiki-link.queryString')
    }), resourceType);
  };

  var createReferenceParameterField = function(parameterName, definition, resourceType) {
    return CKEDITOR.tools.extend(definition || {}, {
      type: 'text',
      resourceReferenceParameter: true,
      setup: setupFromResourceParameter(parameterName, resourceType),
      commit: commitToResourceParameter(parameterName, resourceType)
    });
  };

  var setupFromResourceParameter = function(parameterName, resourceType, oldSetup) {
    return function(data) {
      if (typeof oldSetup === 'function') {
        oldSetup.apply(this, arguments);
      }
      var resourceReference = data.resourceReference || {};
      if (!resourceType || resourceType === resourceReference.type) {
        var referenceParameters = resourceReference.parameters || {};
        this.setValue(referenceParameters[parameterName] || '');
      }
    };
  };

  var commitToResourceParameter = function(parameterName, resourceType, oldCommit) {
    return function(data) {
      var value = this.getValue().trim();
      if (value !== '' && (!resourceType || resourceType === data.resourceReference.type)) {
        data.resourceReference.parameters = data.resourceReference.parameters || {};
        data.resourceReference.parameters[parameterName] = value;
      }
      if (typeof oldCommit === 'function') {
        oldCommit.apply(this, arguments);
      }
    };
  };

  var bindToResourceParameter = function(definition, parameterName, resourceType) {
    definition.resourceReferenceParameter = true;
    definition.setup = setupFromResourceParameter(parameterName, resourceType, definition.setup);
    definition.commit = commitToResourceParameter(parameterName, resourceType, definition.commit);
  };

  /**
   * Use the resource label (e.g. the wiki page title or the attachment file name) as the default link label when the
   * link label text input is left empty.
   */
  var overwriteDefaultLinkLabel = function(dialogDefinition) {
    var linkLabelField = dialogDefinition.getContents('info').get('linkDisplayText');
    var oldSetup = linkLabelField.setup;
    var oldCommit = linkLabelField.commit;
    CKEDITOR.tools.extend(linkLabelField, {
      setup: function(data) {
        if (typeof oldSetup === 'function') {
          oldSetup.apply(this, arguments);
        }
        if (!this.resourceReferenceField) {
          // We don't add the event listener onLoad because the resource reference field is initialized later.
          this.resourceReferenceField = this.getDialog().getContentElement('info', 'resourceReference');
          // Update the wiki generated link content whenever a new resource is selected.
          $(this.resourceReferenceField.getElement().$).on('selectResource',
            this.maybeUpdateWikiGeneratedLinkContent.bind(this));
        }
        this.wikiGeneratedLinkContent = data.wikiGeneratedLinkContent;
        delete this.validationRequest;
      },
      validate: function() {
        if (!this.validationRequest) {
          // Trigger a new validation.
          this.validationRequest = this.validateAsync().always((function() {
            // Re-submit the dialog after the current event is handled.
            setTimeout((function() {
              this.getDialog().click('ok');
            }).bind(this), 0);
          }).bind(this));
          return false;
        } else if (this.validationRequest.state() === 'pending') {
          // Block the submit while the validation takes place.
          return false;
        } else {
          // Trigger a new validation next time validate() is called.
          delete this.validationRequest;
          return true;
        }
      },
      validateAsync: function() {
        // Update the wiki generated link content based on the selected resource before submitting the dialog.
        // Call the base function because we don't need the resource reference parameters.
        var resourceReference = this.resourceReferenceField.base.getValue.call(this.resourceReferenceField);
        var selectedResourceReference = (this.resourceReferenceField.selectedResource || {}).reference || {};
        var selectedResource = {'reference': resourceReference};
        if (selectedResourceReference.type === resourceReference.type &&
            selectedResourceReference.reference === resourceReference.reference) {
          // The previous getValue() call returns a resource reference so it doesn't include the meta data associated
          // with the resource (e.g. its title). We need the meta data to determine the link label so we use the cached
          // resource object, in case it wasn't modified by the user.
          selectedResource = this.resourceReferenceField.selectedResource;
        }
        return this.maybeUpdateWikiGeneratedLinkContent(null, selectedResource);
      },
      commit: function(data) {
        if (typeof oldCommit === 'function') {
          oldCommit.apply(this, arguments);
        }
        data.wikiGeneratedLinkContent = this.wikiGeneratedLinkContent;
      },
      // Update the link label when a different resource is selected, if there's no label set or the current label is
      // generated automatically.
      maybeUpdateWikiGeneratedLinkContent: function(event, resource) {
        var value = this.getValue();
        if (value === '' || value === this.wikiGeneratedLinkContent) {
          return this.maybeGetWikiGeneratedLinkContent(resource).done((function(wikiGeneratedLinkContent) {
            this.setValue(wikiGeneratedLinkContent);
            this.wikiGeneratedLinkContent = wikiGeneratedLinkContent;
          }).bind(this)).fail((function() {
            if (value === '') {
              // Fall-back on the resource label.
              this.setValue(this.getResourceLabel(resource));
            }
            delete this.wikiGeneratedLinkContent;
          }).bind(this));
        } else {
          return $.Deferred().resolve();
        }
      },
      // We use this as a fall-back when the request for the wiki generated link content fails.
      getResourceLabel: function(resource) {
        return (resource && resource.title) ||
          (resource && resource.entityReference && resource.entityReference.name) ||
          (resource && resource.reference && resource.reference.reference) ||
          this.getDialog().getParentEditor().localization.get('xwiki-link.defaultLabel');
      },
      maybeGetWikiGeneratedLinkContent: function(resource) {
        var config = this.getDialog().getParentEditor().config['xwiki-link'] || {};
        if (config.autoGenerateLabels) {
          return this.getWikiGeneratedLinkContent(resource);
        } else {
          // Use the resource label instead.
          return $.Deferred().reject();
        }
      },
      getWikiGeneratedLinkContent: function(resource) {
        var sendRequest = (function() {
          this.getDialog().setState(CKEDITOR.DIALOG_STATE_BUSY);
          var resourceReference = (resource && resource.reference) || {};
          var config = this.getDialog().getParentEditor().config['xwiki-link'] || {};
          return $.get(config.labelGenerator, resourceReference).then(function(html) {
            // Extract the wiki generated link content from the HTML.
            return $(html).find('.wikigeneratedlinkcontent').text();
          }).always((function() {
            this.getDialog().setState(CKEDITOR.DIALOG_STATE_IDLE);
          }).bind(this));
        }).bind(this);
        // Wait until the previous request is handled, by chaining the requests.
        this.wikiGeneratedLinkContentRequest = (this.wikiGeneratedLinkContentRequest || $.Deferred().resolve())
          .then(sendRequest, sendRequest);
        return this.wikiGeneratedLinkContentRequest;
      }
    }, true);
  };

  // Adds a context menu entry to open the selected link in a new tab.
  // See https://github.com/mlewand/ckeditor-plugin-openlink
  var addSupportForOpeningLinksInNewTab = function(editor) {
    editor.addCommand('xwiki-link-open', {
      exec: function(editor) {
        var anchor = getActiveLink(editor);
        if (anchor) {
          var href = anchor.getAttribute('href');
          if (href) {
            window.open(href);
          }
        }
      }
    });

    if (typeof editor.addMenuItem === 'function') {
      editor.addMenuItem('xwiki-link-open', {
        label: editor.localization.get('xwiki-link.openInNewTab'),
        command: 'xwiki-link-open',
        group: 'link',
        order: -1
      });
    }

    if (editor.contextMenu) {
      editor.contextMenu.addListener(function(startElement, selection, path) {
        if (startElement) {
          var anchor = getActiveLink(editor);
          if (anchor && anchor.getAttribute('href')) {
            return {'xwiki-link-open': CKEDITOR.TRISTATE_OFF};
          }
        }
      });
      editor.contextMenu._.panelDefinition.css.push('.cke_button__xwiki-link-open_icon {' +
        CKEDITOR.skin.getIconStyle('link') + '}');
    }

    // Returns the DOM element of the active (currently focused) link. It has also support for linked image widgets.
    // @return {CKEDITOR.dom.element}
    var getActiveLink = function(editor) {
      var anchor = CKEDITOR.plugins.link.getSelectedLink(editor),
        // We need to do some special checking against widgets availability.
        activeWidget = editor.widgets && editor.widgets.focused;
      // If default way of getting links didn't return anything useful..
      if (!anchor && activeWidget && activeWidget.name == 'image' && activeWidget.parts.link) {
        // Since CKEditor 4.4.0 image widgets may be linked.
        anchor = activeWidget.parts.link;
      }
      return anchor;
    };
  };

  // See https://ckeditor.com/docs/ckeditor4/latest/features/balloontoolbar.html
  var addLinkBalloonToolBar = function(editor) {
    editor.ui.addButton('xwiki-link-open', {
      label: editor.localization.get('xwiki-link.openInNewTab'),
      command: 'xwiki-link-open',
      // We use a tool bar group that is not shown on the main tool bar so that this button is shown only on the link
      // balloon tool bar.
      toolbar: 'xwiki-link'
    });

    editor.balloonToolbars.create({
      buttons: 'xwiki-link-open,Link,Unlink',
      cssSelector: 'a[href]'
    });
  };
})();
