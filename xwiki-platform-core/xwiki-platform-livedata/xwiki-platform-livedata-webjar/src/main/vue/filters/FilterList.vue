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
  FilterList is a custom filter that allow to filter static lists
-->
<template>
  <!--
    We use the BaseSelect component from where we provide:
    - the possibles options (as a prop)
    - the title formt (as a slot)
    - options format (as a slot)
    Apply filter on change evebt (when an option is toggled)
  -->
  <BaseSelect
    class="filter-list"
    :options="options"
    :selected-values="values"
    :multiple="true"
    :sort="true"
    @change="applyFilter($event.toString())"
  >

    <!--
      Provide a title for the select
      List the selected options, or display "Select Values" if none is selected
    -->
    <template #title="data">
      <div v-if="data.selectedValues.length === 0">
        Select Values
      </div>
      <div v-else class="selected-enum">
        {{ data.selectedValues.join(", ") }}
      </div>
    </template>

    <!--
      Provide the options template for the select
      Display a checkbox along the option value
    -->
    <template #option="option">
      <input
        type="checkbox"
        :checked="option.checked"
        @click.stop="option.toggle(option.value)"
        tabindex="-1"
      />
      {{ option.label }}
    </template>

  </BaseSelect>
</template>


<script>
import filterMixin from "./filterMixin.js";
import BaseSelect from "../utilities/BaseSelect.vue";

export default {

  name: "filter-list",

  components: {
    BaseSelect,
  },

  // Add the filterMixin to get access to all the filters methods and computed properties inside this component
  mixins: [filterMixin],

  computed: {
    // The list of all the options available for the static list
    options () {
      return this.config.options;
    },

    // The filter value is String that contains an Array,
    // but if no Array is found, the string is parsed as a singleton
    // so that it always return an array
    values () {
      if (this.filterEntry.value) {
        return this.filterEntry.value.split(",");
      } else {
        return [];
      }
    },
  },

};
</script>


<style>

.livedata-filter.filter-list .selected-enum {
  overflow: hidden;
  text-overflow: ellipsis;
}

.livedata-filter.filter-list input[type="checkbox"] {
  margin-right: 1rem;
}

</style>
