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
  <select :value="filterValue" class="xwiki-selectize livedata-selectize filter-boolean" ref="input"
    :aria-label="$t('livedata.filter.boolean.label')">
    <option value=""></option>
    <option :value="trueValue">{{ $t('livedata.displayer.boolean.true') }}</option>
    <option :value="falseValue">{{ $t('livedata.displayer.boolean.false') }}</option>
  </select>
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
      filterValue: undefined
    }
  },

  computed: {
    trueValue() {
      return Object.prototype.hasOwnProperty.call(this.config, 'trueValue') ? this.config.trueValue : 'true';
    },
    falseValue() {
      return Object.prototype.hasOwnProperty.call(this.config, 'falseValue') ? this.config.falseValue : 'false';
    }
  },

  watch: {
    filterValue(newValue, oldValue) {
      if (this.$refs.input.selectize.items.length === 0) {
        // When no values are selected, simply remove the filter.
        this.removeFilter();
      } else if (newValue !== oldValue) {
        $(this.$refs.input).val(newValue).trigger('change');
        this.applyFilter(newValue);
      }
    },
  },

  // Create the selectize widget.
  async mounted() {
    // Wait for the translations to be loaded, otherwise the true / false option label might be displayed untranslated.
    await this.logic.translationsLoaded();
    $(this.$refs.input).xwikiSelectize({
      onChange: value => {
        this.filterValue = value;
      },
    });
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
