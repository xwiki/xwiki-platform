<template>
  <tr class="column-filters">
    <th class="entry-selector"></th>
    <th
      v-for="property in properties"
      :key="property.id"
      v-show="logic.isPropertyVisible(property.id)"
    >
      <livedata-filter
        v-if="logic.isPropertyFilterable(property.id)"
        :property-id="property.id"
        :index="0"
      ></livedata-filter>
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
  "vue!filters/livedata-filter",
], function (
  Vue
) {

  Vue.component("layout-table-header-filters", {

    name: "layout-table-header-filters",

    template: template,

    inject: ["logic"],

    computed: {
      data: function () { return this.logic.data; },
      properties: function () { return this.logic.getPropertyDescriptors(); },
    },

  });
});
</script>


<style>

.layout-table .column-filters th {
  padding-left: 0;
  padding-right: 0;
  font-weight: normal;
  vertical-align: middle;
  width: 100vw;
}
.layout-table .column-filters th.entry-selector {
  width: 0;
}

.layout-table tbody > tr:first-child td {
  border-top: unset;
}

</style>
