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
  "vue!" + BASE_PATH + "layouts/livedata-cards.html",
  "Logic"
], function (
  Vue,
  livedataCards,
  Logic
) {

  return function (element) {
    var logic = Logic(element);

    // vue directive to automatically create and insert the displayer inside the element
    Vue.directive("displayer", {
      bind: function (el, binding) {
        logic.createDisplayer(binding.value.property.id, binding.value.entry)
        .done(function (displayer) {
          el.appendChild(displayer.element);
        });
      },
    });

    /**
     * Create the cards layout from Vuejs
     */
    var vueCardsLayout = new Vue({

      // Constructs a livedata-cards component and passes it the data
      template: '<livedata-cards :logic="logic"></livedata-cards>',

      data: {
        logic: logic,
      },

    }).$mount();

    // return the HTML Element of the layout for the logic script
    return vueCardsLayout.$el;

  };


});
