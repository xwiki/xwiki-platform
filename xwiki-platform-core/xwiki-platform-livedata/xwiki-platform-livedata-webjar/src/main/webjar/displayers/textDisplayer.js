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
    BASE_PATH + "displayers/defaultDisplayer.js",
    "polyfills",
  ], function (
    DefaultDisplayer
  ) {


    /**
     * Create an text displayer for a property of an entry
     * Extends the default displayer class
     */
    var TextDisplayer = function (propertyId, entryData, logic) {
      DefaultDisplayer.call(this, propertyId, entryData, logic);
    };
    TextDisplayer.prototype = Object.create(DefaultDisplayer.prototype);
    TextDisplayer.prototype.constructor = TextDisplayer;


    /* This text displayer is very simple so it doesn't need to override default methods */


    return TextDisplayer;
  });