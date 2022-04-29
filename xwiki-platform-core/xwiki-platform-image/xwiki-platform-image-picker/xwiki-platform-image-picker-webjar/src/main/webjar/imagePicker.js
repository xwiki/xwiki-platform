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

define('xwiki-image-picker', ['jquery', 'blueimp-gallery'], function ($, gallery) {
  'use strict';

  const IMAGE_PICKER_INITIALIZED_CLASS = 'initialized';

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
      // TOOD: transform to a format that can be used to display the results in a "lightbox"
      // TODO: deal with the case where one of the query failed!
      return Promise.all([localDocumentOnly, globalSearch]).then(results => {
        /*
         * Take the images from the current document first.
         * Then take the images from the global search that are not from the current document (to avoid duplicates).
         * At the end, only keep a fixed number of images (this.nb).
         */
        const localImages = results[0];
        const localImagesId = localImages.map(image => image.id);
        const globalImages = results[1].filter(it => !localImagesId.includes(it.id));
        return localImages.concat(globalImages).slice(0, this.nb);
      });
    }

    searchSolr(input, options) {
      const fqs = (options || {}).fqs || [];
      const defaultFqs = ['type:ATTACHMENT', 'mimetype:image/*'].concat(fqs);
      const query = defaultFqs.map((fq) => 'fq=' + fq).join('\n');

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
      // TODO: make solr query
      // TODO: deal with slow query response time (i.e., what happen if we want to run a query while a first one is
      // running?)
      this.rootBlock.addClass('loading');
      return this.solrSearch.search(query).then(this.cb)
        .catch((error) => {
          console.log(error);
          // TODO: display error (to the end user)
        }).finally(() => {
          this.rootBlock.removeClass('loading');
        });
    }
  }

  /**
   * TODO: document me
   */
  class ImagePicker {
    constructor(imagePicker) {
      this.imagePicker = imagePicker;
      this.searchBlock = new SearchBlock(this.imagePicker, this.imagePicker.find('.imagePickerSearch'));
      this.resultsBlock = this.imagePicker.find('.imagePickerResults');
    }

    initialize() {
      if (!this.imagePicker.hasClass(IMAGE_PICKER_INITIALIZED_CLASS)) {
        this.searchBlock.initialize((results) => {
          // TODO: display results
          this.resultsBlock.empty();
          var index = 0;
          for (let result of results) {
            const attachmentReference = XWiki.Model.resolve(result.id, XWiki.EntityType.ATTACHMENT);
            const downloadDocumentURL = new XWiki.Document(attachmentReference.parent).getURL('download');
            const downloadURL = downloadDocumentURL + '/' + encodeURIComponent(attachmentReference.name);
            const filename = result.filename[0];
            const img = $(`<img />`)
              .prop('loading', 'lazy')
              .prop('width', 150)
              .prop('height', 150)
              .prop('src', downloadURL + "?width=150&height=150")
              .prop('alt', filename);
            const textSpan = $('<span>').text(filename).prop('title', filename);
            const link = $(`<a></a>`)
              .append(img)
              .prop('title', filename)
              .prop('href', downloadURL)
              .data('index', index)
              .append(img)
              .append('<br/>')
              .append(textSpan);
            this.resultsBlock.append($('<span class="imageGroup">').append(link));
            index = index + 1;
          }
          const images = this.resultsBlock.find('a');
          const imagePickerId = this.imagePicker.attr('id');
          images.on('click', function (e) {
            e.preventDefault();
            gallery(images, {
              container: `#${imagePickerId} .imagePickerCarousel`,
              index: parseInt($(this).data('index')),
            });
          });
        });
        this.imagePicker.addClass(IMAGE_PICKER_INITIALIZED_CLASS);
      }
    }
  }

  $.fn.imagePicker = function () {
    return this.each(function () {
      const imagePicker = new ImagePicker($(this));
      imagePicker.initialize();
    });
  };

  function init(_event, data) {
    var container = $((data && data.elements) || document);
    container.find('.imagePicker').imagePicker();
  }

  $(document).on('xwiki:dom:updated', init);
  $(init);
});