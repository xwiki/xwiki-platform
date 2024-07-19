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


<!-- BooleanFilter is a custom filter that allow to filter boolean values. -->
<template>
  <span v-if="isReady">
    <select :value="filterEntry.value" class="xwiki-selectize livedata-selectize filter-boolean" ref="input"
      :aria-label="$t('livedata.filter.boolean.label')">
      <option value=""></option>
      <option :value="trueValue">{{ $t('livedata.displayer.boolean.true') }}</option>
      <option :value="falseValue">{{ $t('livedata.displayer.boolean.false') }}</option>
    </select>
  </span>
</template>


<script>
import filterMixin from "./filterMixin.js";
import $ from "jquery";

export default {
  name: "filter-boolean",

  // Add the filterMixin to get access to all the filters methods and computed properties inside this component
  mixins: [filterMixin],

  data() {
    return {
      isReady: false
    }
  },

  computed: {
    // Current value of the filter entry.
    value () {
      return this.filterEntry.value;
    },
    trueValue() {
      return Object.prototype.hasOwnProperty.call(this.config, 'trueValue') ? this.config.trueValue : 'true';
    },
    falseValue() {
      return Object.prototype.hasOwnProperty.call(this.config, 'falseValue') ? this.config.falseValue : 'false';
    },
    // Settings used when creating the selectize widget.
    selectizeSettings () {
      let options = this.config.options || [];
      return  {
        // Allow free text because we want to support the contains and startsWith operators.
        create: false,
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
        onChange: value => {
          if (this.$refs.input.selectize.items.length === 0) {
            // When no values are selected, simply remove the filter.
            this.removeFilter();
          } else if (value !== this.value) {
            this.applyFilter(value);
          }
        },
      };
    }
  },

  watch: {
    value (newValue) {
      $(this.$refs.input).val(newValue).trigger('change');
    }
  },

  // Create the selectize widget.
  async mounted() {
    // Wait for the translations to be loaded, otherwise the true / false option label might be displayed untranslated.
    await this.logic.translationsLoaded();
    this.isReady = true;
    // It is important to wait for the next tick to be sure that the input reference is available in the dom, for
    // selectize to be able to enhance it.
    await this.$nextTick();
    $(this.$refs.input).xwikiSelectize(this.selectizeSettings);
  },

  // Destroy the selectize widget.
  beforeDestroyed() {
    this.$refs.input.selectize?.destroy();
  },

}
</script>

<style>
.livedata-filter.filter-boolean .selectize-input {
  height: 100%;
  vertical-align: middle;
}
</style>
