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
requirejs.config({
  config: {
    'diff-dom': {
      // diffDOM is using Object.entries() but we still have to support IE11 so we need the polyfill.
      polyfills: ['es-object-entries']
    }
  },
  map: {
    '*': {
      // Make sure we load the required polyfills before diffDOM.
      'diff-dom': 'xwiki-withPolyfills!diff-dom',

      // Replace wgxpath with wgxpath-auto in order to polyfill XPath support automatically.
      'wgxpath': 'wgxpath-auto'
    },

    'diff-dom-withoutPolyfills': {'diff-dom': 'diff-dom'},
    'wgxpath-auto': {'wgxpath': 'wgxpath'}
  }
});

define('diff-dom-withoutPolyfills', ['diff-dom'], function(diffDOM) {
  return diffDOM;
});

// Auto polyfill the XPath API (missing in IE11).
define('wgxpath-auto', ['wgxpath'], function(wgxpath) {
  wgxpath.install();
  return wgxpath;
});

define('es-object-entries', function() {
  // See https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Object/entries#polyfill
  if (!Object.entries) {
    Object.entries = function(obj) {
      var ownProps = Object.keys(obj),
        i = ownProps.length,
        resArray = new Array(i); // preallocate the Array
      while (i--) {
        resArray[i] = [ownProps[i], obj[ownProps[i]]];
      }

      return resArray;
    };
  }
});
