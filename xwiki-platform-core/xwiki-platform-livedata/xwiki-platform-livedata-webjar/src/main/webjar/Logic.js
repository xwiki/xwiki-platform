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
   * Change layout to specified one, or default one if none specified
   * @param {String} layoutId The id of the layout to load with requireJS
   * @returns {Object} A jquery promise
   */
  Logic.prototype.changeLayout = function (layoutId) {
    var self = this;
    var defer = $.Deferred();

    layoutId = layoutId || this.data.meta.defaultLayout;
    if (layoutId === this.data.query.currentLayout) { return defer.resolve(this.layouts[layoutId]); }
    if (!this.data.meta.layoutDescriptors[layoutId]) { return defer.reject(); }
    if (this.data.meta.layouts.indexOf(layoutId) === -1) { return defer.reject(); }

    // load layout based on it's filename
    require([BASE_PATH + "layouts/" + this.data.meta.layoutDescriptors[layoutId].file],
      // load success
      function (createLayout) {
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
        var event = new CustomEvent("xwiki:livedata:layoutChange", {
          layout: self.layouts[layoutId],
          layoutId: layoutId,
          previousLayoutId: previousLayoutId,
          livedata: self,
        });
        self.element.dispatchEvent(event);
        defer.resolve(self.layouts[layoutId]);
      },

      // load failure
      function (err) {
        // try to load default layout instead
        if (layoutId !== self.data.meta.defaultLayout) {
          self.changeLayout(self.data.meta.defaultLayout).then(function (layout) {
            defer.resolve(layout);
          }, function () {
            defer.reject();
          });
        }
        else {
          console.error(err);
          defer.reject();
        }
      }
    );

    return defer.promise();
  };



  // return the init function to be used in the layouts
  return init;

});



