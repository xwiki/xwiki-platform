<!--
  See the NOTICE file distributed with this work for additional
  information regarding copyright ownership.

  This is free software; you can redistribute it and/or modify it
  under the terms of the GNU Lesser General Public License as
  published by the Free Software Foundation; either version 2.1 of
  the License, or (at your option) any later version.

  This software is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
  Lesser General Public License for more details.

  You should have received a copy of the GNU Lesser General Public
  License along with this software; if not, write to the Free
  Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
  02110-1301 USA, or see the FSF site: http://www.fsf.org.
-->

<!--
  LayoutList.vue is the main file for the List layout component.
  It displays the entries as a compact, read-only list. Each entry shows its first
  visible property, in the order of the Livedata properties, as its main property,
  and its other visible properties on the right of the line.
-->
<template>
  <div class="layout-list">
    <!--
      The layout Topbar
      Add common layout utilities, like the dropdown menu, the refresh button,
      and the pagination.
      It also has the "select all" button
    -->
    <LivedataTopbar>
      <template #left>
        <LivedataPagination side="left" />
      </template>
      <template #right>
        <LivedataPagination side="right" />
        <LivedataEntrySelectorAll v-if="isSelectionEnabled" />
        <LivedataDropdownMenu />
      </template>
    </LivedataTopbar>

    <!-- Entry selector info bar -->
    <LivedataEntrySelectorInfoBar />

    <!-- Loading bar -->
    <LayoutLoader />

    <!-- List layout root -->
    <ul class="layout-list-root">
      <!--
        We include the entry index in the key in case of inconsistent data, in this case duplicated entry IDs.
        That way even if two entries have the same id, the keys will not be equals.
      -->
      <LayoutListItem
        v-for="(entry, idx) in entries"
        :key="`list-item-${logic.getEntryId(entry)}-${idx}`"
        :entry="entry"
        :entry-idx="idx"
        :property-id="propertyId"
        :secondary-property-ids="secondaryPropertyIds"
      />
    </ul>
    <LivedataBottombar>
      <div v-if="entriesFetched && entries.length === 0" class="noentries-list">
        {{ $t("livedata.bottombar.noEntries") }}
      </div>
      <LivedataPagination />
    </LivedataBottombar>
  </div>
</template>

<script>
import LayoutListItem from "./LayoutListItem.vue";
import {
  getListPropertyId,
  getListSecondaryPropertyIds,
} from "./listLayout.js";
import LivedataBottombar from "../../LivedataBottombar.vue";
import LivedataDropdownMenu from "../../LivedataDropdownMenu.vue";
import LivedataEntrySelectorAll from "../../LivedataEntrySelectorAll.vue";
import LivedataEntrySelectorInfoBar from "../../LivedataEntrySelectorInfoBar.vue";
import LivedataPagination from "../../LivedataPagination.vue";
import LivedataTopbar from "../../LivedataTopbar.vue";
import LayoutLoader from "../LayoutLoader.vue";

export default {
  name: "layout-list",

  components: {
    LivedataBottombar,
    LivedataTopbar,
    LivedataDropdownMenu,
    LivedataEntrySelectorAll,
    LivedataPagination,
    LivedataEntrySelectorInfoBar,
    LayoutListItem,
    LayoutLoader,
  },

  inject: ["logic"],

  data: () => ({
    entriesFetched: false,
  }),

  computed: {
    entries() {
      return this.logic.data.data.entries;
    },
    // The id of the main property displayed for each entry
    propertyId() {
      return getListPropertyId(this.logic);
    },
    // The ids of the other visible properties, displayed on the right of each entry
    secondaryPropertyIds() {
      return getListSecondaryPropertyIds(this.logic);
    },
    isSelectionEnabled() {
      return this.logic.isSelectionEnabled();
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
.layout-list .layout-list-root {
  margin: 0 0 0.5rem;
  padding: 0;
  list-style: none;
}

.layout-list .layout-loader {
  margin-top: -0.5rem;
  margin-bottom: 0.5rem;
}

.noentries-list {
  text-align: center;
  color: var(--text-muted);
  width: 100%;
  padding-bottom: 1em;
}
</style>
