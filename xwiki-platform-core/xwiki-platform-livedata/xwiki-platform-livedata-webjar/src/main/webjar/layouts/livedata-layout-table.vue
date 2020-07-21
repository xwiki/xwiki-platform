<template>
  <div class="livedata-layout-table">

    <livedata-topbar :logic="logic"></livedata-topbar>

    <!-- Table component -->
    <table class="livedata-table">

      <!-- Table Header -->
      <thead>

        <!-- Column name -->
        <tr class="column-header">
          <!-- Entry Select All-->
          <th class="entry-selector"></th>
          <th
            v-for="col in cols"
            :key="col.id"
          >
            <div
              class="column-name"
              @click="logic.sort(col.id, 0)"
            >
              <span>{{ col.name }}</span>
              <span
                v-if="logic.isPropertySortable(col.id)"
                :class="[
                  'sort-icon',
                  'fa',
                  { 'sorted': sortLevel1.property === col.id },
                  (sortLevel1.property === col.id && sortLevel1.descending) ? 'fa-caret-up' : 'fa-caret-down',
                ]"
              ></span>
            </div>
          </th>
        </tr>

        <!-- Column filter -->
        <tr class="column-filters">
          <th class="entry-selector"></th>
          <th
            v-for="col in cols"
            :key="col.id"
          >
            <livedata-filter
              v-if="logic.isPropertyFilterable(col.id)"
              :property-id="col.id"
              :index="0"
              :logic="logic"
            ></livedata-filter>
          </th>
        </tr>

      </thead>


      <!-- Table Body -->
      <tbody>
        <tr
          v-for="(row, rowId) in rows"
          :key="rowId"
        >

          <!-- Entry Select All-->
          <td class="entry-selector">
            <livedata-entry-selector
              :entry="row"
              :logic="logic"
            ></livedata-entry-selector>
          </td>

          <td
            class="cell"
            v-for="col in cols"
            :key="col.id"
          >
            <livedata-displayer
              :property-id="col.id"
              :entry="row"
              :logic="logic"
            ></livedata-displayer>
          </td>

        </tr>
      </tbody>

    </table>

  </div>
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
  "vue!" + BASE_PATH + "livedata-topbar.vue",
  "vue!" + BASE_PATH + "/displayers/livedata-displayer.vue",
  "vue!" + BASE_PATH + "/filters/livedata-filter.vue",
  "vue!" + BASE_PATH + "/livedata-entry-selector.vue",
], function (
  Vue
) {

  Vue.component("livedata-layout-table", {

    name: "livedata-layout-table",

    template: template,

    props: {
      logic: Object,
    },

    computed: {
      data: function () { return this.logic.data; },
      rows: function () { return this.logic.data.data.entries; },
      cols: function () { return this.logic.data.meta.propertyDescriptors; },
      sortLevel1: function () { return this.data.query.sort[0] || {}; },
    },

  });
});
</script>


<style>

.livedata-layout-table table {
  height: 100%;
}

.livedata-layout-table .column-name {
  display: flex;
  flex-direction: row;
  justify-content: space-between;
  align-items: center;
  cursor: pointer;
}

.livedata-layout-table .sort-icon {
  color: currentColor;
  opacity: 0;
}
.livedata-layout-table .sort-icon.sorted {
  opacity: 1;
}
.livedata-layout-table .column-name:hover .sort-icon:not(.sorted) {
  opacity: 0.5;
}

.livedata-layout-table .column-filters th {
  padding-left: 0;
  padding-right: 0;
  font-weight: normal;
}
.livedata-layout-table .column-filters input {
  padding-left: 8px;
  padding-right: 8px;
  width: 100%;
}

.livedata-layout-table .entry-selector {
    padding: 0;
    height: 100%;
}

.livedata-layout-table .cell {
  padding: 0;
  height: 100%;
}
.livedata-layout-table .cell .livedata-displayer.view {
  padding: 8px;
}


</style>
