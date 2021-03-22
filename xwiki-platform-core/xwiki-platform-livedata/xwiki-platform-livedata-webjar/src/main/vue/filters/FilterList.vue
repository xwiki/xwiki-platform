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
  Adds support for filtering list properties by suggesting property values.
-->
<template>
  <!-- A simple text input that will be enhanced by the selectize widget. -->
  <input :value="value" class="filter-list" ref="input"/>
</template>

<script>
import filterMixin from "./filterMixin.js";
import $ from "jquery";
import "xwiki-selectize";

export default {

  name: "filter-list",

  // Add the filterMixin to get access to all the generic filter methods and computed properties inside this component.
  mixins: [filterMixin],

  computed: {
    // Current value of the filter entry.
    value () {
      return this.filterEntry.value;
    },

    // Settings used when creating the selectize widget.
    selectizeSettings () {
      const settings = {
        // Allow free text because we want to support the contains and startsWith operators.
        create: true,
        // Take the list of (initial) options from the filter configuration. This list will be extended with the results
        // obtained from the configured search URL.
        options: this.config.options,
        // Limit the selection to a single value because:
        // * selecting multiple values increases the height of the filter row when table layout is used
        // * constraint (filter) values should be strings; this isn't a limitation of the live data model, but using
        //   arrays or complex objects make it difficult to express the filter in the REST URL used to fetch the live
        //   data or in the live data macro parameters
        // * the user can still add more values by adding more constraints from the advanced filtering panel
        maxItems: 1,
        onChange: value => {
          if (value !== this.value) {
            this.applyFilter(value);
          }
        },
      };
      if (this.config.searchURL) {
        Object.assign(settings, {
          load: this.getLoader({"limit": 10}),
          loadSelected: this.getLoader({"exactMatch": true}),
        });
      }
      return settings;
    },
  },

  methods: {
    // Creates the function to fetch the suggestions using the given parameters.
    getLoader (searchParams) {
      return (text, callback) => {
        // TODO: Support multiple search URLs (sources). See suggestUsersAndGroups.js for an example.
        const searchURL = this.config.searchURL.replace('{encodedQuery}', encodeURIComponent(text));
        $.getJSON(searchURL, searchParams).then(this.getResultsAdapter()).done(callback).fail(callback);
      };
    },

    getResultsAdapter () {
      return results => {
        if (Array.isArray(results)) {
          return results;
        } else if (Array.isArray(results?.propertyValues)) {
          return results.propertyValues.map(this.getSuggestion);
        } else {
          return [];
        }
      };
    },

    // Convert the fetched data to the format expected by the selectize widget.
    getSuggestion (propertyValue) {
      const metaData = propertyValue.metaData || propertyValue;
      return {
        value: propertyValue.value,
        label: metaData.label,
        icon: metaData.icon,
        url: metaData.url,
        hint: metaData.hint,
      };
    },
  },

  // Update the selectize widget whenever the filter value changes.
  watch: {
    value (newValue) {
      $(this.$refs.input).val(newValue).trigger('change');
    },
  },

  // Create the selectize widget.
  mounted () {
    $(this.$refs.input).xwikiSelectize(this.selectizeSettings);
  },

  // Destroy the selectize widget.
  beforeDestroyed () {
    this.$refs.input.selectize?.destroy();
  },
};
</script>

<style>
.livedata-filter.filter-list .selectize-input {
  height: 100%;
  vertical-align: middle;
}
</style>
