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

define('xwiki-attachment-picker', ['jquery', 'xwiki-attachments-icon'], function ($, attachmentsIcon) {
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

  class SolrSearch {
    constructor(options) {
      const doc = new XWiki.Document(XWiki.Model.resolve('XWiki.SuggestSolrService', XWiki.EntityType.DOCUMENT));
      this.sugestSolrServiceURL = doc.getURL('get');
      // TODO: make this configurable.
      this.options = options || {};
      this.limit = options.limit || 20;
      this.solrOptions = this.options.solrOptions || {};
    }

    search(query) {
      const localDocumentOnly = this.searchSolr(query, Object.assign({}, this.solrOptions, {
        fqs: [
          `wiki:${XWiki.currentWiki}`,
          `space:${XWiki.currentSpace}`,
          `name:${XWiki.currentPage}`
        ]
      }));
      const globalSearch = this.searchSolr(query, this.solrOptions);
      return Promise.all([localDocumentOnly, globalSearch]).then(results => {
        /*
         * Take the attachments from the current document first. Then take the attachments from the global search that
         *  are not from the current document (to avoid duplicates). At the end, only keep a fixed number of 
         * attachments (this.nb).
         */
        const localAttachments = results[0];
        const localAttachmentsId = localAttachments.map(attachment => attachment.id);
        const globalAttachments = results[1].filter(it => !localAttachmentsId.includes(it.id));
        return localAttachments.concat(globalAttachments).slice(0, this.limit);
      });
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
      const computedFqs = ['type:ATTACHMENT'].concat(optionsFqs).concat(typesFqs);
      const query = computedFqs.map((fq) => 'fq=' + fq).join('\n');

      return new Promise((resolve, reject) => {
        // TODO: handle more kind of scopes
        if (input === '') {
          // Replace the empty string with  
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
          .fail(function (error) {
            reject(error);
          });

      });
    }
  }

  /**
   * TODO: doc...
   */
  class SearchBlock {
    constructor(rootBlock, searchBlock) {
      this.rootBlock = rootBlock;
      this.searchBlock = searchBlock;
    }

    initialize(cb) {
      this.searchBlock.addClass('xform');
      this.searchBlock.html("<input type='text' />");
      this.solrSearch = new SolrSearch({
        solrOptions: {
          filter: this.rootBlock.data('xwiki-attachment-picker-filter')
        }
      });
      this.cb = cb;
      this.search('').finally(() => {
        this.searchBlock.find('input').on('input', debounce((event) => {
          this.search(event.target.value);
        }, 500));
      });
    }

    search(query) {
      this.rootBlock.addClass('loading');
      return this.solrSearch.search(query).then(this.cb)
        .catch((error) => {
          console.log(error);
          // TODO: localization
          new XWiki.widgets.Notification('The attachments search query failed', 'error');
        }).finally(() => {
          this.rootBlock.removeClass('loading');
        });
    }
  }

  /**
   * TODO: document me
   */
  class AttachmentPicker {

    constructor(attachmentPicker) {
      this.attachmentPicker = attachmentPicker;
      const attachmentPickerSearch = this.attachmentPicker.find('.attachmentPickerSearch');
      this.searchBlock = new SearchBlock(this.attachmentPicker, attachmentPickerSearch);
      this.resultsBlock = this.attachmentPicker.find('.attachmentPickerResults');
      this.noResultsBlock = this.attachmentPicker.find('.attachmentPickerNoResults');
    }

    initialize() {
      if (!this.attachmentPicker.hasClass(ATTACHMENT_PICKER_INITIALIZED_CLASS)) {
        this.searchBlock.initialize((results) => {
          this.resultsBlock.empty();
          this.noResultsBlock.addClass('hidden');
          if (results.length > 0) {
            this.initializeWhenHasResults(results);
          } else {
            this.noResultsBlock.removeClass('hidden');
          }
          if (this.resultsBlock.find('selected').length === 0 && this.selected !== undefined) {
            this.unselect();
          }
        });
        this.attachmentPicker.addClass(ATTACHMENT_PICKER_INITIALIZED_CLASS);
      }
    }

    initializeWhenHasResults(results) {
      var index = 0;

      for (let result of results) {
        this.addAttachment(result, index);
        index = index + 1;
      }

      // const attachmentPickerId = this.attachmentPicker.attr('id');
      // const galleryId = `#${attachmentPickerId}-gallery`;
      // const lightboxOptions = {rootSelector: galleryId};

      this.initSelectedAttachment();
      // $(document).on('click', lightboxOptions.rootSelector + ' .slides',
      //   () => lightboxDescription.toggleDescription(lightboxOptions));

      // attachments.on('click', function (e) {
      //   const slideParams = {
      //     href: $(this).attr('href'),
      //     // thumbnail: createThumbnailURL(attachmentURL),
      //     // caption: caption,
      //     fileName: $(this).attr('title'),
      //     alt: $(this).attr('alt'),
      //     title: $(this).attr('title'),
      //     // id: getAttachmentId(this)
      //   };
      //
      //   lightboxDescription.addSlideDescription(slideParams, lightboxOptions);
      //   e.preventDefault();
      //   gallery(attachments, {
      //     container: galleryId,
      //     index: parseInt($(this).data('index')),
      //   });
      // });
    }

    initSelectedAttachment() {
      const attachments = this.resultsBlock.find('a');
      attachments.on('click', (event) => {
        event.preventDefault();
        const parent = $(event.currentTarget).parents('.attachmentGroup');

        if (this.selected !== undefined && parent.data('id') === this.selected) {
          this.unselect(parent);
        } else {
          this.resultsBlock.find('.attachmentGroup').removeClass('selected');
          parent.addClass('selected');
          this.selected = parent.data('id');
          this.attachmentPicker.trigger('xwiki:attachmentPicker:selected', this.selected);
        }
      });
    }

    unselect(parent) {
      this.selected = undefined;
      if (parent) {
        parent.removeClass('selected');
      }
      this.attachmentPicker.trigger('xwiki:attachmentPicker:unselected');
    }

    addAttachment(result, index) {
      const attachmentReference = XWiki.Model.resolve(result.id, XWiki.EntityType.ATTACHMENT);
      const downloadDocumentURL = new XWiki.Document(attachmentReference.parent).getURL('download');
      const filename = result.filename[0];
      var preview;
      var downloadURL = `${downloadDocumentURL}/${encodeURIComponent(attachmentReference.name)}`;
      const mimeType = result.mimetype[0];
      if (result.mimetype && mimeType.startsWith("image/")) {
        preview = $(`<img />`)
          .prop('loading', 'lazy')
          .prop('src', `${downloadURL}?width=150&height=150`)
          .prop('alt', filename);
      } else {
        const icon = attachmentsIcon.getIcon({
          mimeType: mimeType,
          name: filename
        });
        if (icon.iconSetType === 'FONT') {
          preview = $('<span />').prop('class', icon.cssClass + ' attachmentIcon');
        } else {
          preview = $('<img />').prop('src', icon.url);
        }

      }

      const textSpan = $('<span>').text(filename).prop('title', filename).addClass('attachmentTitle');
      const link = $(`<a></a>`)
        .prop('title', filename)
        .prop('href', downloadURL)
        .data('index', index)
        .append($('<span>').append(preview).addClass('previewWrapper'))
        .append('<br/>')
        .append(textSpan);

      const attachmentGroup = $('<span class="attachmentGroup">')
        .append($('<div/>').append(link))
        .data('id', result.id);
      if (this.selected !== undefined && result.id === this.selected) {
        attachmentGroup.addClass('selected');
      }
      this.resultsBlock.append(attachmentGroup);
    }
  }

  $.fn.attachmentPicker = function () {
    return this.each(function () {
      const attachmentPicker = new AttachmentPicker($(this));
      attachmentPicker.initialize();
    });
  };

  function init(_event, data) {
    var container = $((data && data.elements) || document);
    container.find('.attachmentPicker').attachmentPicker();
  }

  $(document).on('xwiki:dom:updated', init);
  $(init);
});