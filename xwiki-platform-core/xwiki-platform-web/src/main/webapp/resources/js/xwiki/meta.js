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
define(['jquery'], function($) {
  // Note: Starting with XWiki 7.2M3, the returned "document", "xwiki", "space" and "page" variables are deprecated
  // and it's recommended to use the new "reference" variable, which holds the full String reference of the current
  // document.
  // In addition starting with XWiki 7.2M1, the "space" variable now holds the full space reference (i.e. one or
  // several spaces separated by dots, e.g. "space1.space2").
  var html = $('html');
  // Case 1: meta information are stored in the data- attributes of the <html> tag
  // (since Flamingo)
  if (html.data('xwiki-reference') !== undefined) {
    var documentReference = XWiki.Model.resolve(html.data('xwiki-reference'), XWiki.EntityType.DOCUMENT);
    var wikiReference     = documentReference.extractReference(XWiki.EntityType.WIKI);
    var spaceReference    = documentReference.extractReference(XWiki.EntityType.SPACE);
    return {
      'documentReference': documentReference,
       // deprecated, use 'documentReference' instead
      'document':          XWiki.Model.serialize(documentReference.relativeTo(wikiReference)),
       // deprecated, use 'documentReference' instead
      'wiki':              wikiReference.getName(),
       // deprecated, use 'documentReference' instead
      'space':             XWiki.Model.serialize(spaceReference.relativeTo(wikiReference)),
       // deprecated, use 'documentReference' instead
      'page':              documentReference.getName(),
      'version':           html.data('xwiki-version'),
      'restURL':           html.data('xwiki-rest-url'),
      'form_token':        html.data('xwiki-form-token'),
      // Since 10.4RC1
      'userReference':     XWiki.Model.resolve(html.data('xwiki-user-reference'), XWiki.EntityType.DOCUMENT)
    };
  }
  // Case 2: meta information are stored in deprecated <meta> tags
  // (in colibri)
  var metaTags = $('meta');
  var lookingFor = ['document', 'wiki', 'space', 'page', 'version', 'restURL', 'form_token'];
  var results = {}
  for (var i = 0; i < metaTags.length; ++i) {
    var metaTag = $(metaTags[i]);
    var name = metaTag.attr('name');
    for (var j = 0; j < lookingFor.length; ++j) {
      if (name == lookingFor[j]) {
        results[name] = metaTag.attr('content');
      }
    }
  }
  return results;
});
