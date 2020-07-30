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
  "vue!livedata-root",
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
    var self = this;

    this.element = element;
    this.data = JSON.parse(element.getAttribute("data-data") || "{}");

    this.currentLayoutId = "";
    this.changeLayout(this.data.meta.defaultLayout);
    this.entrySelection = {
      selected: [],
      deselected: [],
      isGlobal: false,
    };
    this.hiddenProperties = [];
    this.propertyOrder = this.data.meta.propertyDescriptors
    .filter(function (propertyDescriptor) {
      return self.isPropertyDisplayable(propertyDescriptor.id);
    })
    .map(function (propertyDescriptor) {
      return propertyDescriptor.id;
    });
    this.openedPanels = [];

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
        detail: $.extend(defaultData, eventData),
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


    /**
     * Return the list of all property ids (from propertyDescriptors)
     * @returns {Array}
     */
    getPropertyIds: function () {
      return this.data.meta.propertyDescriptors.map(function (propertyDescriptor) {
        return propertyDescriptor.id;
      });
    },


    /**
     * Return the list of layout ids
     * @returns {Array}
     */
    getLayoutIds: function () {
      return this.data.meta.layouts.map(function (layoutDescriptor) {
        return layoutDescriptor.id;
      });
    },


    /**
     * Return whether the specified property id is valid (i.e. the property has a descriptor)
     * @param {String} propertyId
     */
    isValidPropertyId: function (propertyId) {
      return this.getPropertyIds().indexOf(propertyId) !== -1;
    },


    /**
     * Return the id of the given entry
     * @param {Object} entry
     * @returns {String}
     */
    getEntryId: function (entry) {
      var idProperty = this.data.meta.entryDescriptor.idProperty || "id";
      if (entry[idProperty] === undefined) {
        console.warn("Entry has no id (at property [" + idProperty + "]", entry);
        return;
      }
      return entry[idProperty];
    },


    /**
     * Return whether the array has the given item
     * @param {Array} uniqueArray An array of unique items
     * @param {Any} item
     */
    uniqueArrayHas: function (uniqueArray, item) {
      return uniqueArray.indexOf(item) !== -1;
    },


    /**
     * Add the given item if not present in the array, or does nothing
     * @param {Array} uniqueArray An array of unique items
     * @param {Any} item
     */
    uniqueArrayAdd: function (uniqueArray, item) {
      if (this.uniqueArrayHas(uniqueArray, item)) { return; }
      uniqueArray.push(item);
    },


    /**
     * Remove the given item from the array if present, or does nothing
     * @param {Array} uniqueArray An array of unique items
     * @param {Any} item
     */
    uniqueArrayRemove: function (uniqueArray, item) {
      var index = uniqueArray.indexOf(item);
      if (index === -1) { return; }
      uniqueArray.splice(index, 1);
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
      if (!propertyDescriptor) { return; }
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
      return this.data.meta.layouts.find(function (layoutDescriptor) {
        return layoutDescriptor.id === layoutId;
      });
    },


    /**
     * Get the displayer descriptor associated to a property id
     * @param {String} propertyId
     * @returns {Object}
     */
    getDisplayerDescriptor: function (propertyId) {
      if (!this.isValidPropertyId(propertyId)) { return; }
      // property descriptor config
      var propertyDescriptor = this.getPropertyDescriptor(propertyId) || {};
      var propertyDescriptorDisplayer = propertyDescriptor.displayer || {};
      // property type descriptor config
      var typeDescriptor = this.getPropertyTypeDescriptor(propertyId) || {};
      var typeDescriptorDisplayer = typeDescriptor.displayer || {};
      // merge property and/or type displayer descriptors
      var highLevelDisplayer;
      if (!propertyDescriptorDisplayer.id || propertyDescriptorDisplayer.id === typeDescriptorDisplayer.id) {
        highLevelDisplayer = $.extend({}, typeDescriptorDisplayer, propertyDescriptorDisplayer);
      } else {
        highLevelDisplayer = $.extend({}, propertyDescriptorDisplayer);
      }
      // displayer config
      var displayerId = highLevelDisplayer.id;
      var displayer = this.data.meta.displayers.find(function (displayer) {
        return displayer.id === displayerId;
      });
      // merge displayers
      if (highLevelDisplayer.id) {
        return $.extend({}, displayer, highLevelDisplayer);
      } else {
        // default displayer
        return { id: this.data.meta.defaultDisplayer };
      }
    },


    /**
     * Get the filter descriptor associated to a property id
     * @param {String} propertyId
     * @returns {Object}
     */
    getFilterDescriptor: function (propertyId) {
      if (!this.isValidPropertyId(propertyId)) { return; }
      // property descriptor config
      var propertyDescriptor = this.getPropertyDescriptor(propertyId) || {};
      var propertyDescriptorFilter = propertyDescriptor.filter || {};
      // property type descriptor config
      var typeDescriptor = this.getPropertyTypeDescriptor(propertyId) || {};
      var typeDescriptorFilter = typeDescriptor.filter || {};
      // merge property and/or type filter descriptors
      var highLevelFilter;
      if (!propertyDescriptorFilter.id || propertyDescriptorFilter.id === typeDescriptorFilter.id) {
        highLevelFilter = $.extend({}, typeDescriptorFilter, propertyDescriptorFilter);
      } else {
        highLevelFilter = $.extend({}, propertyDescriptorFilter);
      }
      // filter filter config
      var filterId = highLevelFilter.id;
      var filter = this.data.meta.filters.find(function (filter) {
        return filter.id === filterId;
      });
      // merge filters
      if (highLevelFilter.id) {
        return $.extend({}, filter, highLevelFilter);
      } else {
        // default filter
        return { id: this.data.meta.defaultFilter };
      }
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
      // bad layout
      if (!this.getLayoutDescriptor(layoutId)) {
        console.error("Layout of id `" + layoutId + "` does not have a descriptor");
        return;
      }
      // set layout
      var previousLayoutId = this.currentLayoutId;
      this.currentLayoutId = layoutId;
      // dispatch events
      this.triggerEvent("layoutChange", {
        layoutId: layoutId,
        previousLayoutId: previousLayoutId,
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
     * @returns {Promise}
     */
    setPageIndex: function (pageIndex) {
      var self = this;
      return new Promise (function (resolve, reject) {
        if (pageIndex < 0 || pageIndex >= self.getPageCount()) { return void reject(); }
        var previousPageIndex = self.getPageIndex();
        self.data.query.offset = self.getFirstIndexOfPage(pageIndex);
        self.triggerEvent("pageChange", {
          pageIndex: pageIndex,
          previousPageIndex: previousPageIndex,
        });
        // CALL FUNCTION TO FETCH NEW DATA HERE
        resolve();
      });
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
     * @returns {Promise}
     */
    setPageSize: function (pageSize) {
      var self = this;
      return new Promise (function (resolve, reject) {
        if (pageSize < 0) { return void reject(); }
        var previousPageSize = self.data.query.limit;
        if (pageSize === previousPageSize) { return void resolve(); }
        self.data.query.limit = pageSize;
        self.triggerEvent("pageSizeChange", {
          pageSize: pageSize,
          previousPageSize: previousPageSize,
        });
        // CALL FUNCTION TO FETCH NEW DATA HERE
        resolve();
      });
    },




    /**
     * ---------------------------------------------------------------
     * DISPLAY
     */


    /**
     * Returns whether a certain property is displayable
     * @param {String} propertyId
     * @returns {Boolean}
     */
    isPropertyDisplayable: function (propertyId) {
      var propertyDescriptor = this.getPropertyDescriptor(propertyId);
      var propertyTypeDescriptor = this.getPropertyTypeDescriptor(propertyId);
      return (propertyDescriptor.hidden !== undefined && !propertyDescriptor.hidden) ||
        (propertyDescriptor.hidden === undefined && !propertyTypeDescriptor.hidden);
    },


    /**
     * Returns the property descriptors of displayable properties
     * @returns {Array}
     */
    getDisplayablePropertyDescriptors: function () {
      var self = this;
      return this.data.meta.propertyDescriptors
      .filter(function (propertyDescriptor) {
        return self.isPropertyDisplayable(propertyDescriptor.id);
      })
      .sort(function (descriptorA, descriptorB) {
        return self.propertyOrder.indexOf(descriptorA.id) - self.propertyOrder.indexOf(descriptorB.id);
      });
    },


    /**
     * Returns whether a certain property is visible
     * @param {String} propertyId
     * @returns {Boolean}
     */
    isPropertyVisible: function (propertyId) {
      return this.isPropertyDisplayable(propertyId) &&
        !this.uniqueArrayHas(this.hiddenProperties, propertyId);
    },


    /**
     * Set whether the given property should be visible
     * @param {String} propertyId
     * @param {Boolean} bool
     */
    setPropertyVisibility: function (propertyId, bool) {
      if (!this.isPropertyDisplayable(propertyId)) { return; }
      if (bool) {
        // set visible
        this.uniqueArrayRemove(this.hiddenProperties, propertyId);
      } else {
        // set hidden
        this.uniqueArrayAdd(this.hiddenProperties, propertyId);
      }
    },


    /**
     * Move a property to a certain index in the property order list
     * @param {String|Number} from The id or index of the property to move
     * @param {*} toIndex
     */
    reorderProperty: function (from, toIndex) {
      var fromIndex;
      if (typeof from === "number") {
        fromIndex = from;
      } else if (typeof from === "string") {
        if (!this.isValidPropertyId(from)) { return; }
        if (toIndex < 0) { toIndex = 0; }
        fromIndex = this.propertyOrder.indexOf(from);
      } else {
        return;
      }
      if (fromIndex <= -1 || toIndex <= -1) { return; }
      this.propertyOrder.splice(toIndex, 0, this.propertyOrder.splice(fromIndex, 1)[0]);
    },




    /**
     * ---------------------------------------------------------------
     * ACTIONS
     */


    /**
     * Return whether the entry is currently selected
     * @param {Object} entry
     * @returns {Boolean}
     */
    isEntrySelected: function (entry) {
      var entryId = this.getEntryId(entry);
      if (this.entrySelection.isGlobal) {
        return !this.uniqueArrayHas(this.entrySelection.deselected, entryId);
      } else {
        return this.uniqueArrayHas(this.entrySelection.selected, entryId);
      }
    },


    /**
     * Select the specified entries
     * @param {Object|Array} entries
     */
    selectEntries: function (entries) {
      var self = this;
      var entryArray = (entries instanceof Array) ? entries : [entries];
      entryArray.forEach(function (entry) {
        var entryId = self.getEntryId(entry);
        if (self.entrySelection.isGlobal) {
          self.uniqueArrayRemove(self.entrySelection.deselected, entryId);
        }
        else {
          self.uniqueArrayAdd(self.entrySelection.selected, entryId);
        }
        self.triggerEvent("select", {
          entry: entry,
        });
      });
    },


    /**
     * Deselect the specified entries
     * @param {Object|Array} entries
     */
    deselectEntries: function (entries) {
      var self = this;
      var entryArray = (entries instanceof Array) ? entries : [entries];
      entryArray.forEach(function (entry) {
        var entryId = self.getEntryId(entry);
        if (self.entrySelection.isGlobal) {
          self.uniqueArrayAdd(self.entrySelection.deselected, entryId);
        }
        else {
          self.uniqueArrayRemove(self.entrySelection.selected, entryId);
        }
        self.triggerEvent("deselect", {
          entry: entry,
        });
      });
    },


    /**
     * Toggle the selection of the specified entries
     * @param {Object|Array} entries
     * @param {Boolean} select Whether to select or not the entries. Undefined toggle current state
     */
    toggleSelectEntries: function (entries, select) {
      var self = this;
      var entryArray = (entries instanceof Array) ? entries : [entries];
      entryArray.forEach(function (entry) {
        if (select === undefined) {
          select = !self.isEntrySelected(entry);
        }
        if (select) {
          self.selectEntries(entry);
        } else {
          self.deselectEntries(entry);
        }
      });
    },


    /**
     * Get number of selected entries
     * @returns {Number}
     */
    getSelectedEntriesCount: function () {
      if (this.entrySelection.isGlobal) {
        return this.data.data.count - this.entrySelection.deselected.length;
      } else {
        return this.entrySelection.selected.length;
      }
    },


    /**
     * Set the entry selection globally accross pages
     * @param {Boolean} bool
     */
    setEntrySelectGlobal: function (bool) {
      this.entrySelection.isGlobal = bool;
      this.entrySelection.selected.splice(0);
      this.entrySelection.deselected.splice(0);
      this.triggerEvent("selectGlobal", {
        state: bool,
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
     * @returns {Promise}
     */
    sort: function (property, level, descending) {
      var self = this;
      var err = new Error("Property `" + property + "` is not sortable");
      return new Promise (function (resolve, reject) {
        if (!self.isValidPropertyId(property)) { return void reject(err); }
        if (!self.isPropertySortable(property)) { return void reject(err); }
        // find property current sort level
        var currentLevel = self.data.query.sort.findIndex(function (sortObject) {
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
          descending = (currentLevel !== -1) ? !self.data.query.sort[currentLevel].descending : false;
        }
        // create sort object
        var sortObject = {
          property: property,
          descending: descending,
        };
        // apply sort
        if (level !== -1) {
          self.data.query.sort.splice(level, 1, sortObject);
        }
        if (currentLevel !== -1 && currentLevel !== level) {
          self.data.query.sort.splice(currentLevel, 1);
        }
        // dispatch events
        self.triggerEvent("sort", {
          property: property,
          level: level,
          descending: descending,
        });

        // CALL FUNCTION TO FETCH NEW DATA HERE
        resolve();
      });
    },


    /**
     * Add new sort entry, shorthand of sort:
     * If the property is already sorting, does nothing
     * @param {String} property The property to add to the sort
     * @param {String} descending Specify whether the sort should be descending or not.
     *   Undefined means toggle current direction
     * @returns {Promise}
     */
    addSort: function (property, descending) {
      var currentLevel = -1;
      this.data.query.sort.some(function (sortObject, i) {
        if (sortObject.property === property) {
          currentLevel = i;
          return;
        }
      });
      if (currentLevel !== -1) { return Promise.reject(); }
      return this.sort(property, this.data.query.sort.length, descending);
    },


    /**
     * Remove a sort entry, shorthand of sort:
     * @param {String} property The property to remove to the sort
     * @returns {Promise}
     */
    removeSort: function (property) {
      return this.sort(property, -1);
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
      var propertyDescriptor = this.getPropertyDescriptor(propertyId);
      var propertyTypeDescriptor = this.getPropertyTypeDescriptor(propertyId);
      return propertyDescriptor.filterable ||
        (propertyDescriptor.filterable === undefined && propertyTypeDescriptor.filterable);
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
     * Get the filter in the query data object associated to a property id
     * @param {String} propertyId
     * @returns {Object}
     */
    getQueryFilterGroup: function (propertyId) {
      if (!this.isValidPropertyId(propertyId)) { return; }
      return this.data.query.filters.find(function (filter) {
        return filter.property === propertyId;
      });
    },


    /**
     * Get the filters in the query data object associated to a property id
     * @param {String} propertyId
     * @returns {Array} The constrains array of the filter group, or empty array if it does not exist
     */
    getQueryFilters: function (propertyId) {
      if (!this.isValidPropertyId(propertyId)) { return; }
      var queryFilterGroup = this.getQueryFilterGroup(propertyId);
      return queryFilterGroup && queryFilterGroup.constrains || [];
    },


    /**
     * Get the default filter operator associated to a property id
     * @param {String} propertyId
     * @returns {String}
     */
    getFilterDefaultOperator: function (propertyId) {
      // get valid operator descriptor
      var filterDescriptor = this.getFilterDescriptor(propertyId);
      if (!filterDescriptor) { return; }
      var filterOperators = filterDescriptor.operators;
      if (!(filterOperators instanceof Array)) { return; }
      if (filterOperators.length === 0) { return; }
      // get default operator
      var defaultOperator = filterDescriptor.defaultOperator;
      var isDefaultOperatorValid = !!filterOperators.find(function (operator) {
        return operator.id === defaultOperator;
      });
      if (defaultOperator && isDefaultOperatorValid) {
        return defaultOperator;
      } else {
        return filterOperators[0].id;
      }
    },


    /**
     * Return an object containing the new and old filter entries corresponding to parameters
     *  oldEntry: the filter entry to be modified
     *  newEntry: what this entry should be modified to
     * @param {String} property The property to filter according to
     * @param {String} index The index of the filter entry
     * @param {String} filterEntry The filter data used to update the filter configuration
     *  (see Logic.prototype.filter for more)
     * @returns {Object} {oldEntry, newEntry}
     *  with oldEntry / newEntry being {property, index, operator, value}
     */
    _computeFilterEntries: function (property, index, filterEntry) {
      var self = this;
      if (!this.isValidPropertyId(property)) { return; }
      if (!this.isPropertyFilterable(property)) { return; }
      // default indexes
      index = index || 0;
      if (index < 0) { return; }
      if (filterEntry.index < 0) { filterEntry.index = -1; }
      // old entry
      var oldEntry = {
        property: property,
        index: index,
      };
      var queryFilters = this.getQueryFilters(property);
      var currentEntry = queryFilters[index] || {};
      oldEntry = $.extend({}, currentEntry, oldEntry);
      // new entry
      var newEntry = filterEntry || {};
      var defaultEntry = {
        property: property,
        value: "",
        get operator () { return self.getFilterDefaultOperator(this.property); },
        index: 0,
      };
      newEntry = $.extend({}, defaultEntry, oldEntry, newEntry);
      return {
        oldEntry: oldEntry,
        newEntry: newEntry,
      };
    },


    /**
     * Return the filtering type, based on oldEntry and newEntry
     * @param {Object} oldEntry
     * @param {Oject} newEntry
     * @returns {String} "add" | "remove" | "move" | "modify"
     */
    _getFilteringType: function (oldEntry, newEntry) {
      var queryFilter = this.getQueryFilterGroup(oldEntry.property);
      if (queryFilter && oldEntry.index >= queryFilter.constrains.length) {
        return "add";
      }
      if (newEntry.index === -1) {
        return "remove";
      }
      if (oldEntry.index !== newEntry.index) {
        return "move";
      }
      return "modify";
    },


    /**
     * Update filter configuration based on parameters, then fetch new data
     * @param {String} property The property to filter according to
     * @param {String} index The index of the filter entry
     * @param {String} filterEntry The filter data used to update the filter configuration
     *  filterEntry = {property, operator, value}
     *  undefined values are defaulted to current values, then to default values.
     * @param {String} filterEntry.property The new property to filter according to
     * @param {String} filterEntry.index The new index the filter should go. -1 delete filter
     * @param {String} filterEntry.operator The operator of the filter.
     *  Should match the filter descriptor of the filter property
     * @param {String} filterEntry.value Value for the new filter entry
     * @returns {Promise}
     */
    filter: function (property, index, filterEntry) {
      var self = this;
      var err = new Error("Property `" + property + "` is not filterable");
      return new Promise (function (resolve, reject) {
        var filterEntries = self._computeFilterEntries(property, index, filterEntry);
        if (!filterEntries) { return void reject(err); }
        var oldEntry = filterEntries.oldEntry;
        var newEntry = filterEntries.newEntry;
        var filteringType = self._getFilteringType(oldEntry, newEntry);
        // remove filter at current property and index
        self.getQueryFilters(oldEntry.property).splice(index, 1);
        // add filter at new property and index
        if (newEntry.index !== -1) {
          // create filterGroup if not exists
          if (!self.getQueryFilterGroup(newEntry.property)) {
            self.data.query.filters.push({
              property: newEntry.property,
              matchAll: true,
              constrains: [],
            });
          }
          // add entry
          self.getQueryFilterGroup(newEntry.property).constrains.splice(newEntry.index, 0, {
            operator: newEntry.operator,
            value: newEntry.value,
          });
        }
        // remove filter group if empty
        if (self.getQueryFilters(oldEntry.property).length === 0) {
          self.removeAllFilters(oldEntry.property);
        }
        // dispatch events
        self.triggerEvent("filter", {
          type: filteringType,
          oldEntry: oldEntry,
          newEntry: newEntry,
        });

        // CALL FUNCTION TO FETCH NEW DATA HERE
        resolve();
      });
    },


    /**
     * Add new filter entry, shorthand of filter:
     * @param {String} property Which property to add the filter to
     * @param {String} operator The operator of the filter. Should match the filter descriptor of the property
     * @param {String} value Default value for the new filter entry
     * @returns {Promise}
     */
    addFilter: function (property, operator, value) {
      var index = ((this.getQueryFilterGroup(property) || []).constrains || []).length;
      return this.filter(property, index, {
        property: property,
        operator: operator,
        value: value
      });
    },


    /**
     * Remove a filter entry in the configuration, then fetch new data
     * @param {String} property Property to remove the filter to
     * @param {String} index The index of the filter to remove. Undefined means last.
     * @returns {Promise}
     */
    removeFilter: function (property, index) {
      return this.filter(property, index, {index: -1});
    },


    /**
     * Remove all the filters associated to a property
     * @param {String} property Property to remove the filters to
     * @returns {Promise}
     */
    removeAllFilters: function (property) {
      var self = this;
      return new Promise (function (resolve, reject) {
        if (!self.isValidPropertyId(property)) { return; }
        var filterIndex = self.data.query.filters.findIndex(function (filterGroup, i) {
          return filterGroup.property === property;
        });
        if (filterIndex === -1) { return void reject(); }
        var removedFilterGroups = self.data.query.filters.splice(filterIndex, 1);
        // dispatch events
        self.triggerEvent("filter", {
          type: "removeAll",
          property: property,
          removedFilters: removedFilterGroups[0].constrains,
        });
        // CALL FUNCTION TO FETCH NEW DATA HERE
        resolve();
      });
    },




  };





  // return the init function to be used in the layouts
  return init;

});



