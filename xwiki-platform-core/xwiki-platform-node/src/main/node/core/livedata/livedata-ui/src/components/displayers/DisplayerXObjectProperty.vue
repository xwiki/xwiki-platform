<!--
  See the NOTICE file distributed with this work for additional
  information regarding copyright ownership.

  This is free software; you can redistribute it and/or modify it
  under the terms of the GNU Lesser General Public License as
  published by the Free Software Foundation; either version 2.1 of
  the License, or (at your option) any later version.

  This software is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
  Lesser General Public License for more details.

  You should have received a copy of the GNU Lesser General Public
  License along with this software; if not, write to the Free
  Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
  02110-1301 USA, or see the FSF site: http://www.fsf.org.
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
    v-model:is-view="isView"
    :is-loading="isLoading"
    @saveEdit="applyEdit"
  >
    <!-- Provide the Html Viewer widget to the `viewer` slot -->
    <template #viewer>
      <div
        :class="['html-wrapper', isLoading ? 'disabled' : '']"
        v-html="sanitizeHtml(value)"
        ref="xObjectPropertyView"
      />
    </template>

    <!-- Provide the Html Editor widget to the `editor` slot -->
    <template #editor>
      <div v-html="sanitizeHtml(editField)" ref="xObjectPropertyEdit" />
    </template>
  </BaseDisplayer>
</template>

<script>
import BaseDisplayer from "./BaseDisplayer.vue";
import displayerMixin from "./displayerMixin.js";
import displayerStatesMixin from "./displayerStatesMixin.js";
import { loadById } from "../../services/require.js";
import { XWiki } from "../../services/xwiki.js";
import { edit } from "../displayerXObjectPropertyHelper.js";

export default {
  name: "displayer-xobject-property",

  inject: ["logic", "jQuery"],

  // Add the displayerMixin to get access to all the displayers methods and computed properties inside this component.
  mixins: [displayerMixin, displayerStatesMixin],

  props: ["timestamp"],

  components: { BaseDisplayer },

  data() {
    return {
      editField: undefined,
    };
  },

  methods: {
    /**
     * Process the edit form content and send it to be saved.
     */
    applyEdit() {
      const documentName = this.logic.getEntryId(this.entry);
      if (!documentName && !this.entry._new) {
        new XWiki.widgets.Notification(
          this.$t(
            "livedata.displayer.xObjectProperty.missingDocumentName.errorMessage",
          ),
          "error",
        );
      } else {
        const $ = this.jQuery;
        $(document).trigger("xwiki:actions:beforeSave");
        const fields = $(this.$refs.xObjectPropertyEdit)
          .find(":input")
          .serializeArray();
        const className = this.getClassName();

        const data = {};
        fields.forEach((field) => {
          var newName = field.name;

          if (newName.startsWith(className)) {
            // Remove the class name and the object number in order to keep only the property name.
            newName = newName.substring(className.length);
            newName = newName.replace(/^_\d+_/, "");
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

    getClassName() {
      const classNameProperty = this.propertyId + "_class";
      if (classNameProperty in this.data.query.source) {
        return this.data.query.source[classNameProperty];
      } else {
        return this.data.query.source.className;
      }
    },

    /**
     * Takes an update method and retrieves its content.
     *
     * @param updateMethod - the method dedicate to the update of a given aspect of the displayer. For instance,the view
     * or edit html content
     * @returns a `Promise` with the content of the updated view
     */
    update(updateMethod) {
      this.isLoading = true;
      const documentName = this.logic.getEntryId(this.entry);
      const className = this.getClassName();
      const property = this.propertyId;
      return updateMethod(documentName, className, property);
    },

    /**
     * Update the content of the editor slot.
     */
    async updateEdit() {
      const editConfirmationPromise = loadById("xwiki-edit-confirmation");

      // Loads the edit field. If the server requires an edit confirmation it returns it as JSON with a 423 status
      // instead of the editor; in that case we display the confirmation modal and, if the user confirms, retry the
      // request forcing the confirmation so that the editor is returned.
      const loadEditField = async (extraParams) => {
        try {
          return await this.update((documentName, className, property) =>
            edit(documentName, className, property, extraParams),
          );
        } catch (error) {
          const editConfirmation = await editConfirmationPromise;
          // Re-throws the error if it's not an edit confirmation (423) response.
          const confirmation =
            editConfirmation.parseConfirmationResponse(error);
          await editConfirmation.showConfirmationModal(confirmation);
          const xcontext = await loadById("xwiki-meta");
          return loadEditField({ force: 1, force_token: xcontext.form_token });
        }
      };

      try {
        await this.applyEditField(await loadEditField());
      } catch (error) {
        this.handleEditFieldFailure(error, await editConfirmationPromise);
      }
    },

    /**
     * Display the loaded edit field in the editor slot and enhance it (DOM updated event, focus).
     *
     * @param html - the edit field HTML returned by the server
     */
    async applyEditField(html) {
      this.isLoading = false;
      this.editField = html;

      // Wait for the rendering to be finished after editField is updated, to have access to xObjectPropertyEdit
      // and be able to send the trigger event.
      await this.$nextTick();
      const $ = this.jQuery;
      if (this.$refs.xObjectPropertyEdit) {
        $(document).trigger("xwiki:dom:updated", {
          elements: [this.$refs.xObjectPropertyEdit],
        });
        // Focuses on the first visible field of the loaded form.
        $(this.$refs.xObjectPropertyEdit)
          .find(":input")
          .filter(":visible")
          .first()
          .focus();
      }
    },

    /**
     * Handle a failure to load the edit field by switching back to view mode and notifying the user, unless the user
     * simply dismissed the edit confirmation.
     *
     * @param error - the error that occurred while loading the edit field
     * @param editConfirmation - the edit confirmation module used to detect a dismissal
     */
    handleEditFieldFailure(error, editConfirmation) {
      // Notify the edit bus that the edition is canceled. Switching to edit mode (BaseDisplayer.setEdit) started the
      // edition on the bus, so it must be canceled here. Otherwise the Live Data keeps considering this property as
      // being edited, which disables the edit popover of every cell until the page is reloaded.
      this.logic.getEditBus().cancel(this.entry, this.propertyId);
      // Stop the loader and switch to view mode.
      this.isLoading = false;
      this.isView = true;
      // Notify the user of genuine failures, but not when they simply dismissed the edit confirmation.
      if (!editConfirmation.isConfirmationDismissed(error)) {
        new XWiki.widgets.Notification(
          this.$t(
            "livedata.displayer.xObjectProperty.failedToRetrieveField.errorMessage",
            ["edit"],
          ),
          "error",
        );
      }
    },
  },

  watch: {
    // Refreshes the edit field when the view mode switched to edit.
    isView: function (isView) {
      if (!isView) {
        this.updateEdit();
      }
    },
  },
};
</script>

<style></style>
