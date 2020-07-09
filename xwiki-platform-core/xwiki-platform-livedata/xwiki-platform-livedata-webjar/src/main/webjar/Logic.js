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
  "Vue",
  "vue!" + BASE_PATH + "livedata-root.html",
  "polyfills"
], function (
  $,
  Vue
) {

  /**
   * Map the element to its data object
   * So that each instance of the livedata on the page handle there own data
   */
  var instancesMap = new WeakMap();



  /**
   * The init function of the logic script
   * For each livedata element on the page, returns its corresponding data / API
   * If the data does not exists yet, create it from the element
   * @param {HTMLElement} element The HTML Element corresponding to the Livedata component
   */
  var init = function (element) {

    if (!instancesMap.has(element)) {
      // create a new logic object associated to the element
      var logic = new Logic(element);
      instancesMap.set(element, logic);

      logic.changeLayout();
    }

    return instancesMap.get(element);
  };


  /**
   * Class for a logic element
   * Contains the Livedata data object and methods to mutate it
   * Can be used in the layouts to display the data, and call its API
   * @param {HTMLElement} element The HTML Element corresponding to the Livedata
   */
  var Logic = function (element) {
    this.element = element;
    this.data = JSON.parse(element.getAttribute("data-data") || "{}");
    element.removeAttribute("data-data");
    // create Vuejs instance
    new Vue({
      el: this.element,
      template: "<livedata-root :logic='logic'></livedata-root>",
      data: {
        logic: this,
      },
    });
  };




  /**
   * THE LOGIC API
   */
  Logic.prototype = {


    /**
     * ---------------------------------------------------------------
     * EVENTS
     */


    /**
     * Send custom events
     * @param {String} eventName The name of the event, without the prefix "xwiki:livedata"
     * @param {Object} eventData The data associated with the event.
     *  The livedata object reference is automatically added
     */
    triggerEvent: function (eventName, eventData) {
      // configure event
      var defaultData = {
        livedata: this,
      };
      eventName = "xwiki:livedata:" + eventName;
      eventData = {
        bubbles: true,
        detail: $.extend({}, defaultData, eventData),
      };
      var event = new CustomEvent(eventName, eventData);
      // dispatch event
      this.element.dispatchEvent(event);
    },

    /**
     * Listen for custom events
     * @param {String} eventName The name of the event, without the prefix "xwiki:livedata"
     * @param {Function} callback Function to call we the event is triggered
     */
    onEvent: function (eventName, callback) {
      eventName = "xwiki:livedata:" + eventName;
      this.element.addEventListener(eventName, function (e) {
        callback(e.detail);
      });
    },


    /**
     * Listen for custom events, mathching certain conditions
     * @param {String} eventName The name of the event, without the prefix "xwiki:livedata"
     * @param {Object|Function} condition The condition to execute the callback
     *  if Object, values of object properties must match e.detail properties values
     *  if Function, the function must return true. e.detail is passed as argument
     * @param {Function} callback Function to call we the event is triggered
     */
    onEventWhere: function (eventName, condition, callback) {
      eventName = "xwiki:livedata:" + eventName;
      this.element.addEventListener(eventName, function (e) {
        // Object check
        if (typeof condition === "object") {
          var every = Object.keys(condition).every(function (key) {
            return condition[key] === e.detail[key];
          });
          if (!every) { return; }
        }
        // Function check
        if (typeof condition === "function") {
          if (!condition(e.detail)) { return; }
        }
        // call callback
        callback(e.detail);
      });
    },




    /**
     * ---------------------------------------------------------------
     * UTILS
     */


    isValidPropertyId: function (propertyId) {
      return this.data.query.properties.indexOf(propertyId) !== -1;
    },





    /**
     * ---------------------------------------------------------------
     * DESCRIPTORS
     */


    /**
     * Return the property descriptor corresponding to a property id
     * @param {String} propertyId
     * @returns {Object}
     */
    getPropertyDescriptor: function (propertyId) {
      return this.data.meta.propertyDescriptors.find(function (propertyDescriptor) {
        return propertyDescriptor.id === propertyId;
      });
    },


    /**
     * Return the property type descriptor corresponding to a property id
     * @param {String} propertyId
     * @returns {Object}
     */
    getPropertyTypeDescriptor: function (propertyId) {
      var propertyDescriptor = this.getPropertyDescriptor(propertyId);
      return this.data.meta.propertyTypes.find(function (typeDescriptor) {
        return typeDescriptor.id === propertyDescriptor.type;
      });
    },


    /**
     * Return the property type descriptor corresponding to a property id
     * @param {String} propertyId
     * @returns {Object}
     */
    getLayoutDescriptor: function (layoutId) {
      return this.data.meta.layoutDescriptors.find(function (layoutDescriptor) {
        return layoutDescriptor.id === layoutId;
      });
    },


    /**
     * Get the displayer descriptor associated to a property id
     * @param {String} propertyId
     */
    getDisplayerDescriptor: function (propertyId) {
      if (!this.isValidPropertyId(propertyId)) { return; }
      // property descriptor config
      var propertyDescriptor = this.getPropertyDescriptor(propertyId);
      var propertyDescriptorDisplayer = propertyDescriptor.displayer || {};
      // property type descriptor config
      var typeDescriptor = this.getPropertyTypeDescriptor(propertyId);
      var typeDescriptorDisplayer = typeDescriptor.displayer || {};
      // default displayer config
      var displayerId = propertyDescriptorDisplayer.id || typeDescriptorDisplayer.id;
      var displayer = this.data.meta.displayers.find(function (displayer) {
        return displayer.id === displayerId;
      });

      return $.extend({}, displayer, typeDescriptorDisplayer, propertyDescriptorDisplayer);
    },


    /**
     * Get the filter descriptor associated to a property id
     * @param {String} propertyId
     */
    getFilterDescriptor: function (propertyId) {
      if (!this.isValidPropertyId(propertyId)) { return; }
      // property descriptor config
      var propertyDescriptor = this.getPropertyDescriptor(propertyId);
      var propertyDescriptorFilter = propertyDescriptor.filter || {};
      // property type descriptor config
      var typeDescriptor = this.getPropertyTypeDescriptor(propertyId);
      var typeDescriptorFilter = typeDescriptor.filter || {};
      // default filter config
      var filterId = propertyDescriptorFilter.id || typeDescriptorFilter.id;
      var filter = this.data.meta.filters.find(function (filter) {
        return filter.id === filterId;
      });

      return $.extend({}, filter, typeDescriptorFilter, propertyDescriptorFilter);
    },




    /**
     * ---------------------------------------------------------------
     * LAYOUT
     */


    /**
     * Load a layout, or default layout if none specified
     * @param {String} layoutId The id of the layout to load with requireJS
     * @returns {Promise}
     */
    changeLayout: function (layoutId) {
      var self = this;
      return new Promise (function (resolve, reject) {

        layoutId = layoutId || self.data.meta.defaultLayout;
        // layout already loaded
        if (layoutId === self.data.query.currentLayout) {
          return void resolve(layoutId);
        }
        // bad layout
        if (self.data.meta.layouts.indexOf(layoutId) === -1) { return void reject(); }
        var layoutDescriptor = self.getLayoutDescriptor(layoutId);
        if (!layoutDescriptor) { return void reject(); }

        // requirejs success callback
        var loadLayoutSuccess = function () {
          var previousLayoutId = self.data.query.currentLayout;
          self.data.query.currentLayout = layoutId;
          // dispatch events
          self.triggerEvent("layoutChange", {
            layoutId: layoutId,
            previousLayoutId: previousLayoutId,
          });
          resolve(layoutId);
        };

        // requirejs error callback
        var loadLayoutFailure = function (err) {
          console.warn(err);
          // try to load default layout instead
          if (layoutId !== self.data.meta.defaultLayout) {
            self.changeLayout(self.data.meta.defaultLayout).then(function (layout) {
              resolve(layout);
            }, function () {
              reject();
            });
          } else {
            console.error(err);
            reject();
          }
        };

        // load layout based on it's filename
        require(["vue!" + BASE_PATH + "layouts/livedata-layout-" + layoutId + ".html"],
          loadLayoutSuccess,
          loadLayoutFailure
        );

      });
    },




    /**
     * ---------------------------------------------------------------
     * PAGINATION
     */


    /**
     * Get total number of pages
     * @returns {Number}
     */
    getPageCount: function () {
      return Math.ceil(this.data.data.count / this.data.query.limit);
    },


    /**
     * Get the page corresponding to the specified entry (0-based index)
     * @param {Number} entryIndex The index of the entry. Uses current entry if undefined.
     * @returns {Number}
     */
    getPageIndex: function (entryIndex) {
      if (entryIndex === undefined) {
        entryIndex = this.data.query.offset;
      }
      return Math.floor(entryIndex / this.data.query.limit);
    },


    /**
     * Set page index (0-based index), then fetch new data
     * @param {Number} pageIndex
     * @returns {Number}
     */
    setPageIndex: function (pageIndex) {
      if (pageIndex < 0 || pageIndex >= this.getPageCount()) { return; }
      var previousPageIndex = this.data.query.offset;
      this.data.query.offset = this.getFirstIndexOfPage(pageIndex);
      this.triggerEvent("pageChange", {
        pageIndex: pageIndex,
        previousPageIndex: previousPageIndex,
      });
      // CALL FUNCTION TO FETCH NEW DATA HERE
    },


    /**
     * Get the first entry index of the given page index
     * @param {Number} pageIndex The page index. Uses current page if undefined.
     * @returns {Number}
     */
    getFirstIndexOfPage: function (pageIndex) {
      if (pageIndex === undefined) {
        pageIndex = this.getPageIndex();
      }
      if (0 <= pageIndex && pageIndex < this.getPageCount()) {
        return pageIndex * this.data.query.limit;
      } else {
        return -1;
      }
    },


    /**
     * Get the last entry index of the given page index
     * @param {Number} pageIndex The page index. Uses current page if undefined.
     * @returns {Number}
     */
    getLastIndexOfPage: function (pageIndex) {
      if (pageIndex === undefined) {
        pageIndex = this.getPageIndex();
      }
      if (0 <= pageIndex && pageIndex < this.getPageCount()) {
        return Math.min(this.getFirstIndexOfPage(pageIndex) + this.data.query.limit, this.data.data.count) - 1;
      } else {
        return -1;
      }
    },


    /**
     * Set the pagination page size, then fetch new data
     * @param {Number} pageSize
     */
    setPageSize: function (pageSize) {
      if (pageSize < 0) { return; }
      var previousPageSize = this.data.query.limit;
      this.data.query.limit = pageSize;
      this.triggerEvent("pageSizeChange", {
        pageSize: pageSize,
        previousPageSize: previousPageSize,
      });
      // CALL FUNCTION TO FETCH NEW DATA HERE
    },




    /**
     * ---------------------------------------------------------------
     * DISPLAYERS
     */


    /**
     * Return a new displayer widget based on the specified property and row data
     * @param {String} propertyId The id of the property of the entry
     * @param {Object} entry The entry data object
     * @param {String} displayerId
     * @returns {Promise}
     */
    createDisplayer: function (propertyId, entry, displayerId) {
      var self = this;

      return new Promise (function (resolve, reject) {
        // default displayerId
        if (displayerId === undefined) {
          displayerId = self.getDisplayerDescriptor(propertyId).id;
        }

        // load success callback
        var loadDisplayerSuccess = function (Displayer) {
          var displayer = new Displayer(propertyId, entry, self);
          resolve(displayer);
        };

        // load error callback
        var loadDisplayerFailure = function (err) {
          // try to load the default displayer instead
          if (displayerId !== "default") {
            self.createDisplayer(propertyId, entry, "default").then(function (displayer) {
              resolve(displayer);
            }, function () {
              reject();
            });
          } else {
            console.error(err);
            reject();
          }
        };

        // load displayer based on it's id
        require([BASE_PATH + "displayers/" + displayerId + "Displayer.js"],
          loadDisplayerSuccess,
          loadDisplayerFailure
        );

      });
    },




    /**
     * ---------------------------------------------------------------
     * SORT
     */


    /**
     * Returns whether a certain property is sortable or not
     * @param {String} propertyId
     * @returns {Boolean}
     */
    isPropertySortable: function (propertyId) {
      var propertyDescriptor = this.getPropertyDescriptor(propertyId);
      var propertyTypeDescriptor = this.getPropertyTypeDescriptor(propertyId);
      return propertyDescriptor.sortable ||
        (propertyDescriptor.sortable === undefined && propertyTypeDescriptor.sortable);
    },


    /**
     * Returns the property descriptors of sortable properties
     * @returns {Array}
     */
    getSortablePropertyDescriptors: function () {
      var self = this;
      return this.data.meta.propertyDescriptors.filter(function (propertyDescriptor) {
        return self.isPropertySortable(propertyDescriptor.id);
      });
    },


    /**
     * Get the sort query associated to a property id
     * @param {String} propertyId
     */
    getQuerySort: function (propertyId) {
      if (!this.isValidPropertyId(propertyId)) { return; }
      return this.data.query.sort.find(function (sort) {
        return sort.property === propertyId;
      });
    },


    /**
     * Update sort configuration based on parameters, then fetch new data
     * @param {String} property The property to sort according to
     * @param {String} level The sort level of the property (0 is the highest).
     *   Undefined means current. Negative value removes property sort.
     * @param {String} descending Specify whether the sort should be descending or not.
     *   Undefined means toggle current direction
     */
    sort: function (property, level, descending) {
      if (!this.isValidPropertyId(property)) { return; }
      if (!this.isPropertySortable(property)) { return; }
      // find property current sort level
      var currentLevel = this.data.query.sort.findIndex(function (sortObject) {
        return sortObject.property === property;
      });
      // default level
      if (level === undefined) {
        level = (currentLevel !== -1) ? currentLevel : 0;
      } else if (level < 0) {
        level = -1;
      }
      // default descending
      if (descending === undefined) {
        descending = (currentLevel !== -1) ? !this.data.query.sort[currentLevel].descending : false;
      }
      // create sort object
      var sortObject = {
        property: property,
        descending: descending,
      };
      // apply sort
      if (level !== -1) {
        this.data.query.sort.splice(level, 1, sortObject);
      }
      if (currentLevel !== -1 && currentLevel !== level) {
        this.data.query.sort.splice(currentLevel, 1);
      }
      // dispatch events
      this.triggerEvent("sort", {
        property: property,
        level: level,
        descending: descending,
      });

      // CALL FUNCTION TO FETCH NEW DATA HERE
    },


    /**
     * Add new sort entry, shorthand of sort:
     * If the property is already sorting, does nothing
     * @param {String} property The property to add to the sort
     * @param {String} descending Specify whether the sort should be descending or not.
     *   Undefined means toggle current direction
     */
    addSort: function (property, descending) {
      var currentLevel = -1;
      this.data.query.sort.some(function (sortObject, i) {
        if (sortObject.property === property) {
          currentLevel = i;
          return;
        }
      });
      if (currentLevel !== -1) { return; }
      this.sort(property, this.data.query.sort.length, descending);
    },


    /**
     * Remove a sort entry, shorthand of sort:
     * @param {String} property The property to remove to the sort
     */
    removeSort: function (property) {
      this.sort(property, -1);
    },




    /**
     * ---------------------------------------------------------------
     * FILTER
     */


    /**
     * Returns whether a certain property is filterable or not
     * @param {String} propertyId
     * @returns {Boolean}
     */
    isPropertyFilterable: function (propertyId) {
      return !!this.getFilterDescriptor(propertyId).id;
    },


    /**
     * Returns the property descriptors of filterable properties
     * @returns {Array}
     */
    getFilterablePropertyDescriptors: function () {
      var self = this;
      return this.data.meta.propertyDescriptors.filter(function (propertyDescriptor) {
        return self.isPropertyFilterable(propertyDescriptor.id);
      });
    },


    /**
     * Get the filter query associated to a property id
     * @param {String} propertyId
     */
    getQueryFilter: function (propertyId) {
      if (!this.isValidPropertyId(propertyId)) { return; }
      return this.data.query.filters.find(function (filter) {
        return filter.property === propertyId;
      });
    },


    /**
     * Get the default filter operator associated to a property id
     * @param {String} propertyId
     * @returns {String}
     */
    getFilterDefaultOperator: function (propertyId) {
      var filterDescriptor = this.getFilterDescriptor(propertyId);
      if (!filterDescriptor) { return; }
      var filterOperators = filterDescriptor.operators;
      if (!(filterOperators instanceof Array)) { return; }
      return filterOperators[0].id;
    },


    /**
     * Update filter configuration based on parameters, then fetch new data
     * @param {String} property The property to filter according to
     * @param {String} index The index of the filter entry
     * @param {String} filterEntry The filter data used to update the filter configuration
     *  filterEntry = {property, operator, value}
     *  undefined values are defaulted to current values, then to default values.
     * @param {String} filterEntry.property The new property to filter according to
     * @param {String} filter.operator The operator of the filter.
     *  Should match the filter descriptor of the filter property
     * @param {String} filter.value Value for the new filter entry
     */
    filter: function (property, index, filterEntry) {
      var self = this;
      if (!this.isValidPropertyId(property)) { return; }
      if (!this.isPropertyFilterable(property)) { return; }
      // default index
      index = index || 0;
      if (index < 0) { return; }
      // filter entry at current index
      var queryFilterGroup = this.getQueryFilter(property) || {};
      var queryFilter = queryFilterGroup.constrains || [];
      var currentEntry = queryFilter[index] || {};
      // default filterEntry
      filterEntry = filterEntry || {};
      var defaultFilterEntry = {
        property: property,
        value: "",
        get operator () {
          return self.getFilterDefaultOperator(this.property);
        },
      };
      filterEntry = $.extend({}, defaultFilterEntry, currentEntry, filterEntry);
      if (filterEntry.operator === undefined) { return; }
      // apply filter
      // remove filter at current property and index
      if (queryFilter[index]) {
        queryFilter.splice(index, 1);
      }
      // add filter at new property and index
      if (!this.getQueryFilter(filterEntry.property)) {
        this.data.query.filters.push({
          property: filterEntry.property,
          matchAll: true,
          constrains: [],
        });
      }
      this.getQueryFilter(filterEntry.property).constrains.splice(index, 0, filterEntry);
      // dispatch events
      this.triggerEvent("filter", {
        type: "modify",
        property: filterEntry.property,
        operator: filterEntry.operator,
        value: filterEntry.value,
        index: index,
      });

      // CALL FUNCTION TO FETCH NEW DATA HERE
    },


    /**
     * Add new filter entry, shorthand of filter:
     * @param {String} property Which property to add the filter to
     * @param {String} operator The operator of the filter. Should match the filter descriptor of the property
     * @param {String} value Default value for the new filter entry
     */
    addFilter: function (property, operator, value) {
      var index = ((this.getQueryFilter(property) || []).constrains || []).length;
      this.filter(property, index, {
        property: property,
        operator: operator,
        value: value
      });
    },


    /**
     * Remove a filter entry in the configuration, then fetch new data
     * @param {String} property Property to remove the filter to
     * @param {String} index The index of the filter to remove. Undefined means last.
     */
    removeFilter: function (property, index) {
      if (!this.isValidPropertyId(property)) { return; }
      var filter = this.getQueryFilter(property);
      if (!filter || filter.constrains.length === 0) { return; }
      // default index
      if (index === undefined) {
        index = filter.constrains.length - 1;
      }
      if (index < 0) { return; }
      // remove filter
      var removedFilterArray = filter.constrains.splice(index, 1);
      // dispatch events
      this.triggerEvent("filter", {
        type: "remove",
        property: property,
        index: index,
        removedFilter: removedFilterArray[0],
      });
      // CALL FUNCTION TO FETCH NEW DATA HERE
    },


    /**
     * Remove all the filters associated to a property
     * @param {String} property Property to remove the filters to
     */
    removeAllFilters: function (property) {
      if (!this.isValidPropertyId(property)) { return; }
      var filterIndex = this.data.query.filters.findIndex(function (filterGroup, i) {
        return filterGroup.property === property;
      });
      if (filterIndex === -1) { return; }
      var removedFilterGroups = this.data.query.filters.splice(filterIndex, 1);
      // dispatch events
      this.triggerEvent("filter", {
        type: "removeAll",
        property: property,
        removedFilters: removedFilterGroups[0].constrains,
      });
      // CALL FUNCTION TO FETCH NEW DATA HERE
    },


    /**
     * Return a new filter widget based on the specified property
     * @param {String} propertyId The id of the property of the entry
     * @param {Number} index The index of the filter among the property filters
     * @param {String} filterId
     * @returns {Promise}
     */
    createFilter: function (propertyId, index, filterId) {
      var self = this;

      return new Promise (function (resolve, reject) {
        // default filterId
        if (filterId === undefined) {
          filterId = self.getFilterDescriptor(propertyId).id;
        }

        // load success callback
        var loadFilterSuccess = function (Filter) {
          var filter = new Filter(propertyId, index, self);
          resolve(filter);
        };

        // load error callback
        var loadFilterFailure = function (err) {
          // try to load the default filter instead
          if (filterId !== "default") {
            self.createFilter(propertyId, index, "default").then(function (filter) {
              resolve(filter);
            }, function () {
              reject();
            });
          } else {
            console.error(err);
            reject();
          }
        };

        // load filter based on it's id
        require([BASE_PATH + "filters/" + filterId + "Filter.js"],
          loadFilterSuccess,
          loadFilterFailure
        );

      });
    },


  };





  // return the init function to be used in the layouts
  return init;

});



