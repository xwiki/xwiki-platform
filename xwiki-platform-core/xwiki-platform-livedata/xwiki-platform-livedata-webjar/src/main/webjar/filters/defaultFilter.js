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


define(["polyfills"], function () {

    /**
     * Load the filter custom css to the page
     */
    (function loadCss(url) {
      var link = document.createElement("link");
      link.type = "text/css";
      link.rel = "stylesheet";
      link.href = url;
      document.head.appendChild(link);
    })(BASE_PATH + "filters/filters.css");



    /**
     * Create a default filter for a property
     * @param {String} propertyId The property id of the entry to display
     * @param {Object} logic The logic object associated to the livedata
     */
    var Filter = function (propertyId, index, logic) {

      this.logic = logic;
      this.propertyId = propertyId;
      this.index = index;

      this.element = undefined;
      this.initElement();
      this.filter();
    };


    /**
     * Create the root element of the filter
     */
    Filter.prototype.initElement = function () {
      var self = this;
      if (this.element) { return; }

      this.element = document.createElement("div");
      // set attributes
      this.element.className = "livedata-filter";

      // listen to events
      this.logic.onEventWhere("filter",
        function (detail) {
          if (detail.type === "removeAll") {
            return (detail.property === self.propertyId);
          }
          return (detail.oldEntry.property === self.propertyId) && detail.oldEntry.index === self.index;
        },
        function (detail) { self.filter(); }
      );
    };


    Filter.prototype.filter = function () {
      var self = this;
      var params = this._createParameters();
      return this.createFilter(params)
      .then(function (element) {
        self.element.innerHTML = "";
        self.element.appendChild(element);
      });
    };


    /**
     * Return the object of parameters to be passed to the filter creation functions
     * @returns {Object} an object containing useful data for filters: {
     *  filterGroup: the filter group of the property in the query data
     *  filterIndex: the index of the filter inside the filter group
     *  filterEntry: the filter entry object ({operator, value})
     *  property: the property descriptor object
     *  config: the configuration object of the filter, found in the filterDescriptor
     *  data: the livedata data object
     *  logic: the logic instance
     * }
     */
    Filter.prototype._createParameters = function () {
      var filterGroup = this.logic.getQueryFilterGroup(this.propertyId) || {};
      // return the param object
      return {
        filterGroup: filterGroup,
        filterIndex: this.index,
        filterEntry: (filterGroup.constrains || [])[this.index] || {},
        property: this.logic.getPropertyDescriptor(this.propertyId),
        config: this.logic.getFilterDescriptor(this.propertyId),
        data: this.logic.data,
        logic: this.logic,
      };
    };


    /**
     * Create the filter widget to be displayed
     * This method can be overriden by other filters that inherit from this one
     * Parameters are given by the Filter.prototype.initElement function
     * @param {object} params An object containing useful data for the filter.
     *  Param object detail can be found in the Filter.prototype._createParameters method
     * @returns {Promise}
     */
    Filter.prototype.createFilter = function (params) {
      var self = this;
      return new Promise (function (resolve, reject) {
        var input = document.createElement("input");
        input.type = "text";
        input.size = "1";
        input.style.width = "100%";
        if (params.filterEntry.value !== undefined && params.filterEntry.value !== null) {
          input.value = params.filterEntry.value;
        }

        // validate changes
        input.onchange = function (e) {
          self.applyFilter(e.target.value);
        };

        resolve(input);
      });
    };


    /**
     * Apply changes in the filter and update the data model
     * @param {String} newValue
     */
    Filter.prototype.applyFilter = function (newValue) {
      this.logic.filter(this.propertyId, this.index, {value: newValue});
    };


    return Filter;

  });
