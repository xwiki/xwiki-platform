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

import * as DOMPurify from 'dompurify';

/**
 * The displayerMixin is a vue mixin containing all the needed
 * props, computed values, methods, etc. for any custom displayer:
 * `propertyId`, `entry`, `value`, `config`, `applyEdit()`, ...
 * It should be included in every custom displayer component
 */
export default {

  inject: ["logic"],

  directives: {
    // Only used by the date displayer.
    onInserted: {
      inserted (el, binding) {
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
    safeValue() {
      return this.sanitizeHtml(this.value)
    },
    // The property descriptor of `this.propertyId`
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
    // The base value uses the value provided in the props the initial value of the form input.
    // Once the form is edited, `this.editedValue` is defined and is used instead.
    // This is needed in order to have a initial value (this.value) computed by `displayerMixing` while 
    // being able to bind the edited value of the input tag in the template to a data attribute (editedValue) that will
    // be updated at runtime without changing the initial `this.value`.
    baseValue: {
      get() {
        return this.editedValue || this.value;
      },
      set(value) {
        this.editedValue = value;
      }
    },
    // Checks if the property value is allowed to be edited and if the livedata is in a state where the displayer can
    // be edited.
    isEditable() {
      const editable = this.logic.isEditable({
        entry: this.entry,
        propertyId: this.propertyId,
      });
      // Checks that no other property is currently being edited.
      const noOtherEditing = this.logic.getEditBus().isEditable()
      return editable && noOtherEditing;
    }
  },

  methods: {
    /**
     * Generic save operation.
     * 
     * @param value of a value is provided, it is used for saving the property, otherwise `this.editedValue` is used
     */
    genericSave(value) {
      const savedValue =  value || this.editedValue;
      this.logic.getEditBus().save(this.entry, this.propertyId, {[this.propertyId]: savedValue})
    },
    sanitizeHtml(value) {
      if (!this.logic.isContentTrusted()) {
        // TODO: Take into account xml.htmlElementSanitizer properties when sanitizing (see XWIKI-20249).
        return DOMPurify.sanitize(value);
      } else {
        return value;
      }
    },
    sanitizeUrl(url, subtitute) {
      // TODO: Take into account xml.htmlElementSanitizer properties when sanitizing (see XWIKI-20249).
      if (this.logic.isContentTrusted() || DOMPurify.isValidAttribute('a', 'href', url)) {
        return url;
      } else {
        return (subtitute || '#');
      }
    }
  },
  
  data() {
    return {
      editedValue: undefined
    }
  },

  watch: {
    isView: function(newIsView) {
      if (newIsView) {
        // When we switch back to view mode, the edited value is reset.
        this.editedValue = undefined;
      }
    }
  }
};
