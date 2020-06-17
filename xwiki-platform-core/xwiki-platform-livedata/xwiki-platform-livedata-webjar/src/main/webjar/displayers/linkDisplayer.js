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

define([
    BASE_PATH + "displayers/defaultDisplayer.js"
  ], function (
    DefaultDisplayer
  ) {

    // Extend Default Displayer
    var LinkDisplayer = function (propertyId, entryData, logic) {
      DefaultDisplayer.call(this, propertyId, entryData, logic);
    };
    LinkDisplayer.prototype = Object.create(DefaultDisplayer.prototype);
    LinkDisplayer.prototype.constructor = LinkDisplayer;


    LinkDisplayer.prototype.createView = function (defer, params) {
      // default href
      var propertyHref = params.config.propertyHref && params.entry[params.config.propertyHref];
      var href = propertyHref || params.config.href;
      // default text
      var text = params.value || href;
      // create element
      var element = document.createElement("a");
      if (text !== undefined && text !== null) {
        element.href = href || "#";
        element.innerHTML = text;
      }
      return defer.resolve(element);

    };

    return LinkDisplayer;
  });