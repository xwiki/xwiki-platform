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

<script>
import filterMixin from "./filterMixin.js";
import { loadById } from "../../services/require.js";
import XWikiLoader from "../utilities/XWikiLoader.vue";

export default {
  name: "filter-list",
  components: { XWikiLoader },
  // Add the filterMixin to get access to all the generic filter methods and computed properties inside this component.
  mixins: [filterMixin],

  inject: ["jQuery"],

  props: {
    isAdvanced: {
      type: Boolean,
      default: false,
    },
  },

  data() {
    return {
      isReady: false,
      selectizeLoaded: false,
    };
  },

  async created() {
    await loadById("xwiki-selectize");
    this.selectizeLoaded = true;
  },

  computed: {
    // Current value of the filter entry.
    value() {
      return this.filterEntry.value;
    },

    fullyReady() {
      return this.isReady && this.selectizeLoaded;
    },

    // Settings used when creating the selectize widget.
    selectizeSettings() {
      let options = this.config.options || [];
      // If the current filter has the empty operator and no existing option has an empty value, the default
      // empty option is added.
      // The empty option is not displayed in the advanced filtering panel nor when the empty operator is not available.
      if (
        !this.isAdvanced &&
        this.hasEmptyOperator &&
        this.filterEntry?.operator === "empty" &&
        !options.some((value) => value.value === "")
      ) {
        options.push(this.getDefaultEmptyOption());
      }
      const settings = {
        // Allow free text because we want to support the contains and startsWith operators.
        create: true,
        // Take the list of (initial) options from the filter configuration. This list will be extended with the results
        // obtained from the configured search URL.
        options,
        // Limit the selection to a single value because:
        // * selecting multiple values increases the height of the filter row when table layout is used
        // * constraint (filter) values should be strings; this isn't a limitation of the live data model, but using
        //   arrays or complex objects make it difficult to express the filter in the REST URL used to fetch the live
        //   data or in the live data macro parameters
        // * the user can still add more values by adding more constraints from the advanced filtering panel
        maxItems: 1,
        onChange: (value) => {
          if (this.$refs.input.selectize.items.length === 0) {
            // When no values are selected, simply remove the filter.
            this.removeFilter();
          } else if (value !== this.value) {
            // If the selected value has an empty value, then use the empty operator, otherwise use the default operator
            // Note that this imply that any filter list descriptor needs to have an empty operator defined.
            if (value === "") {
              this.applyFilter(value, "empty");
            } else if (this.isAdvanced) {
              // In the advanced filtering panel the operator is selected separately, so keep the current operator.
              this.applyFilter(value);
            } else {
              // In the top filter, selecting a value always uses the default operator. Passing it explicitly is
              // required so that switching away from the "empty" operator resets to the default operator instead of
              // keeping the "empty" operator (which would make the backend ignore the value).
              this.applyFilter(
                value,
                this.logic.getFilterDefaultOperator(this.propertyId),
              );
            }
          }
        },
      };
      if (this.config.searchURL) {
        Object.assign(settings, {
          load: this.getLoader({ limit: 10 }),
          loadSelected: this.getLoader({ exactMatch: true }),
        });
      }
      return settings;
    },

    isVisible() {
      // We do not show this component when the type of filter is 'Empty' and we are in the advanced filtering panel.
      return this.filterEntry.operator !== "empty" || !this.isAdvanced;
    },
    hasEmptyOperator() {
      return this.config.operators.some((it) => it.id === "empty");
    },
  },

  methods: {
    // Creates the function to fetch the suggestions using the given parameters.
    getLoader(searchParams) {
      return (text, callback) => {
        // TODO: Support multiple search URLs (sources). See suggestUsersAndGroups.js for an example.
        const searchURL = this.config.searchURL.replace(
          "{encodedQuery}",
          encodeURIComponent(text),
        );
        this.jQuery
          .getJSON(searchURL, searchParams)
          // eslint-disable-next-line promise/no-callback-in-promise
          .then((results) => callback(this.getResultsAdapter(results)))
          // eslint-disable-next-line promise/no-callback-in-promise
          .catch(() => callback(this.getResultsAdapter()));
      };
    },

    getResultsAdapter(results) {
      let adaptedResults = [];
      if (Array.isArray(results)) {
        adaptedResults = results;
      } else if (Array.isArray(results?.propertyValues)) {
        adaptedResults = results.propertyValues.map(this.getSuggestion);
      }

      // An empty option is automatically added to the results only when hasEmptyOperator is true, no empty
      // option is already found, and we are not in an advanced filter panel.
      if (
        !this.isAdvanced &&
        this.hasEmptyOperator &&
        !adaptedResults.some((value) => value.value === "")
      ) {
        adaptedResults.unshift(this.getDefaultEmptyOption());
      }

      return adaptedResults;
    },

    // Convert the fetched data to the format expected by the selectize widget.
    getSuggestion(propertyValue) {
      const metaData = propertyValue.metaData || propertyValue;
      return {
        value: propertyValue.value,
        label: metaData.label,
        icon: metaData.icon,
        url: metaData.url,
        hint: metaData.hint,
      };
    },

    getDefaultEmptyOption() {
      return {
        value: "",
        label: this.$t("livedata.filter.list.emptyLabel"),
      };
    },

    // Synchronize the selectize widget display with the current filter entry (value and operator). This is needed
    // because the filter entry can be updated outside of this widget (e.g., from the advanced filtering panel).
    syncSelectizeValue() {
      if (!this.$refs.input) {
        // The input might not be in the DOM yet (e.g., an external filter change while the widget is still loading).
        return;
      }
      const input = this.jQuery(this.$refs.input);
      let value = this.value;
      if (this.filterEntry?.operator === "empty") {
        // Make sure the empty option exists so the widget can display its label even when the operator was switched
        // to empty after the widget creation (e.g., from the advanced filtering panel).
        this.$refs.input.selectize?.addOption(this.getDefaultEmptyOption());
        // The empty string is ignored by default. We change the value to empty string plus a comma value separator to
        // take it into account.
        value = ",";
      }
      input.val(value).trigger("change");
    },
  },

  // Update the selectize widget whenever the filter entry changes.
  watch: {
    value() {
      this.syncSelectizeValue();
    },
    // The operator can be changed outside of this widget (e.g., from the advanced filtering panel), so the displayed
    // value must be kept consistent with the active operator.
    "filterEntry.operator"() {
      this.syncSelectizeValue();
    },
    async fullyReady(isReady) {
      if (isReady) {
        // It is important to wait for the next tick to be sure that the input reference is available in the dom, for
        // selectize to be able to enhance it.
        await this.$nextTick();
        this.jQuery(this.$refs.input).xwikiSelectize(this.selectizeSettings);
        // For non-empty operators, selectize picks up the initial value from the HTML input attribute. For the empty
        // operator, selectize ignores empty string values, so we must sync explicitly after widget creation.
        if (this.filterEntry?.operator === "empty") {
          this.syncSelectizeValue();
        }
      }
    },
  },

  // Create the selectize widget.
  async mounted() {
    // Wait for the translations to be loaded, otherwise the empty option label might be displayed untranslated.
    await this.logic.translationsLoaded();
    this.isReady = true;
  },

  // Destroy the selectize widget.
  beforeDestroyed() {
    this.$refs.input.selectize?.destroy();
  },
};
</script>

<!--
  Adds support for filtering list properties by suggesting property values.
-->
<template>
  <!-- A simple text input that will be enhanced by the selectize widget. -->
  <span v-if="isReady && selectizeLoaded" v-show="isVisible">
    <input
      :value="value"
      class="filter-list livedata-filter"
      ref="input"
      :aria-label="this.$t('livedata.filter.list.label')"
    />
  </span>
  <XWikiLoader v-else />
</template>

<style>
.livedata-filter.filter-list .ts-control {
  height: 100%;
  vertical-align: middle;
}
</style>
