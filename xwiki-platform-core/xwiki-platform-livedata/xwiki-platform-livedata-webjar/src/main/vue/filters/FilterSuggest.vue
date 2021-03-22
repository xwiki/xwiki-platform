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
  },

  // Watch for filter entry value changes
  // When any, update the suggest picker to match corresponding value
  watch: {
    value (newValue, oldValue) {
      $(this.$refs.filterSuggest).val(newValue).trigger('change');
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

.livedata-filter.filter-suggest .selectize-input {
  height: 100%;
  vertical-align: middle;
}

</style>
