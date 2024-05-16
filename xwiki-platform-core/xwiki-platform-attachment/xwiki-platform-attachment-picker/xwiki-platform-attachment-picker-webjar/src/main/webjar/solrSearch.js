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

define('xwiki-attachment-picker-solr-search', ['jquery'], function($) {
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
        // Forcing the acceptance of all locales, otherwise only the attachments of documents with the same locale
        // as the current document are returned.
        const computedFqs = ['type:ATTACHMENT', 'locale:*'].concat(optionsFqs).concat(typesFqs);
        const computedParams = [];

        computedParams.push(...computedFqs.map((fq) => 'fq=' + fq));

        if (options.sort) {
          computedParams.push('sort=' + options.sort);
        }

        const query = computedParams.join('\n');

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

    return SolrSearch;
});
