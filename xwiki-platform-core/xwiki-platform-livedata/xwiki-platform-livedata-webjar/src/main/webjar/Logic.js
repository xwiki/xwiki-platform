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


define(["jquery", "polyfills"], function ($) {

  /**>
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
    this.layouts = {};
  };




  /**
   * Send custom events
   * @param {String} eventName The name of the event, without the prefix "xwiki:livedata"
   * @param {Object} eventData The data associated with the event.
   *  The livedata object reference is automatically added
   */
  Logic.prototype._triggerEvent = function (eventName, eventData) {
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
  };




  /**
   * Load a layout, or default layout if none specified
   * @param {String} layoutId The id of the layout to load with requireJS
   * @returns {Promise}
   */
  Logic.prototype.changeLayout = function (layoutId) {
    var self = this;
    return new Promise (function (resolve, reject) {

      layoutId = layoutId || self.data.meta.defaultLayout;
      // layout already loaded
      if (layoutId === self.data.query.currentLayout && self.layouts[layoutId]) {
        return void resolve(thselfis.layouts[layoutId]);
      }
      // bad layout
      if (!self.data.meta.layoutDescriptors[layoutId]) { return void reject(); }
      if (self.data.meta.layouts.indexOf(layoutId) === -1) { return void reject(); }

      // requirejs success callback
      var loadLayoutSuccess = function (createLayout) {
        var previousLayoutId = self.data.query.currentLayout;
        // remove current layout from the page
        if (previousLayoutId && self.layouts[previousLayoutId]) {
          self.element.removeChild(self.layouts[previousLayoutId]);
        }
        // add layout element in loaded layouts list if not already loaded on the page
        if (!self.layouts[layoutId]) {
          self.layouts[layoutId] = createLayout(self.element);
        }
        // add new layout to the page
        self.element.appendChild(self.layouts[layoutId]);
        self.data.query.currentLayout = layoutId;
        // dispatch events
        self._triggerEvent("layoutChange", {
          layout: self.layouts[layoutId],
          layoutId: layoutId,
          previousLayoutId: previousLayoutId,
        });
        resolve(self.layouts[layoutId]);
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
      require([BASE_PATH + "layouts/" + self.data.meta.layoutDescriptors[layoutId].file],
        loadLayoutSuccess,
        loadLayoutFailure
      );

    });
  };








  /**
   * Return the property descriptor corresponding to a property id
   * @param {String} propertyId
   * @returns {Object}
   */
  Logic.prototype.getPropertyDescriptor = function (propertyId) {
    return this.data.meta.propertyDescriptors.find(function (descriptor) {
      return descriptor.id === propertyId;
    });
  };



  /**
   * Return a new displayer based on the specified property and row data
   * @param {String} propertyId The id of the property of the entry
   * @param {Object} entry The entry data object
   * @param {String} displayerId
   * @returns {Promise}
   */
  Logic.prototype.createDisplayer = function (propertyId, entry, displayerId) {
    var self = this;

    return new Promise (function (resolve, reject) {
      // default displayerId
      if (displayerId === undefined) {
        displayerId = ((self.getPropertyDescriptor(propertyId) || {}).displayer || {}).id || "default";
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
  };




  /**
   * Update sort configuration based on parameters, then fetch new data
   * @param {String} property The property to sort according to
   * @param {String} level The sort level of the property (0 is the highest).
   *   Undefined means current. Negative value removes property sort.
   * @param {String} descending Specify whether the sort should be descending or not.
   *   Undefined means toggle current direction
   */
  Logic.prototype.sort = function (property, level, descending) {
    if (this.data.query.properties.indexOf(property) === -1) { return; }
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
    this._triggerEvent("sort", {
      property: property,
      level: level,
      descending: descending,
    });

    // CALL FUNCTION TO FETCH NEW DATA HERE
  };


  /**
   * Add new sort entry, shorthand of Logic.prototype.sort
   * If the property is already sorting, does nothing
   * @param {String} property The property to add to the sort
   * @param {String} descending Specify whether the sort should be descending or not.
   *   Undefined means toggle current direction
   */
  Logic.prototype.addSort = function (property, descending) {
    var currentLevel = -1;
    this.data.query.sort.some(function (sortObject, i) {
      if (sortObject.property === property) {
        currentLevel = i;
        return;
      }
    });
    if (currentLevel !== -1) { return; }
    this.sort(property, this.data.query.sort.length, descending);
  };

  /**
   * Remove a sort entry, shorthand of Logic.prototype.sort
   * @param {String} property The property to remove to the sort
   */
  Logic.prototype.removeSort = function (property) {
    this.sort(property, -1);
  };



  /**
   * Get the filter descriptor associated to a property
   * @param {String} property
   */
  Logic.prototype.getFilterDescriptor = function (property) {
    if (this.data.query.properties.indexOf(property) === -1) { return; }
    var propertyFilter = this.getPropertyDescriptor(property).filter || {};
    var filterId = propertyFilter.id || "text";
    // get default filter configuration, and combine it with current property
    var filterDescriptor = this.data.meta.filters.find(function (filterDescr) {
      return filterDescr.id === filterId;
    });
    $.extend(filterDescriptor, propertyFilter);
    return filterDescriptor;
  };


  /**
   * Get the default filter operator associated to a property
   * @param {String} property
   */
  Logic.prototype.getFilterDefaultOperator = function (property) {
    var filterDescriptor = this.getFilterDescriptor(property);
    if (!filterDescriptor) { return; }
    var filterOperators = filterDescriptor.operators;
    if (!(filterOperators instanceof Array)) { return; }
    return filterOperators[0];
  };


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
  Logic.prototype.filter = function (property, index, filterEntry) {
    var self = this;
    if (this.data.query.properties.indexOf(property) === -1) { return; }
    // default index
    index = index || 0;
    if (index < 0) { return; }
    // filter entry at current index
    var queryFilter = this.data.query.filters[property] || [];
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
    if (!this.data.query.filters[filterEntry.property]) {
      this.data.query.filters[filterEntry.property] = [];
    }
    this.data.query.filters[filterEntry.property].splice(index, 0, filterEntry);
    // dispatch events
    this._triggerEvent("filter", {
      property: filterEntry.property,
      operator: filterEntry.operator,
      value: filterEntry.value,
      index: index,
    });

    // CALL FUNCTION TO FETCH NEW DATA HERE
  };



  /**
   * Add new filter entry, shorthand of Logic.prototype.filter
   * @param {String} property Which property to add the filter to
   * @param {String} operator The operator of the filter. Should match the filter descriptor of the property
   * @param {String} value Default value for the new filter entry
   */
  Logic.prototype.addFilter = function (property, operator, value) {
    var index = (this.data.query.filters[property] || []).length;
    this.filter(property, index, {
      property: property,
      operator: operator,
      value: value
    });
  };



  /**
   * Remove a filter entry in the configuration, then fetch new data
   * @param {String} property Which property to add the filter to
   * @param {String} index The index of the filter to remove. Undefined means last.
   */
  Logic.prototype.removeFilter = function (property, index) {
    if (this.data.query.properties.indexOf(property) === -1) { return; }
    if (!this.data.query.filters[property]) { return; }
    // default index
    if (index === undefined) {
      index = this.data.query.filters[property].length - 1;
    }
    if (index < 0) { return; }
    // remove filter
    this.data.query.filters[property].splice(index, 1);
    // dispatch events
    var event = new CustomEvent("xwiki:livedata:removeFilter", {
      livedata: this,
      property: property,
      index: this.data.query.filters[property].length - 1,
    });
    this.element.dispatchEvent(event);

    // CALL FUNCTION TO FETCH NEW DATA HERE
  };





  // return the init function to be used in the layouts
  return init;

});



