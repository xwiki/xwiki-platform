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

// This cache stores the metadata of the already resolved icons as well as the Promises for the icons currently being 
// asynchronously resolved.
// The goal of this cache is to only request for the resolution of an icon once per live data rendering.
const iconCache = {};

async function fetchRemoteIconDescriptor(iconName) {
  try {
    const parameters = `name=${encodeURIComponent(iconName)}`;
    const iconURL = `${XWiki.contextPath}/rest/wikis/${XWiki.currentWiki}/iconThemes/icons?${parameters}`;
    const response = await window.fetch(iconURL, {
      headers: {
        'Accept': 'application/json'
      }
    });
    const jsonResponse = await response.json();
    iconCache[iconName] = jsonResponse?.icons[0];
    return jsonResponse?.icons[0];
  } catch (err) {
    console.error(err);
  }
}

export default function fetch(iconName) {
  // If the icon was not already fetched, fetch it!
  if (!iconCache[iconName]) {
    // We set the iconCache value to the promise object, so that every other request of the same icon whether they
    // occur during the first request or after, will use the same promise and will not create another request.
    iconCache[iconName] = this.fetchRemoteIconDescriptor(iconName);
  }
  return iconCache[iconName];
}
