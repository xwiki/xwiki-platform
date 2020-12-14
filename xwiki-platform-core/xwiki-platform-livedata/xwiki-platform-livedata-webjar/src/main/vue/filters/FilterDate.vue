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
  DateFilter is a custom filter that allow to filter dates
  It could either evaluate a single date (equals, not equals, before, after, ...)
  or match a date range
-->
<template>
  <!-- A simple text input that will be upgraded to have a date picker -->
  <input
    class="filter-date"
    ref="filterDate"
    type="text"
    size="1"
    :value="filterEntry.value"
  />
</template>


<script>
import filterMixin from "./filterMixin.js";
import daterangepicker from "daterangepicker";
import moment from "moment";
import $ from "jquery";

export default {

  name: "filter-date",

  // Add the filterMixin to get access to all the filters methods and computed properties inside this component
  mixins: [filterMixin],


  computed: {

    operator () {
      return this.filterEntry.operator;
    },

    format () {
      return "YYYY/MM/DD HH:mm";
    },

    ranges () {
      return {
        "Today":
          [moment().startOf('day'), moment().endOf('day')],
        "Yesterday":
          [moment().subtract(1, 'days').startOf('day'), moment().subtract(1, 'days').endOf('day')],
        "Last 7 days":
          [moment().subtract(6, 'days').startOf('day'), moment().endOf('day')],
        "Last 30 days":
          [moment().subtract(29, 'days').startOf('day'), moment().endOf('day')],
        "This month":
          [moment().startOf('month'), moment().endOf('month')],
        "Last month":
          [moment().subtract(1, 'month').startOf('month'), moment().subtract(1, 'month').endOf('month')],
      };
    },

    defaultFilterConfig () {
      return {
        drops: 'down',
        opens: 'right',
        autoUpdateInput: false,
        autoApply: false,
        singleDatePicker: true,
        showDropdowns: true,
        timePicker: true,
        timePicker24Hour: true,
        showCustomRangeLabel: true,
        alwaysShowCalendars: true,
        locale: {
          format: this.format,
        },
      };
    },

    filterConfig () {
      if (this.operator === "isBetween") {
        return Object.assign({}, this.defaultFilterConfig, {
          singleDatePicker: false,
          ranges: this.ranges,
        });
      } else {
        return Object.assign({}, this.defaultFilterConfig);
      }
    },

  },

  methods: {
    getValue (daterangepicker) {
      if (this.operator === "isBetween") {
        return `${daterangepicker.startDate.valueOf()}-${daterangepicker.endDate.valueOf()}`
      } else {
        return daterangepicker.startDate.format(this.format);
      }
    },

    applyDate () {
      const daterangepicker = $(this.$refs.filterDate).data("daterangepicker");
      this.applyFilter(this.getValue(daterangepicker));
    },
  },

  mounted () {
    this.$watch("filterConfig",
      () => {
        const filterDate = this.$refs.filterDate;
        // Create the date range picker associated to the single date input
        $(filterDate).daterangepicker(
          this.filterConfig,
        );
        $(filterDate).on('apply.daterangepicker', (e) => {
            this.applyDate();
        });
        // Fix prototypejs prototype pollution
        $(filterDate).on('hide.daterangepicker', (e) => {
          // Overwrite at instance level the 'hide' function added by Prototype.js to the Element prototype.
          // This removes the 'hide' function only for the event target.
          filterDate.hide = undefined;
          // Restore the 'hide' function after the event is handled (i.e. after all the listeners have been called).
          setTimeout(function () {
            // This deletes the local 'hide' key from the instance, making the 'hide' inherited from the prototype
            // visible again (the next calls to 'hide' won't find the key on the instance and thus it will go up
            // the prototype chain).
            delete e.target['hide'];
          }, 0);
        });
      },
      { immediate: true },
    );
  },

};
</script>


<style>

</style>
