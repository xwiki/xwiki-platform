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
  "Vue",
  "vue!" + BASE_PATH + "top-bar.html",
], function (
  Vue,
  topbar
) {

  return function (logic) {

    /**
     * Create the top bar from Vuejs
     */
    var vueTopBar = new Vue({

      // Constructs a top bar component and passes it the data
      template: '<top-bar :logic="logic"></top-bar>',

      data: {
        logic: logic,
      },

    }).$mount();

    // return the HTML Element of the top bar for the layout
    return vueTopBar.$el;

  };


});
