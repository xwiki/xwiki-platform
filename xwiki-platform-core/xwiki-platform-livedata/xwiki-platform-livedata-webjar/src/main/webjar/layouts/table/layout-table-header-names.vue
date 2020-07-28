<template>
  <tr class="column-header">
    <!-- Entry Select All-->
    <th class="entry-selector">
      <livedata-entry-selector-all
        :logic="logic"
      ></livedata-entry-selector-all>
    </th>

    <th
      v-for="property in properties"
      :key="property.id"
      v-show="logic.isPropertyVisible(property.id)"
    >
      <div
        class="column-name"
        @click="logic.sort(property.id, 0)"
      >
        <span>{{ property.name }}</span>
        <span
          v-if="logic.isPropertySortable(property.id)"
          :class="[
            'sort-icon',
            'fa',
            { 'sorted': firstSortLevel.property === property.id },
            (firstSortLevel.property === property.id && firstSortLevel.descending) ? 'fa-caret-up' : 'fa-caret-down',
          ]"
        ></span>
      </div>
    </th>

  </tr>
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
  "vue!livedata-entry-selector-all",
], function (
  Vue
) {

  Vue.component("layout-table-header-names", {

    name: "layout-table-header-names",

    template: template,

    props: {
      logic: Object,
    },

    computed: {
      data: function () { return this.logic.data; },
      properties: function () { return this.logic.getDisplayablePropertyDescriptors(); },
      firstSortLevel: function () { return this.data.query.sort[0] || {}; },
    },

  });
});
</script>


<style>

.layout-table .column-name {
  display: flex;
  flex-direction: row;
  justify-content: space-between;
  align-items: center;
  cursor: pointer;
}

.layout-table .sort-icon {
  color: currentColor;
  opacity: 0;
}
.layout-table .sort-icon.sorted {
  opacity: 1;
}
.layout-table .column-name:hover .sort-icon:not(.sorted) {
  opacity: 0.5;
}

</style>
