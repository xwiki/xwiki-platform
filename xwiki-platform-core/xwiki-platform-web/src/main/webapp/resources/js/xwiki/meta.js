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
  var html = $('html');
  // Case 1: meta information are stored in the data- attributes of the <html> tag
  // (since Flamingo)
  if (html.data('xwiki-document') !== undefined) {
    return {
      'document':   html.data('xwiki-document'),
      'wiki':       html.data('xwiki-wiki'),
      'space':      html.data('xwiki-space'),
      'page':       html.data('xwiki-page'),
      'version':    html.data('xwiki-version'),
      'restURL':    html.data('xwiki-rest-url'),
      'form_token': html.data('xwiki-form-token')
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
