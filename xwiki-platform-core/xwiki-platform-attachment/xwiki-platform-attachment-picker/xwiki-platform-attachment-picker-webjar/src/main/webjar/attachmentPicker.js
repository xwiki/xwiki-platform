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

/*define('xwiki-attachment-picker-icons', ['jquery'], function ($) {
  'use strict';

  const iconCache = {};

  function getIcon(iconName) {
    return new Promise((resolve, reject) => {
      if (iconCache[iconName]) {
        resolve(iconCache[iconName]);
      } else {
        const parameters = `name=${encodeURIComponent(iconName)}`;
        const iconURL = `${XWiki.contextPath}/rest/wikis/${XWiki.currentWiki}/iconThemes/icons?${parameters}`;
        $.getJSON(iconURL).done((response) => {
          iconCache[iconName] = response.icons[0];
          const icon = response.icons[0];
          resolve(icon);
        }).fail((error) => {
          reject(error);
        });
      }
    });
  } 

  function renderIcon(iconDescriptor) {
    if (iconDescriptor.iconSetType === 'IMAGE') {
      return $('<img/>').prop("src", iconDescriptor.url);
    } else {
      return $('<span/>').addClass(iconDescriptor.cssClass);
    }
  }

  return {
    getIcon: getIcon,
    renderIcon: renderIcon
  };
})*/
// ;

define('xwiki-attachment-picker', ['jquery', 'blueimp-gallery', 'xwiki-lightbox-description'],
  function ($, gallery, lightboxDescription, iconsClient) {
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
      constructor() {
        const doc = new XWiki.Document(XWiki.Model.resolve('XWiki.SuggestSolrService', XWiki.EntityType.DOCUMENT));
        this.sugestSolrServiceURL = doc.getURL('get');
        // TODO: make this configurable.
        this.nb = 20;
      }

      search(query) {
        const localDocumentOnly = this.searchSolr(query, {
          fqs: [
            `wiki:${XWiki.currentWiki}`,
            `space:${XWiki.currentSpace}`,
            `name:${XWiki.currentPage}`
          ]
        });
        const globalSearch = this.searchSolr(query);
        // TODO: deal with the case where one of the query failed!
        return Promise.all([localDocumentOnly, globalSearch]).then(results => {
          /*
           * Take the attachments from the current document first. Then take the attachments from the global search that
           *  are not from the current document (to avoid duplicates). At the end, only keep a fixed number of 
           * attachments (this.nb).
           */
          const localAttachments = results[0];
          const localAttachmentsId = localAttachments.map(attachment => attachment.id);
          const globalAttachments = results[1].filter(it => !localAttachmentsId.includes(it.id));
          return localAttachments.concat(globalAttachments).slice(0, this.nb);
        });
      }

      searchSolr(input, options) {
        console.log('searchSolr');
        options = options || {};
        const optionsFqs = options.fqs || [];
        const types = options.types;
        var typesFqs;
        if (types !== undefined && types !== '') {
          typesFqs = ['mimetype:(' + types.split(",").join(" OR ") + ')'];
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
            'nb': this.nb,
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
        this.solrSearch = new SolrSearch();
        this.cb = cb;
        this.search('').finally(() => {
          this.searchBlock.find('input').on('input', debounce((event) => {
            this.search(event.target.value);
          }, 200));
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
            // TODO: display results
            this.resultsBlock.empty();
            this.noResultsBlock.addClass('hidden');
            if (results.length > 0) {
              this.initializeWhenHasResults(results);
            } else {
              this.noResultsBlock.removeClass('hidden');
            }
          });
          this.attachmentPicker.addClass(ATTACHMENT_PICKER_INITIALIZED_CLASS);
        }
      }

      initializeWhenHasResults(results) {
        var index = 0;
        const attachmentPickerId = this.attachmentPicker.attr('id');
        for (let result of results) {
          this.addAttachment(result, index);
          index = index + 1;
        }
        const attachments = this.resultsBlock.find('a');

        const galleryId = `#${attachmentPickerId}-gallery`;
        const lightboxOptions = {rootSelector: galleryId};

        $(document).on('click', lightboxOptions.rootSelector + ' .slides',
          () => lightboxDescription.toggleDescription(lightboxOptions));

        attachments.on('click', function (e) {
          const slideParams = {
            href: $(this).attr('href'),
            // thumbnail: createThumbnailURL(attachmentURL),
            // caption: caption,
            fileName: $(this).attr('title'),
            alt: $(this).attr('alt'),
            title: $(this).attr('title'),
            // id: getAttachmentId(this)
          };

          lightboxDescription.addSlideDescription(slideParams, lightboxOptions);
          e.preventDefault();
          gallery(attachments, {
            container: galleryId,
            index: parseInt($(this).data('index')),
          });
        });
      }

      addAttachment(result, index) {
        const attachmentReference = XWiki.Model.resolve(result.id, XWiki.EntityType.ATTACHMENT);
        const downloadDocumentURL = new XWiki.Document(attachmentReference.parent).getURL('download');
        const filename = result.filename[0];
        var preview;
        var downloadURL = `${downloadDocumentURL}/${encodeURIComponent(attachmentReference.name)}`;
        if (result.mimetype && result.mimetype[0].startsWith("image/")) {
          preview = $(`<img />`)
            .prop('loading', 'lazy')
            .prop('width', 150)
            .prop('height', 150)
            .prop('src', `${downloadURL}?width=150&height=150`)
            .prop('alt', filename);
        } else {
          console.log("HERE");
          iconsClient.getIcon({});
          preview = $("<img/>");
        }
        console.log('HERE');

        const textSpan = $('<span>').text(filename).prop('title', filename);
        const link = $(`<a></a>`)
          .append(preview)
          .prop('title', filename)
          .prop('href', downloadURL)
          .data('index', index)
          .append(preview)
          .append('<br/>')
          .append(textSpan);
        this.resultsBlock.append($('<span class="attachmentGroup">')
          .append($('<div/>').append(link)));
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