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
require.config({
  config: {
    'xwiki-icon': {
      url: `${XWiki.contextPath}/rest/wikis/${encodeURIComponent(XWiki.currentWiki)}/iconThemes/icons`
    }
  }
});

define('xwiki-icon', ['module'], function(module) {
  'use strict';

  async function getIcons(specs) {
    const params = new URLSearchParams();
    specs.icons.forEach(icon => params.append('name', icon));

    const response = await fetch(`${module.config().url}?${params}`,{
      headers: {
        'Accept': 'application/json'
      }
    });

    if (!response.ok) {
      throw new Error(`Response status: ${response.status}`);
    }

    const result = await response.json();
    const icons = {};
    result.icons.forEach(icon => {
      icons[icon.name] = icon;
      icon.render = function() {
        let element = document.createElement(icon.url ? 'img' : 'span');
        if (icon.url) {
          element.src = icon.url;
          element.alt = '';
          element.dataset.xwikiLightbox = 'false';
        } else {
          element.className = icon.cssClass;
          element.setAttribute('aria-hidden', 'true');
        }
        element.classList.add('icon');
        return element;
      };
    });
    return icons;
  }

  function load(name, parentRequire, onLoad, config) {
    parentRequire([name], function(specs) {
      if (module.config().url) {
        getIcons(specs).then(onLoad).catch(error => {
          console.error('Failed to load icons: ', error);
          onLoad({});
        });
      } else {
        console.error('The URL to get the icons is not configured.');
        onLoad({});
      }
    });
  }

  return {load};
});
