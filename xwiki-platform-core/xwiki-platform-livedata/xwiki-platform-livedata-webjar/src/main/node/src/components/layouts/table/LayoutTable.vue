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
        <LivedataPagination side="left" />
      </template>
      <template #right>
        <LivedataPagination side="right" />
        <LivedataDropdownMenu />
      </template>
    </LivedataTopbar>

    <!-- Entry selector info bar -->
    <LivedataEntrySelectorInfoBar />

    <!-- Loading bar -->
    <LayoutLoader />

    <!-- Table layout root -->
    <div class="layout-table-wrapper">
      <table class="layout-table-root responsive-table">

        <!--
          Table Header
          Implement quick sort, filter, and property reorder
        -->
        <thead>
        <!-- Table property names -->
        <LayoutTableHeaderNames />

        <!-- Table filters -->
        <LayoutTableHeaderFilters />
        </thead>


        <!-- Table Body -->
        <tbody>
        <!-- The rows (= the entries) -->
        <!--
        We include the entry index in the key in case of inconsistent data, in this case duplicated entry IDs.
        That way even if two entries have the same id, the keys will not be equals.
        -->
        <LayoutTableRow
          v-for="(entry, idx) in entries"
          :key="`table-${logic.getEntryId(entry)}-${idx}`"
          :entry="entry"
          :entry-idx="idx"
        />

        <!-- Component to create a new entry -->
        <LayoutTableNewRow v-if="canAddEntry" />

        </tbody>

      </table>
    </div>

    <LivedataBottombar>
      <div v-if="entriesFetched && entries.length === 0" class="noentries-table">
        {{ $t("livedata.bottombar.noEntries") }}
      </div>
      <LivedataPagination />
    </LivedataBottombar>
  </div>
</template>


<script>
import LivedataTopbar from "../../LivedataTopbar.vue";
import LivedataDropdownMenu from "../../LivedataDropdownMenu.vue";
import LivedataPagination from "../../LivedataPagination.vue";
import LivedataEntrySelectorInfoBar from "../../LivedataEntrySelectorInfoBar.vue";
import LayoutTableHeaderNames from "./LayoutTableHeaderNames.vue";
import LayoutTableHeaderFilters from "./LayoutTableHeaderFilters.vue";
import LayoutTableRow from "./LayoutTableRow.vue";
import LayoutTableNewRow from "./LayoutTableNewRow.vue";
import LayoutLoader from "../LayoutLoader.vue";
import LivedataBottombar from "../../LivedataBottombar.vue";

export default {

  name: "layout-table",

  components: {
    LivedataBottombar,
    LivedataTopbar,
    LivedataDropdownMenu,
    LivedataPagination,
    LivedataEntrySelectorInfoBar,
    LayoutTableHeaderNames,
    LayoutTableHeaderFilters,
    LayoutTableRow,
    LayoutTableNewRow,
    LayoutLoader,
  },

  inject: ["logic"],

  data: () => ({
    entriesFetched: false,
  }),

  computed: {
    data() {
      return this.logic.data;
    },
    entries() {
      return this.logic.data.data.entries;
    },
    canAddEntry() {
      return this.logic.canAddEntry();
    },
  },

  mounted() {
    this.logic.onEvent("afterEntryFetch", () => {
      this.entriesFetched = true;
    });
  },

};
</script>


<style>

.layout-table-wrapper {
  overflow: auto;
}

.layout-table {

  table {
    height: 100%;
  }

  th {
    border-bottom: unset;
  }

  .livedata-entry-selector-all .btn {
    display: flex;
    align-items: center;
    justify-content: flex-start;
    padding-left: 2rem;
  }
}

.noentries-table {
  text-align: center;
  color: var(--text-muted);
  width: 100%;
  padding-bottom: 1em;
}
</style>
