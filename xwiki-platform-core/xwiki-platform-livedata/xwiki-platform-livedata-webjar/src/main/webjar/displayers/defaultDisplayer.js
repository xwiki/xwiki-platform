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


define(["jquery"], function ($) {

  /**
   * Create a default displayer for a property of an entry
   * @param {String} propertyId The property id of the entry to display
   * @param {Object} entryData The entry to display the property of
   * @param {Object} logic The logic object associated to the livedata
   */
  var Displayer = function (propertyId, entryData, logic) {

    this.logic = logic;
    this.propertyId = propertyId;
    this.entryData = entryData;

    this.element = document.createElement("div");
    this.element.className = "livedata-displayer";

    this.createView();

  };

  /**
   * Return the object of parameters to be passed to the viewer and editor functions
   * @returns {Object}
   */
  Displayer.prototype._createParameters = function () {
    // find property descriptor sort level
    var propertyDescriptor = this.logic.getPropertyDescriptor(this.propertyId);
    // return the params object
    return {
      value: this.entryData[this.propertyId],
      property: propertyDescriptor,
      entry: this.entryData,
      config: propertyDescriptor.displayer || {},
      data: this.logic.data,
      logic: this.logic,
    };
  };



  Displayer.prototype.view = function (defer, params) {
    var element = document.createElement("div");
    if (params.value !== undefined || params.value !== null) {
      element.innerText = params.value;
    }
    return defer.resolve(element);
  };

  Displayer.prototype.edit = function (defer, params) {

  };



  Displayer.prototype.createView = function () {
    var self = this;
    var defer = $.Deferred();
    var params = this._createParameters();

    this.view(defer, params).done(function (element) {
      self.element.innerHTML = "";
      self.element.appendChild(element);
    });

  };



  return Displayer;

});
