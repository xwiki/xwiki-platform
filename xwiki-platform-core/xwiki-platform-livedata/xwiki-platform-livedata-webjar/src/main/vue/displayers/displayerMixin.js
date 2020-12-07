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

  inject: ["logic"],

  directives: {
    // This directive autofocus the element that has it
    // This can be usefull in order to autofocus the input in the Editor widget
    // right after the user switched from the Viewer widget
    autofocus: {
      inserted (el) { el.focus(); }
    },
  },

  props: {
    propertyId: String,
    entry: Object,
  },

  // The computed values provide common data needed by displayers
  computed: {
    // The value to be displayed
    value () {
      return this.entry[this.propertyId];
    },
    // The property descriptor of `this.propetyId`
    propertyDescriptor () {
      return this.logic.getPropertyDescriptor(this.propertyId);
    },
    // The configuration (aka displayerDescriptor) of the displayer
    config () {
      return this.logic.getDisplayerDescriptor(this.propertyId);
    },
    // The whole Livedata data object
    data () {
      return this.logic.data;
    },
  },

  methods: {
    // This method should be used to apply edit and go back to view mode
    // It validate the entered value, ensuring that is is valid for the server
    applyEdit (newValue) {
      this.logic.setValue({
        entry: this.entry,
        propertyId: this.propertyId,
        value: newValue
      });
      // Go back to view mode
      // (there might be a cleaner way to do this)
      this.$el.__vue__.view();
    },

    // This method should be used to cancel edit and go back to view mode
    // This is like applyEdit but it does not save the entered value
    cancelEdit () {
      // Go back to view mode
      // (there might be a cleaner way to do this)
      this.$el.__vue__.view();
    },

  },

};
