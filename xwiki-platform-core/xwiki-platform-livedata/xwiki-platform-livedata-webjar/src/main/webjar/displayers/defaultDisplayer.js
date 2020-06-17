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
  "jquery",
], function ($) {

  /**
   * Load the displayer custom css to the page
   */
  (function loadCss(url) {
    var link = document.createElement("link");
    link.type = "text/css";
    link.rel = "stylesheet";
    link.href = url;
    document.getElementsByTagName("head")[0].appendChild(link);
  })(BASE_PATH + "displayers/displayers.css");


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

    this.view();

  };


  /**
   * Return the object of parameters to be passed to the viewer and editor functions
   * @returns {Object} an object containing useful data for displayer: {
   *  value: the entry property value to be displayed
   *  property: the property descriptor object
   *  entry: the entry data object
   *  config: the configuration object of the displayer, found in the propertyDescriptor
   *  data: the livedata data object
   *  logic: the logic instance
   * }
   */
  Displayer.prototype._createParameters = function () {
    var propertyDescriptor = this.logic.getPropertyDescriptor(this.propertyId);
    // return the param object
    return {
      value: this.entryData[this.propertyId],
      property: propertyDescriptor,
      entry: this.entryData,
      config: propertyDescriptor.displayer || {},
      data: this.logic.data,
      logic: this.logic,
    };
  };



  /**
   * Create viewer element for the displayer
   * This method can be overriden by other displayers that inherit from this one
   * Parameters are given by the Displayer.prototype.view function
   * Must resolve the given promise at the end
   * @param {Object} defer A jquery promise that must be resolved when the viewer has been created
   * @param {object} params An object containing useful data for the displayer.
   *  Param object detail can be found in the Displayer.prototype._createParameters method
   */
  Displayer.prototype.createView = function (defer, params) {
    var element = document.createElement("div");
    if (params.value !== undefined && params.value !== null) {
      element.innerText = params.value;
    }
    defer.resolve(element);
  };



  /**
   * Create editor element for the displayer
   * This method can be overriden by other displayers that inherit from this one
   * Parameters are given by the Displayer.prototype.edit function
   * Must resolve the given promise at the end
   * @param {Object} defer A jquery promise that must be resolved when the editor has been created
   * @param {object} params An object containing useful data for the displayer.
   *  Param object detail can be found in the Displayer.prototype._createParameters method
   */
  Displayer.prototype.createEdit = function (defer, params) {

  };


  /**
   * Call this.createView and append viewer to the displayer root element
   */
  Displayer.prototype.view = function () {
    var self = this;
    var defer = $.Deferred();
    var params = this._createParameters();

    this.createView(defer, params);
    defer.done(function (element) {
      self.element.innerHTML = "";
      self.element.appendChild(element);
    });

  };

  /**
   * Call this.createEdit and append editor to the displayer root element
   */
  Displayer.prototype.edit = function () {

  };



  return Displayer;

});
