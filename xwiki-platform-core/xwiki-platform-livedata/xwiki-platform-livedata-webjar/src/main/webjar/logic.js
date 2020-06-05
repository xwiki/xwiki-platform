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


define(["mockData"], function (mockData) {

    return {

        /**
         * The data object of the livedata that contains EVERYTHING
         * The data is accessible in the layouts
         * and is mutated with this logic methods
         */
        data: mockData,


        /**
         * Load a layout, or default layout if none specified
         * @param {String} layoutName The name of the layout to load with requireJS
         */
        loadLayout: function (layoutName) {
            var self = this;

            layoutName = layoutName || this.data.layouts["default"];
            if (layoutName === this.data.layouts.current) return;

            // load layout based on it's filename
            require([BASE_PATH + "layouts/" + layoutName + ".js"],
            // load success
            function (layout) {
                self.data.layouts.current = layoutName;

            },
            // load failure
            function (err) {
                if (layoutName === self.data.layouts["default"]) {
                    console.error(err);
                }
                else {
                    self.loadLayout(self.data.layouts["default"]);
                }
            });
        },


    };


});



