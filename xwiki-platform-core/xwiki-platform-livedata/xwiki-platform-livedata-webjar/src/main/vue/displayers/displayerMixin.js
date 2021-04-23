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


/**
 * The displayerMixin is a vue mixin containing all the needed
 * props, computed values, methods, etc. for any custom displayer:
 * `propertyId`, `entry`, `value`, `config`, `applyEdit()`, ...
 * It should be included in every custom displayer component
 */
export default {

  inject: ["logic", "editBus"],

  directives: {
    // Only used by the date displayer.
    onInserted: {
      inserted(el, binding) {
        const handler = binding.value;
        if (!(handler instanceof Function)) {
          return void console.warn(`Warning: v-on-inserted directive expects a function`);
        }
        handler();
      }
    },
    // This directive autofocus the element that has it
    // This can be useful in order to autofocus the input in the Editor widget
    // right after the user switched from the Viewer widget
    autofocus: {
      inserted(el) {
        el.focus();
      }
    },
  },

  props: {
    propertyId: String,
    entry: Object,
  },

  // The computed values provide common data needed by displayers
  computed: {
    // The value to be displayed
    value() {
      return this.entry[this.propertyId];
    },
    // The property descriptor of `this.propertyId`
    propertyDescriptor() {
      return this.logic.getPropertyDescriptor(this.propertyId);
    },
    // The configuration (aka displayerDescriptor) of the displayer
    config() {
      return this.logic.getDisplayerDescriptor(this.propertyId);
    },
    // The whole Livedata data object
    data() {
      return this.logic.data;
    },
  },

  methods: {
    genericSave() {
      this.editBus.save(this.entry, this.propertyId, [{[this.propertyId]: this.editedValue}])
    }
  }
};
