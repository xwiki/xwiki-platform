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
  ['jquery', 'xwiki-attachments-icon', 'xwiki-l10n!xwiki-attachment-picker-translation-keys'],
  function ($, attachmentsIcon, translations) {
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
     * Handle the interaction with the solr search endpoint.
     */
    class SolrSearch {
      constructor(options) {
        const doc = new XWiki.Document(XWiki.Model.resolve('XWiki.SuggestSolrService', XWiki.EntityType.DOCUMENT));
        this.sugestSolrServiceURL = doc.getURL('get');
        this.options = options || {};
        this.limit = options.limit || 20;
        this.target = options.target;
        this.solrOptions = this.options.solrOptions || {};
      }

      search(query, isGlobal) {
        const {currentWiki, currentSpace, currentPage} = this.resolveTargetFqs();
        const localDocumentOnly = this.searchSolr(query, Object.assign({}, this.solrOptions, {
          fqs: [
            `wiki:${currentWiki}`,
            `space:"${currentSpace}"`,
            `name:"${currentPage}"`
          ]
        }));

        // TODO: Make this algorithm configurable through an macro parameter.
        let values;
        if (isGlobal) {
          const globalSearch = this.searchSolr(query, this.solrOptions);
          values = [localDocumentOnly, globalSearch];
        } else {
          values = [localDocumentOnly];
        }
        return Promise.all(values).then(results => {
          /*
           * Take the attachments from the current document first. Then take the attachments from the global search that
           *  are not from the current document (to avoid duplicates). At the end, only keep a fixed number of 
           * attachments (this.nb).
           */
          const localAttachments = results[0];
          localAttachments.forEach(attachment => attachment.isLocal = true);
          const localAttachmentsId = localAttachments.map(attachment => attachment.id);

          // Check if global attachments where request before trying to merge them while removing the duplicates.
          let globalAttachments = [];
          if (results[1]) {
            globalAttachments = results[1].filter(it => !localAttachmentsId.includes(it.id));
          }
          globalAttachments.forEach(attachment => attachment.isLocal = false);
          return localAttachments.concat(globalAttachments).slice(0, this.limit);
        });
      }

      resolveTargetFqs() {
        let currentWiki = XWiki.currentWiki;
        let currentSpace = XWiki.currentSpace;
        let currentPage = XWiki.currentPage;
        if (this.target) {
          const target = new XWiki.Document(XWiki.Model.resolve(this.target, XWiki.EntityType.DOCUMENT));
          if (target.wiki) {
            currentWiki = target.wiki;
          }
          currentSpace = target.space;
          currentPage = target.page;
        }
        return {currentWiki, currentSpace, currentPage};
      }

      searchSolr(input, options) {
        options = options || {};
        const optionsFqs = options.fqs || [];
        const types = options.filter;
        var typesFqs;
        if (types !== undefined && types !== '') {
          typesFqs = ['mimetype:(' + types.split(",").map(type => "(" + type + ")").join(" OR ") + ')'];
        } else {
          typesFqs = [];
        }
        const computedFqs = ['type:ATTACHMENT', 'locale:*'].concat(optionsFqs).concat(typesFqs);
        const query = computedFqs.map((fq) => 'fq=' + fq).join('\n');

        return new Promise((resolve, reject) => {
          // TODO: handle more kind of scopes
          if (input === '') {
            // Replace the empty string with '*', matching everything. 
            input = '*';
          }
          $.getJSON(this.sugestSolrServiceURL, {
            'query': query,
            'nb': this.limit,
            'media': 'json',
            'input': input
          })
            .done(function (response) {
              resolve(response);
            })
            .fail(function (_jqxhr, textStatus, error) {
              reject(`${textStatus} ${error}`);
            });

        });
      }
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
        const downloadDocumentURL = new XWiki.Document(attachmentReference.parent).getURL('download');
        const filename = result.filename[0];
        const downloadURL = `${downloadDocumentURL}/${encodeURIComponent(attachmentReference.name)}`;
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
            .prop('src', `${downloadURL}?width=150&height=150`)
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
