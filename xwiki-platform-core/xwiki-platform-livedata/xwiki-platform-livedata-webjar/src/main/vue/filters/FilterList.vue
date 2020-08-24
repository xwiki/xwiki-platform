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

<template>
  <BaseSelect
    class="filter-list"
    :options="options"
    :selected="values"
    :multiple="true"
    :sort="true"
    @change="changeHandler"
  >

    <template #title=data>
      <div v-if="data.selected.length === 0">
        Select Values
      </div>
      <div v-else class="selected-enum">
        {{ data.selected.join(", ") }}
      </div>
    </template>

    <template #option=option>
      <input
        type="checkbox"
        :checked="option.checked"
        @click.stop="option.toggle(option.value)"
        tabindex="-1"
      />
      {{ option.value }}
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

  mixins: [filterMixin],

  computed: {
    options () {
      return this.config.options;
    },

    values () {
      try {
         const values = JSON.parse(this.filterEntry.value || "[]");
        if (values instanceof Array) {
          return values;
        } else {
          return [values];
        }
      } catch (err) {
        console.warn(err);
        return [];
      }
    },
  },

  methods: {
    changeHandler (selected) {
      this.logic.filter(this.propertyId, this.index, {value: JSON.stringify(selected)});
    },
  },


};
</script>


<style>

.filter-list .selected-enum {
  overflow: hidden;
  text-overflow: ellipsis;
}

.filter-list input[type="checkbox"] {
  margin-right: 1rem;
}

</style>
