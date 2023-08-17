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

define('xwiki-iconService', [
  'jquery',
], function ($) {

  /**
   * Generate an URL for getting JSON resources about icons.
   */
  const getResourceURL = function (action, parameters) {
    const params = Object.assign({outputSyntax: "plain", action: action}, parameters);
    return (new XWiki.Document('IconPicker', 'IconThemesCode')).getURL('get', $.param(params), true);
  };

  let cachedIconThemes = false;
  const getIconThemes = function () {
    return new Promise(function (resolve, reject) {

      if (cachedIconThemes) {
        return resolve(cachedIconThemes);
      }

      $.getJSON(getResourceURL('data_iconthemes'), function (data) {
        cachedIconThemes = data;
        resolve(data);
      }).fail(reject);
    });
  };

  const cachedIcons = {};
  const getIcons = function (iconTheme) {
    return new Promise(function (resolve, reject) {

      if (cachedIcons[iconTheme]) {
        resolve(cachedIcons[iconTheme]);
      }

      $.getJSON(getResourceURL('data_icons', {iconTheme}), function (dataIcons) {
        cachedIcons[iconTheme] = dataIcons;
        resolve(dataIcons);
      }).fail(reject);
    });
  };

  return {
    getIconThemes,
    getIcons,
  };
});
