<template>
  <div class="layout-table">

    <!-- Topbar -->
    <livedata-topbar :logic="logic">
      <template #left>
        <livedata-dropdown-menu :logic="logic"></livedata-dropdown-menu>
      </template>
      <template #right>
        <livedata-pagination :logic="logic"></livedata-pagination>
      </template>
    </livedata-topbar>

    <!-- Entry selector info bar -->
    <livedata-entry-selector-info-bar
      :logic="logic"
    ></livedata-entry-selector-info-bar>


    <!-- Table component -->
    <table class="livedata-table">

      <!-- Table Header -->
      <thead>

        <!-- Table property names -->
        <tr
          is="layout-table-header-names"
          :logic="logic"
        ></tr>

        <!-- Table filters -->
        <tr
          is="layout-table-header-filters"
          :logic="logic"
        ></tr>

      </thead>


      <!-- Table Body -->
      <tbody>

        <!-- Table row -->
        <tr
          is="layout-table-row"
          v-for="entry in entries"
          :key="logic.getEntryId(entry)"
          :entry=entry
          :logic="logic"
        ></tr>
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
  "vue!livedata-topbar",
  "vue!livedata-dropdown-menu",
  "vue!livedata-pagination",
  "vue!livedata-entry-selector-info-bar",
  "vue!layouts/table/layout-table-header-names",
  "vue!layouts/table/layout-table-header-filters",
  "vue!layouts/table/layout-table-row",
], function (
  Vue
) {

  Vue.component("layout-table", {

    name: "layout-table",

    template: template,

    props: {
      logic: Object,
    },

    computed: {
      data: function () { return this.logic.data; },
      entries: function () { return this.logic.data.data.entries; },
    },

  });
});
</script>


<style>

.layout-table table {
  height: 100%;
}

.layout-table th {
  border-bottom: unset;
}

.layout-table .livedata-entry-selector-all .btn {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  padding-left: 2rem;
}

</style>
