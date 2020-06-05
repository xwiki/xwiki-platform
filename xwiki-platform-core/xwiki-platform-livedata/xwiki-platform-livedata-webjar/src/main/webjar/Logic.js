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


define([], function () {

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
     * @param {String} layoutName The name of the layout to load with requireJS
     */
    Logic.prototype.loadLayout = function (layoutName) {
        var self = this;

        layoutName = layoutName || this.data.layouts["default"];
        if (layoutName === this.data.layouts.current) return;

        // load layout based on it's filename
        require([BASE_PATH + "layouts/" + layoutName + ".js"],

            // load success
            function (layout) {
                // remove current layout from the page
                if (self.data.layouts.current && self.layouts[self.data.layouts.current]) {
                    self.element.removeChild(self.layouts[self.data.layouts.current]);
                }
                // add layout element in loaded layouts list if not already loaded on the page
                if (!self.layouts[layoutName]) {
                    self.layouts[layoutName] = new layout(self.element);
                }
                // add new layout to the page
                self.element.appendChild(self.layouts[layoutName]);
                self.data.layouts.current = layoutName;

                // dispatch events
                var event = new Event("xwiki:livedata:layoutchange");
                self.element.dispatchEvent(event);

            },

            // load failure
            function (err) {
                // try to load default layout instead
                if (layoutName !== self.data.layouts["default"]) {
                    self.loadLayout(self.data.layouts["default"]);
                }
                else {
                    console.error(err);
                }
            }
        );
    };



    return init;

});



