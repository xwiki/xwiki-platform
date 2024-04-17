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

define('xwiki-attachment-picker-translation-keys', {
  prefix: 'attachment.picker.',
  keys: [
    'solrSearch.query.errorMessage',
    'searchField.placeholder',
    'searchField.scope.currentPage',
    'searchField.scope.allPages',
  ]
});

define('xwiki-attachment-picker',
  ['jquery',
  'xwiki-attachments-icon',
  'xwiki-l10n!xwiki-attachment-picker-translation-keys',
  'xwiki-attachment-picker-solr-search'],
  function ($, attachmentsIcon, translations, SolrSearch) {
    'use strict';

    const ATTACHMENT_PICKER_INITIALIZED_CLASS = 'initialized';

    /**
     * Utility function to debounce an event. The callback is delayed until no similar event is received before the end
     * of the delay.
     *
     * @param callback the function to call when the delay is passed
     * @param delay the delay in milliseconds before callback is called
     * @returns {(function(...[*]): void)|*} the function to pass to the event handler
     */
    function debounce(callback, delay) {
      let timeout;
      return function (...args) {
        clearTimeout(timeout);
        timeout = setTimeout(() => callback.apply(this, args), delay);
      };
    }

    /**
     * Handle the rendering and the events of the search box in the attachment picker.
     */
    class SearchBlock {
      constructor(rootBlock, searchBlock, resultsBlock) {
        this.rootBlock = rootBlock;
        this.searchBlock = searchBlock;
        this.resultsBlock = resultsBlock;
      }

      initialize(cb) {
        this.searchBlock.addClass('xform');
        const searchField = $("<input/>")
          .prop("type", "text")
          .addClass('form-control')
          .prop('placeholder', translations.get('searchField.placeholder'));

        const inputGroup = $('<div/>').addClass('input-group');
        const inputGroupBtn = $('<div/>').addClass('input-group-btn')
          .attr('data-toggle', 'buttons');
        const labelLocal = $('<label/>')
          .addClass('btn')
          .addClass('btn-primary')
          .addClass('active')
          .append($('<input/>')
            .attr('type', 'radio')
            .attr('checked', true)
            .attr('name', 'scope')
            .attr('value', 'local'))
          .append(document.createTextNode(translations.get('searchField.scope.currentPage')));
        const labelGlobal = $('<label/>')
          .addClass('btn')
          .addClass('btn-primary')
          .append($('<input/>')
            .attr('type', 'radio')
            .attr('name', 'scope')
            .attr('value', 'global'))
          .append(document.createTextNode(translations.get('searchField.scope.allPages')));

        inputGroup.append(searchField);
        inputGroup.append(inputGroupBtn);
        inputGroupBtn.append(labelLocal);
        inputGroupBtn.append(labelGlobal);

        this.searchBlock.append(inputGroup);
        this.solrSearch = new SolrSearch({
          limit: parseInt(this.rootBlock.data('xwiki-attachment-picker-limit')),
          target: this.rootBlock.data('xwiki-attachment-picker-target'),
          solrOptions: {
            filter: this.rootBlock.data('xwiki-attachment-picker-filter')
          }
        });
        this.cb = cb;
        this.search('').finally(() => {
          const searchTextInput = this.searchBlock.find('input[type="text"]');
          const listener = debounce(() => {
            const isGlobal = this.searchBlock.find("input[name='scope']:checked").val() === 'global';
            return this.search(searchTextInput.val(), isGlobal);
          }, 500);
          searchTextInput.on('input', listener);
          this.searchBlock.find('input[type="radio"]').on('change', listener);
        });
      }

      /**
       * Search for the attachments matching the provided query, either on the current document, or in the whole farm.
       * @param query the string query, for instance an attachment name
       * @param isGlobal when true, search for the query in the whole farm, when false, only search on the current
       * document
       */
      search(query, isGlobal) {
        const loadingClass = 'loading';
        this.resultsBlock.empty();
        this.resultsBlock.addClass(loadingClass);
        return this.solrSearch.search(query, isGlobal).then(this.cb)
          .catch((error) => {
            console.log(error);
            new XWiki.widgets.Notification(translations.get('solrSearch.query.errorMessage'), 'error');
          }).finally(() => {
            this.resultsBlock.removeClass(loadingClass);
          });
      }
    }

    /**
     * Main class of the attachment picker. Initialize and plug together the other classes of this module.
     */
    class AttachmentGalleryPicker {

      constructor(attachmentGalleryPicker) {
        this.attachmentPicker = attachmentGalleryPicker;
        const attachmentPickerSearch = this.attachmentPicker.find('.attachmentPickerSearch');
        this.resultsBlock = this.attachmentPicker.find('.attachmentPickerResults');
        this.searchBlock = new SearchBlock(this.attachmentPicker, attachmentPickerSearch, this.resultsBlock);
        this.noResultsBlock = this.attachmentPicker.find('.attachmentPickerNoResults');
        this.globalSelectionBlock = this.attachmentPicker.find('.attachmentPickerGlobalSelection');
      }

      initialize() {
        if (!this.attachmentPicker.hasClass(ATTACHMENT_PICKER_INITIALIZED_CLASS)) {
          this.searchBlock.initialize(results => {
            this.resultsBlock.empty();
            this.noResultsBlock.addClass('hidden');
            this.globalSelectionBlock.addClass('hidden');
            if (results.length > 0) {
              this.initializeWhenHasResults(results);
            } else {
              this.noResultsBlock.removeClass('hidden');
            }
            if (this.resultsBlock.find('.selected').length === 0 && this.selected !== undefined) {
              this.unselect();
            }
          });
          this.initSelectedAttachment();
          this.attachmentPicker.addClass(ATTACHMENT_PICKER_INITIALIZED_CLASS);
        }
      }

      initializeWhenHasResults(results) {
        results.forEach(this.addAttachment.bind(this));
      }

      initSelectedAttachment() {
        this.resultsBlock.on('click', 'a', (event) => {
          event.preventDefault();
          // We only select the attachment on the first click of a chain.
          // The `detail` event property for a click indicates the position of this click in the chain
          // When double-clicking, detail === 1 means that this click is the first click of a double click.
          if (event.detail === 1) {
            const parent = $(event.currentTarget).parents('.attachmentGroup');

            if (this.selected !== undefined && parent.data('id') === this.selected) {
              this.unselect(parent);
            } else {
              this.resultsBlock.find('.attachmentGroup').removeClass('selected');
              parent.addClass('selected');
              this.selected = parent.data('id');
              this.attachmentPicker.trigger('xwiki:attachmentGalleryPicker:selected', this.selected);
              this.updateGlobalSelectionWarning(parent);
            }
          }
        });
      }

      updateGlobalSelectionWarning(parent) {
        if (parent.hasClass('globalAttachment')) {
          this.globalSelectionBlock.removeClass('hidden');
        } else {
          this.globalSelectionBlock.addClass('hidden');
        }
      }

      unselect(parent) {
        this.selected = undefined;
        if (parent) {
          parent.removeClass('selected');
          this.globalSelectionBlock.addClass('hidden');
        }
        this.attachmentPicker.trigger('xwiki:attachmentGalleryPicker:unselected');
      }

      addAttachment(result, index) {
        const attachmentReference = XWiki.Model.resolve(result.id, XWiki.EntityType.ATTACHMENT);
        const downloadURL = new XWiki.Attachment(attachmentReference).getURL();
        const filename = result.filename[0];
        const preview = this.initPreviewElement(result, downloadURL, filename);

        const textSpan = $('<span>').text(filename).prop('title', filename).addClass('attachmentTitle');
        const link = $('<a></a>')
          .prop('title', filename)
          .prop('href', downloadURL)
          .data('index', index)
          .append($('<span>').append(preview).addClass('previewWrapper'))
          .append('<br/>')
          .append(textSpan);

        const attachmentGroup = $('<span class="attachmentGroup">')
          .append($('<div/>').append(link))
          .data('id', result.id);
        if (result.isLocal) {
          attachmentGroup.addClass('localAttachment');
        } else {
          attachmentGroup.addClass('globalAttachment');
        }
        if (this.selected !== undefined && result.id === this.selected) {
          attachmentGroup.addClass('selected');
          this.updateGlobalSelectionWarning(attachmentGroup);
        }
        this.resultsBlock.append(attachmentGroup);
      }

      initPreviewElement(result, downloadURL, filename) {
        let preview;
        const mimeType = result.mimetype[0];
        if (result.mimetype && mimeType.startsWith("image/")) {
          preview = $('<img />')
            .prop('loading', 'lazy')
            // Preserve the aspect ratio of the image while resizing to a 150x150px box
            .prop('src', `${downloadURL}?width=150&height=150&keepAspectRatio=true`)
            .prop('alt', filename);
        } else {
          const icon = attachmentsIcon.getIcon({
            mimeType: mimeType,
            name: filename
          });
          if (icon.iconSetType === 'FONT') {
            preview = $('<span />')
              .prop('class', icon.cssClass)
              .addClass('attachmentIcon');
          } else {
            preview = $('<img />').prop('src', icon.url);
          }
        }
        return preview;
      }
    }

    $.fn.attachmentGalleryPicker = function () {
      return this.each(function () {
        new AttachmentGalleryPicker($(this)).initialize();
      });
    };

    function init(_event, data) {
      const container = $((data && data.elements) || document);
      container.find('.attachmentGalleryPicker').attachmentGalleryPicker();
    }

    $(document).on('xwiki:dom:updated', init);
    $(init);
  });
