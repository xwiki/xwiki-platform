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
  LayoutTable.vue is the main file for the Table layout component.
  It displays data formatted as a table, with properties as columns
  and entries as rows.
  It contains a header row containing LivedataFilter componenents for each column.
-->
<template>
  <div class="layout-table">

    <!--
      The layout Topbar
      Add common layout utilities, like the dropdown menu, the refresh button,
      and the pagination.
    -->
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


    <!-- Table layout root -->
    <table class="layout-table-root">

      <!--
        Table Header
        Implement quick sort, filter, and property reorder
      -->
      <!-- The style property is used to inject css-variables -->
      <thead
        :style="{
          '--property-count': properties.length,
        }"
      >
        <!-- Table property names -->
        <LayoutTableHeaderNames/>

        <!-- Table filters -->
        <LayoutTableHeaderFilters/>
      </thead>


      <!-- Table Body -->
      <tbody>
        <!-- The rows (= the entries) -->
        <LayoutTableRow
          v-for="entry in entries"
          :key="logic.getEntryId(entry)"
          :entry="entry"
        />

        <!-- Component to create a new entry -->
        <LayoutTableNewRow v-if="canAddEntry"/>
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
    properties () { return this.logic.getPropertyDescriptors() },
    entries () { return this.logic.data.data.entries; },
    canAddEntry () { return this.logic.canAddEntry(); },
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

/* Table responsive mode */
@media screen and (max-width: 767px) {

  /* FOR IE11 support */
  .layout-table thead {
    display: flex;
    flex-direction: row;
  }
  .layout-table thead tr {
    display: flex;
    flex-direction: column;
  }
  /*
   * Align the property name with the filter
   * 38px correspond to the height of the filters inputs
   * This is hard coded and not scalable, but I don't have better for IE :(
   */
  .layout-table thead tr th {
    height: 38px;
  }

  /* Align header titles with entry titles */
  .layout-table thead th.draggable-item {
    margin-left: -10px;
  }

  @supports (grid-auto-flow: column) {
    .layout-table thead {
      display: grid;
      grid-auto-flow: column;
      grid-template-columns: auto 1fr;
      grid-template-rows: repeat(var(--property-count), auto);
      grid-column-gap: 1rem;
    }

    .layout-table thead tr {
      display: contents;
      height: auto;
    }
    .layout-table thead tr th {
      height: auto;
    }
  }
}

</style>
