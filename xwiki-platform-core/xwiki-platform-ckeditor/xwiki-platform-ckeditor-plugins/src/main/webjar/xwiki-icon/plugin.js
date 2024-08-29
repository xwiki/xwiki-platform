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
(function () {
  'use strict';

  CKEDITOR.plugins.add('xwiki-icon', {
    init: function (editor) {
      editor.config.mentions = editor.config.mentions || [];
      // Icon Autocomplete
      editor.config.mentions.push({
        marker: `icon::`,
        pattern: new RegExp('icon::\\S{0,30}$'),
        minChars: 0,
        itemsLimit: 0,
        itemTemplate: `<li data-id="{id}">
            <div class="ckeditor-autocomplete-item-head">
              <span class="ckeditor-autocomplete-item-icon-wrapper">` +
                // We use aria-hidden="true" and role="presentation" for the icon display
                // because it is redundant with the label that is shown next to it.
                `<span class="{iconClass}" aria-hidden="true"></span>
                <img src="{imgSrc}" role="presentation"></img>
              </span>
              <span class="ckeditor-autocomplete-item-label">{label}</span>
            </div>
          </li>`,
        feed: function (opts, callback) {
          require(['xwiki-iconService'], function (iconService) {
            iconService.getIconThemes().then(function (iconThemes) {
              // Retreive the list of available icons.
              iconService.getIcons(iconThemes.currentIconTheme).then(function (icons) {
                callback(icons
                  .filter(icon => icon.name.toLowerCase().startsWith(opts.query.toLowerCase()))
                  .map(icon => ({
                    id: icon.name,
                    label: icon.name,
                    imgSrc: icon.metadata.url,
                    iconClass: icon.metadata.cssClass
                  }))
                );
              }).catch(function () {
              editor.showNotification(editor.localization.get('xwiki-icon.iconsFetchFailed'), 'warning', 5000);
            });
            }).catch(function () {
              editor.showNotification(editor.localization.get('xwiki-icon.iconThemesFetchFailed'), 'warning', 5000);
            });
          });
        },
        outputTemplate: function (item) {
          editor.execCommand('xwiki-macro-insert', {
            name: "displayIcon",
            parameters: {
              name: item.id,
            },
            // Displaying the macro inline allows to write text around the icon after insertion.
            inline: "enforce",
          });
          return "";
        }
      });
    },
  });
})();
