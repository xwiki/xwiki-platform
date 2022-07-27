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

/**
 * Deferred dependency loading for RequireJS.
 *
 * Usage example:
 *
 * require(['deferred!module'], function(modulePromise) {
 *   modulePromise.then(module => {
 *     // Use the module.
 *   });
 * });
 *
 * @since 8.1M1
 */
define('deferred', ['jquery'], function($) {
  var promises = {};

  // Handle the modules that are not loaded yet.
  // See https://github.com/requirejs/requirejs/wiki/Internal-API:-onResourceLoad
  var oldOnResourceLoad = require.onResourceLoad;
  require.onResourceLoad = function(context, map, depArray) {
    if (typeof oldOnResourceLoad === 'function') {
      oldOnResourceLoad.apply(this, arguments);
    }
    var id = map.fullName || map.id;
    var promise = promises[id];
    if (promise) {
      promise.resolve(require(id));
    }
  };

  return {
    load: function (name, parentRequire, onLoad, config) {
      var promise = promises[name];
      if (!promise) {
        promises[name] = promise = $.Deferred();
      }
      // Check if the module is already loaded.
      if (parentRequire.defined(name)) {
        promise.resolve(parentRequire(name));
      }
      onLoad(promise.promise());
    }
  };
});