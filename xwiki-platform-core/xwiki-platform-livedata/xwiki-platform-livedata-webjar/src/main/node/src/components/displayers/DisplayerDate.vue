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
  DisplayerDate is a custom displayer that displays the entry value as dates
-->
<template>
  <!--
    Uses the BaseDisplayer as root element, as it handles for us
    all the displayer default behavior
  -->
  <BaseDisplayer
    ref="baseDisplayer"
    class="displayer-date"
    :property-id="propertyId"
    :entry="entry"
    v-model:is-view="isView"
    @saveEdit="genericSave"
    v-if="moment !== null"
  >

    <!-- Provide the Date Viewer widget to the `viewer` slot -->
    <template #viewer>
      <div class="displayed-date">{{ valueFormatted }}</div>
    </template>

    <!-- Provide the Date Editor widget to the `editor` slot -->
    <template #editor>
      <!-- A simple text input that will be upgraded to have a date picker -->
      <input
        class="editor-date"
        ref="editorDate"
        type="text"
        size="1"
        :value="valueFormatted"
        v-on-inserted="upgradeDatePicker"
        v-autofocus
        v-if="daterangepicker"
      />
    </template>

  </BaseDisplayer>
</template>


<script>
import displayerMixin from "./displayerMixin.js";
import displayerStatesMixin from "./displayerStatesMixin";
import BaseDisplayer from "./BaseDisplayer.vue";
import { loadById } from "@/services/require.js";

export default {

  name: "displayer-date",

  inject: ["jQuery"],

  data() {
    return {
      moment: null,
      daterangepicker: false,
    };
  },

  async mounted() {
    this.moment = await loadById("moment");
    await loadById("daterangepicker");
    this.daterangepicker = true;
  },

  components: {
    BaseDisplayer,
  },

  props: {
    format: {
      type: String,
      default: "YYYY/MM/DD HH:mm",
    },
  },

  // Add the displayerMixin to get access to all the displayers methods and computed properties inside this component
  mixins: [displayerMixin, displayerStatesMixin],

  computed: {
    // Date formatted to be human-readable
    valueFormatted() {
      return this.moment(+this.value).format(this.format);
    },

    editorConfig() {
      return {
        drops: "down",
        opens: "right",
        autoUpdateInput: true,
        autoApply: true,
        singleDatePicker: true,
        showDropdowns: true,
        timePicker: true,
        timePicker24Hour: true,
        alwaysShowCalendars: true,
        locale: {
          format: this.format,
        },
      };
    },
  },

  methods: {
    async upgradeDatePicker() {
      // Create the date picker in edit mode
      if (this.$refs.baseDisplayer.isView) {
        return;
      }
      await this.$nextTick();
      const editorDate = this.$refs.editorDate;
      // Create the date range picker associated to the single date input
      const $ = this.jQuery;
      $(editorDate).daterangepicker(
        this.editorConfig,
      );
      $(editorDate).on("apply.daterangepicker", () => {
        this.applyDate();
      });
      $(editorDate).on("cancel.daterangepicker", () => {
        this.cancelEdit();
      });
      $(editorDate).on("hide.daterangepicker", () => {
        this.cancelEdit();
      });
      // Fix prototypejs prototype pollution
      $(editorDate).on("hide.daterangepicker", (e) => {
        // Overwrite at instance level the 'hide' function added by Prototype.js to the Element prototype.
        // This removes the 'hide' function only for the event target.
        editorDate.hide = undefined;
        // Restore the 'hide' function after the event is handled (i.e. after all the listeners have been called).
        setTimeout(function() {
          // This deletes the local 'hide' key from the instance, making the 'hide' inherited from the prototype
          // visible again (the next calls to 'hide' won't find the key on the instance and thus it will go up
          // the prototype chain).
          delete e.target["hide"];
        }, 0);
      });
      // Open picker automatically
      await this.$nextTick();
      $(editorDate).data("daterangepicker").show();
    },

    getValue(daterangepicker) {
      return daterangepicker.startDate.format(this.format);
    },

    applyDate() {
      const $ = this.jQuery;
      const daterangepicker = $(this.$refs.editorDate).data("daterangepicker");
      const value = this.getValue(daterangepicker);
      const valueTimestamps = +this.moment(value, this.format);
      this.applyEdit(valueTimestamps);
    },
  },
};
</script>


<style>

.livedata-displayer .editor-date {
  width: 100%;
}

.displayed-date {
  min-width: 100%;
  min-height: 100%;
}

</style>
