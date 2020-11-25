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
  DateFilter is a custom filter that allow to filter list,
  by giving suggestion on what the user can write
-->
<template>
  <!-- A simple text input that will be upgraded to selectize picker -->
  <input
    :value="filterEntry.value"
    class="filter-suggest"
    ref="filterSuggest"
  />
</template>


<script>
import filterMixin from "./filterMixin.js";
import $ from "jquery";
import xwikiSelectize from "xwiki-selectize";

export default {

  name: "filter-suggest",

  // Add the filterMixin to get access to all the filters methods and computed properties inside this component
  mixins: [filterMixin],


  computed: {
    // Current value of the filter entry
    value () {
      return this.filterEntry.value;
    },

    // Settings used in xwiki.selectize jquery plugin
    selectizeSettings () {
      const self = this;
      return {
        create: true,
        load: self.getLoad(text => ({ "fp": text, "limit": 10 })),
        loadSelected: self.getLoad(text => ({ "fp": text, "exactMatch": true })),
        onChange: value => {
          if (value !== self.value) {
            self.applyFilter(value)
          }
        },
      };
    },
  },

  methods: {
    // Transform fetch data to xwiki.selectize parsable data
    getSuggestion (propertyValue) {
      const metaData = propertyValue.metaData || {};
      return {
        value: propertyValue.value,
        label: metaData.label,
        icon: metaData.icon,
        url: metaData.url,
        hint: metaData.hint,
      };
    },

    // Create function to load options
    getLoad (getOptions) {
      return (text, callback) => {
        $.getJSON(this.config.url, getOptions(text)).then(response => {
          if (response && Array.isArray(response.propertyValues)) {
            return response.propertyValues.map(this.getSuggestion);
          } else {
            return [];
          }
        }).done(callback).fail(callback);
      };
    },

    // Taken from xwiki-platform-web/src/main/webapp/resources/uicomponents/suggest/xwiki.selectize.js
    // Customized to work for suggest filter when it needs to be updated
    // from changes from another suggest filter
    loadSelectedValues (values) {
      const selectize = this.$refs.filterSuggest.selectize;
      const wrapper = selectize.$wrapper;
      wrapper.addClass(selectize.settings.loadingClass);
      selectize.loading++;
      values.reduce((deferred, value) => {
        return deferred.then(() => {
          return this.loadSelectedValue(value);
        });
      }, $.Deferred().resolve()).always(() => {
        selectize.loading = Math.max(selectize.loading - 1, 0);
        if (!selectize.loading) {
          wrapper.removeClass(selectize.settings.loadingClass);
        }
      });
    },

    loadSelectedValue (value) {
      const selectize = this.$refs.filterSuggest.selectize;
      const deferred = $.Deferred();
      let load;
      if (typeof selectize.settings.loadSelected === "function") {
        load = selectize.settings.loadSelected;
      } else {
        load = selectize.settings.load;
      }
      if (value && typeof load === "function") {
        load.call(selectize, value, function (options) {
          $.isArray(options) && options.forEach(function (option) {
            const value = option[selectize.settings.valueField];
            if (selectize.options.hasOwnProperty(value)) {
              selectize.updateOption(value, option);
            } else {
              selectize.addOption(option);
            }
          });
          deferred.resolve();
        });
      } else {
        deferred.resolve();
      }
      return deferred.promise();
    },
  },

  // Watch for filter entry value changes
  // When any, update the suggest picker to match corresponding value
  watch: {
    value (newValue, oldValue) {
      if (newValue === oldValue) { return; }
      const selectize = this.$refs.filterSuggest.selectize;
      const valueArray = newValue.split(",");
      // Update selectize plugin if needed
      if (selectize.getValue() === newValue) { return; }
      // Clear all items and add all new items
      selectize.clear(true);
      valueArray.forEach((val) => {
        if (selectize.options.hasOwnProperty(val)) {
          selectize.addItem(val, true);
        } else {
          selectize.createItem(val, false);
        }
      });
      // Load options for selected values
      this.loadSelectedValues(valueArray);
    },
  },

  mounted () {
    $(this.$refs.filterSuggest).xwikiSelectize(this.selectizeSettings);
  },

  beforeDestroyed () {
    if (this.$refs.filterSuggest.selectize) {
      this.$refs.filterSuggest.selectize.destroy();
    }
  },


};
</script>


<style>

</style>
