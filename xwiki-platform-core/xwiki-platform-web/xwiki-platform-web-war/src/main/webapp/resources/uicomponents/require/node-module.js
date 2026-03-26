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
 * RequireJS plugin that can be used to load modules defined using the CommonJS API.
 *
 * Usage example:
 *
 * require(['node-module!fast-diff'], function(diff) {
 *   const changes = diff(oldText, newText);
 * });
 *
 * @since 16.10.16
 * @since 17.4.8
 * @since 17.10.1
 */
define('node-module', ['jquery'], function($) {
  return {
    load: function(name, req, onLoad, config) {
      $.get(req.toUrl(name + '.js'), function(text) {
        onLoad.fromText(`define(function(require, exports, module) {${text}});`);
      }, 'text');
    }
  }
});