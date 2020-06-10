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

      logic.loadLayout();
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
   * Load a layout, or default layout if none specified
   * @param {String} layoutId The id of the layout to load with requireJS
   * @returns {Object} A jquery promise
   */
  Logic.prototype.loadLayout = function (layoutId) {
    var self = this;
    var defer = $.Deferred();

    layoutId = layoutId || this.data.query.defaultLayout;
    if (layoutId === this.data.query.currentLayout) return;
    if (!this.data.meta.layoutDescriptors[layoutId]) return;
    if (this.data.query.layouts.indexOf(layoutId) === -1) return;

    // load layout based on it's filename
    require([BASE_PATH + "layouts/" + this.data.meta.layoutDescriptors[layoutId].file],
      // load success
      function (Layout) {
        // remove current layout from the page
        if (self.data.query.currentLayout && self.layouts[self.data.query.currentLayout]) {
          self.element.removeChild(self.layouts[self.data.query.currentLayout]);
        }
        // add layout element in loaded layouts list if not already loaded on the page
        if (!self.layouts[layoutId]) {
          self.layouts[layoutId] = new Layout(self.element);
        }
        // add new layout to the page
        self.element.appendChild(self.layouts[layoutId]);
        self.data.query.currentLayout = layoutId;
        // dispatch events
        var event = new CustomEvent("xwiki:livedata:layoutChange", {
          layoutId: layoutId,
          livedata: self,
        });
        self.element.dispatchEvent(event);
        defer.resolve();
      },

      // load failure
      function (err) {
        // try to load default layout instead
        if (layoutId !== self.data.query.defaultLayout) {
          self.loadLayout(self.data.query.defaultLayout);
        }
        else {
          console.error(err);
          defer.reject();
        }
      }
    );

    return defer.promise();
  };


  /**
  * Update sort configuration based on parameters, then fetch new data
  * @param {String} property The property to filter according to
  * @param {String} level The sort level of the property (0 is the highest).
  *   Undefined means current. Negative value removes property sort.
  * @param {String} descending Specify whether the sort should be descending or not.
  *   Undefined means toggle current direction
   */
  Logic.prototype.sort = function (property, level, descending) {
    if (this.data.query.properties.indexOf(property) === -1) { return; }
    // find property current sort level
    var currentLevel = -1;
    this.data.query.sort.some(function (sortObject, i) {
      if (sortObject.property === property) {
        currentLevel = i;
        return;
      }
    });
    // default level
    if (level === undefined) {
      level = (currentLevel !== -1) ? currentLevel : 0;
    }
    else if (level < 0) {
      level = -1;
    }
    // default descending
    if (descending === undefined) {
      descending = (currentLevel !== -1) ? !this.data.query.sort[currentLevel].descending : true;
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
    var event = new CustomEvent("xwiki:livedata:sort", {
      livedata: this,
      property: property,
      level: level,
      descending: descending,
    });
    this.element.dispatchEvent(event);

    // CALL FUNCTION TO FETCH NEW DATA HERE
  };




  // return the init function to be used in the layouts
  return init;

});



