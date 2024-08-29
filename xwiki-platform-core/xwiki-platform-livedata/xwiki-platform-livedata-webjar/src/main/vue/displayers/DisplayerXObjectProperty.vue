<!--
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
-->


<!--
  DisplayerXObjectProperty is a custom displayer that displays an XObject property.
  It fetches the edit and view widgets from the server.
-->
<template>
  <!-- Uses the BaseDisplayer as root element, as it handles for us all the displayer default behavior. -->
  <BaseDisplayer
    class="displayer-xobject-property"
    :property-id="propertyId"
    :entry="entry"
    :is-view.sync="isView"
    :is-loading="isLoading"
    @saveEdit="applyEdit">

    <!-- Provide the Html Viewer widget to the `viewer` slot -->
    <template #viewer>
      <div :class="['html-wrapper', isLoading ? 'disabled' : '']"
           v-html="sanitizeHtml(value)"
           ref="xObjectPropertyView"/>
    </template>

    <!-- Provide the Html Editor widget to the `editor` slot -->
    <template #editor>
      <div v-html="sanitizeHtml(editField)"
           ref="xObjectPropertyEdit"/>
    </template>
  </BaseDisplayer>
</template>


<script>

import displayerMixin from "./displayerMixin.js";
import BaseDisplayer from "./BaseDisplayer.vue";
import displayerStatesMixin from "./displayerStatesMixin.js";
import $ from "jquery";
import xObjectPropertyHelper from "xwiki-livedata-xObjectPropertyHelper";


export default {

  name: "displayer-xobject-property",

  inject: ["logic"],

  // Add the displayerMixin to get access to all the displayers methods and computed properties inside this component.
  mixins: [displayerMixin, displayerStatesMixin],

  props: ['timestamp'],

  components: {BaseDisplayer,},

  data() {
    return {
      editField: undefined,
    }
  },

  methods: {
    /**
     * Process the edit form content and send it to be saved.
     */
    applyEdit() {
      const documentName = this.logic.getEntryId(this.entry);
      if (!documentName) {
        new XWiki.widgets.Notification(this.$t('livedata.displayer.xObjectProperty.missingDocumentName.errorMessage'),
          'error');
      } else {
        $(document).trigger('xwiki:actions:beforeSave');
        const fields = $(this.$refs.xObjectPropertyEdit).find(':input').serializeArray();
        const className = this.data.query.source.className;

        const data = {};
        fields.forEach(field => {
          var newName = field.name;

          if (newName.startsWith(className)) {
            // Remove the class name and the object number in order to keep only the property name.
            newName = newName.substring(className.length);
            newName = newName.replace(/^_\d+_/, '');
          }

          // Aggregates the fields with the same name in an array. If a field is found only once it is stored alone.
          if (data[newName]) {
            if (!Array.isArray(data[newName])) {
              data[newName] = [data[newName]];
            }

            data[newName].push(field.value);
            
          } else {
            data[newName] = field.value;
          }
        });

        this.logic.getEditBus().save(this.entry, this.propertyId, data);
      }
    },

    /**
     * Takes an update method and retrieves its content.
     * 
     * @param {method} updateMethod the method dedicate to the update of a given aspect of the displayer. For instance,
     *  the view or edit html content
     * @returns {*} a `Promise` with the content of the updated view
     */
    update(updateMethod) {
      this.isLoading = true;
      const documentName = this.logic.getEntryId(this.entry);
      const className = this.data.query.source.className;
      const property = this.propertyId;
      return updateMethod(documentName, className, property);
    },

    /**
     * Update the content of the editor slot.
     */
    updateEdit() {
      this.update(xObjectPropertyHelper.edit)
        .then((html) => {
          this.isLoading = false;

          this.editField = html;

          // Wait for the rendering to be finished after editField is updated, to have access to  xObjectPropertyEdit
          // and be able to send the trigger event.
          this.$nextTick().then(() => {
            if (this.$refs.xObjectPropertyEdit) {
              $(document).trigger('xwiki:dom:updated', {'elements': [this.$refs.xObjectPropertyEdit]});
              // Focuses on the first visible field of the loaded form.
              $(this.$refs.xObjectPropertyEdit).find(':input').filter(':visible').focus();
            }
          })
        })
        .catch(() => {
          // Stop the loader and switch to view mode.
          this.isLoading = false;
          this.isView = true;
        })
    }
  },

  watch: {
    // Refreshes the edit field when the view mode switched to edit.
    isView: function(isView) {
      if (!isView) {
        this.updateEdit();
      }
    }
  },
  mounted() {
    // Pass the vue-i18n localization helper to the XObjectPropertyHelper to allow error messages to be localized.
    xObjectPropertyHelper.setLocalization(this.$t);
  }
};

</script>


<style>

</style>
