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
], function($) {
  /**
   * Generate an URL for getting JSON resources about icons.
   */
  function getResourceURL(action, parameters) {
    return new XWiki.Document('IconPicker', 'IconThemesCode').getURL('get', $.param({
      outputSyntax: "plain",
      action: action,
      ...parameters
    }, true));
  }

  let iconThemesPromise = false;
  function getIconThemes() {
    if (!iconThemesPromise) {
      iconThemesPromise = $.getJSON(getResourceURL('data_iconthemes'));
      iconThemesPromise.catch(() => {
        // Reset the promise so that we can try again later.
        iconThemesPromise = false;
      });
    }
    return iconThemesPromise;
  }

  const iconsPromises = {};
  function getIcons(iconTheme, query) {
    let iconsPromisesPerTheme = iconsPromises[iconTheme] = iconsPromises[iconTheme] || {};
    let iconsPromise = iconsPromisesPerTheme[query];
    if (!iconsPromise) {
      iconsPromise = iconsPromisesPerTheme[query] = $.getJSON(getResourceURL('data_icons', {
        iconTheme,
        query,
        metadata: true
      }));
      iconsPromise.catch(() => {
        // Reset the promise so that we can try again later.
        delete iconsPromisesPerTheme[query];
      });
    }
    return iconsPromise;
  }

  return {
    getIconThemes,
    getIcons,
  };
});
