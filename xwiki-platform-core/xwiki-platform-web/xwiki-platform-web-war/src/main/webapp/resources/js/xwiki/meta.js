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
define(['jquery', 'xwiki-entityReference', 'xwiki-events-bridge'], function($, XWiki) {
  class XWikiMeta {
    constructor() {
      this.init();
    }

    // We keep the init function for backward compatibility (otherwise we would have moved the code in the constructor).
    init() {
      // Note: Starting with XWiki 7.2M3, the returned "document", "xwiki", "space" and "page" variables are deprecated
      // and it's recommended to use the new "reference" variable, which holds the full String reference of the current
      // document.
      // In addition starting with XWiki 7.2M1, the "space" variable now holds the full space reference (i.e. one or
      // several spaces separated by dots, e.g. "space1.space2").
      const html = document.documentElement;
      // Case 1: meta information are stored in the data- attributes of the <html> tag
      // (since Flamingo)
      if (html.dataset.xwikiReference) {
        this.documentReference = XWiki.Model.resolve(html.dataset.xwikiReference, XWiki.EntityType.DOCUMENT);
        const wikiReference = this.documentReference.extractReference(XWiki.EntityType.WIKI);
        const spaceReference = this.documentReference.extractReference(XWiki.EntityType.SPACE);
        // deprecated, use 'documentReference' instead
        this.document = XWiki.Model.serialize(this.documentReference.relativeTo(wikiReference));
        // deprecated, use 'documentReference' instead
        this.wiki = wikiReference.getName();
        // deprecated, use 'documentReference' instead
        this.space = XWiki.Model.serialize(spaceReference.relativeTo(wikiReference));
        this.page = this.documentReference.getName();
        this.version = html.dataset.xwikiVersion;
        this.restURL = html.dataset.xwikiRestUrl;
        this.form_token = html.dataset.xwikiFormToken;
        // Since 10.4RC1
        // For the guest user we set userReference to null
        const userReferenceString = html.dataset.xwikiUserReference;
        if (userReferenceString) {
          this.userReference = XWiki.Model.resolve(userReferenceString, XWiki.EntityType.DOCUMENT);
        } else {
          this.userReference = null;
        }
        // Since 11.2RC1
        this.isNew = html.dataset.xwikiIsnew === 'true';
        // Since 12.3RC1
        // Note that the 'data-xwiki-locale' attribute is set since XWiki 10.4RC1 but it hasn't been exposed here.
        this.locale = html.dataset.xwikiLocale;
      } else {
        // Case 2: meta information are stored in deprecated <meta> tags
        // (in colibri)
        const lookingFor = ['document', 'wiki', 'space', 'page', 'version', 'restURL', 'form_token']
        document.querySelectorAll('meta').forEach(metaTag => {
          if (lookingFor.includes(metaTag.name)) {
            this[metaTag.name] = metaTag.content;
          }
        });
      }
    }

    setVersion(newVersion) {
      this.version = newVersion;
      $(document).trigger('xwiki:document:changeVersion', {
        'version': this.version,
        'documentReference': this.documentReference
      });
    }

    /**
     * Refresh the version of a document from a REST endpoint. It fires a xwiki:document:changeVersion event.
     * In case of 404 this certainly means that the document is new.
     *
     * @param handle404 function to choose how to handle when the document is new
     */
    async refreshVersion(handle404) {
      // We put a timestamp in the JSON URL to avoid getting it from cache.
      const pageInfoUrl = this.restURL + "?media=json&timestamp=" + new Date().getTime();
      try {
        const response = await fetch(pageInfoUrl);
        if (!response.ok) {
          if (response.status === 404 && typeof(handle404) === "function") {
            handle404(response);
          } else {
            throw new Error(`Response status: ${response.status}`);
          }
        }
    
        const json = await response.json();
        this.setVersion(json.version);
      } catch (error) {
        console.error("Error while refreshing the version from URL " + pageInfoUrl, error);
      }
    }
  }

  return new XWikiMeta();
});
