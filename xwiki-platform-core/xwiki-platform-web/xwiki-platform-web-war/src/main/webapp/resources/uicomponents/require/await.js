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
 * RequireJS plugin that waits for the value of a module that is a promise, i.e. for modules whose initialization is
 * asynchronous. RequireJS doesn't accept async functions as module factories (they are silently ignored), but a module
 * factory can return a promise, which this plugin resolves.
 *
 * Usage example:
 *
 * define('my-module-promise', ['dependency'], function(dependency) {
 *   return dependency.loadSomething().then(something => createModule(something));
 * });
 *
 * define('my-module', ['await!my-module-promise'], function(myModule) {
 *   // Use the module, which is fully initialized.
 *   return myModule;
 * });
 *
 * The module value is passed as is when it's not a promise. A rejected promise fails the module loading.
 *
 * @since 18.9.0RC1
 */
define('await', [], function() {
  'use strict';

  return {
    load: function(name, parentRequire, onLoad) {
      parentRequire([name], function(value) {
        Promise.resolve(value).then(onLoad, onLoad.error);
      }, onLoad.error);
    }
  };
});
