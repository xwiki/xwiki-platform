<template>
  <base-select
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
        @click.stop
        tabindex="-1"
      />
      {{ option.value }}
    </template>

  </base-select>
</template>


<script>
/*
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
 */
define([
  "Vue",
  "filters/filter-mixin",
  "vue!utilities/base-select",
], function (
  Vue,
  filterMixin
) {

  Vue.component("filter-list", {

    name: "filter-list",

    template: template,

    mixins: [filterMixin],

    computed: {
      options: function () {
        return this.config.options;
      },

      values: function () {
        return JSON.parse(this.filterEntry.value || "[]");
      },
    },

    methods: {
      changeHandler: function (selected) {
        this.logic.filter(this.propertyId, this.index, {value: JSON.stringify(selected)});
      },
    },


  });

});
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
