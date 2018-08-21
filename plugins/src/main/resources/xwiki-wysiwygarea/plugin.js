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
(function() {
  'use strict';

  CKEDITOR.plugins.add('xwiki-wysiwygarea', {
    requires: 'wysiwygarea',

    init: function(editor) {
      // The plugins load additional CSS by calling editor.addContentsCss() which collects the style sheets in the
      // editor.config.contentsCss configuration property. Unfortunately this property is ignored when fullPage is on.
      // We need full page, because we want to load all the XWiki styles, and we also need the plugins styles so the
      // following code fixes this.
      if (editor.config.fullPage) {
        this.injectPluginStyles(editor);
      }
    },

    // Inject the plugins styles into the editing area.
    injectPluginStyles: function(editor) {
      // See https://ckeditor.com/docs/ckeditor4/latest/api/CKEDITOR_config.html#cfg-contentsCss
      // See https://ckeditor.com/docs/ckeditor4/latest/api/CKEDITOR_editor.html#event-toHtml
      editor.on('toHtml', function(event) {
        if (!CKEDITOR.tools.isArray(editor.config.contentsCss)) {
          // No styles to inject.
          return;
        }
        // CKEDITOR.htmlParser.fragment instance
        var documentFragment = event.data.dataValue;
        if (typeof documentFragment.find !== 'function') {
          // The root node doesn't have support for finding child elements.
          return;
        }
        var heads = documentFragment.find('head', true);
        if (heads.length === 0) {
          // No place to inject the styles.
          return;
        }
        var head = heads[0];
        editor.config.contentsCss.forEach(function(url) {
          // We want to inject only the styles required by the loaded plugins.
          if (url.indexOf('/plugins/') > 0) {
            head.children.push(new CKEDITOR.htmlParser.element('link', {
              rel: 'stylesheet',
              type: 'text/css',
              href: url
            }));
          }
        });
      }, null, null, 14);
    }
  });
})();
