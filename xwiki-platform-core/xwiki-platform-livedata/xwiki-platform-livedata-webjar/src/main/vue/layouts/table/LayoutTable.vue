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
  <div class="layout-table">

    <!-- Topbar -->
    <LivedataTopbar>
      <template #left>
        <LivedataDropdownMenu/>
        <LivedataRefreshButton/>
      </template>
      <template #right>
        <LivedataPagination/>
      </template>
    </LivedataTopbar>

    <!-- Entry selector info bar -->
    <LivedataEntrySelectorInfoBar/>


    <!-- Table component -->
    <table class="livedata-table">

      <!-- Table Header -->
      <thead>

        <!-- Table property names -->
        <tr is="LayoutTableHeaderNames"></tr>

        <!-- Table filters -->
        <tr is="LayoutTableHeaderFilters"></tr>

      </thead>


      <!-- Table Body -->
      <tbody>

        <!-- Table row -->
        <tr
          is="LayoutTableRow"
          v-for="entry in entries"
          :key="logic.getEntryId(entry)"
          :entry="entry"
        ></tr>

        <tr is="LayoutTableNewRow"></tr>

      </tbody>

    </table>

  </div>
</template>


<script>
import LivedataTopbar from "../../LivedataTopbar.vue";
import LivedataDropdownMenu from "../../LivedataDropdownMenu.vue";
import LivedataRefreshButton from "../../LivedataRefreshButton.vue";
import LivedataPagination from "../../LivedataPagination.vue";
import LivedataEntrySelectorInfoBar from "../../LivedataEntrySelectorInfoBar.vue";
import LayoutTableHeaderNames from "./LayoutTableHeaderNames.vue";
import LayoutTableHeaderFilters from "./LayoutTableHeaderFilters.vue";
import LayoutTableRow from "./LayoutTableRow.vue";
import LayoutTableNewRow from "./LayoutTableNewRow.vue";

export default {

  name: "layout-table",

  components: {
    LivedataTopbar,
    LivedataDropdownMenu,
    LivedataRefreshButton,
    LivedataPagination,
    LivedataEntrySelectorInfoBar,
    LayoutTableHeaderNames,
    LayoutTableHeaderFilters,
    LayoutTableRow,
    LayoutTableNewRow,
  },

  inject: ["logic"],

  computed: {
    data () { return this.logic.data; },
    entries () { return this.logic.data.data.entries; },
  },

};
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
