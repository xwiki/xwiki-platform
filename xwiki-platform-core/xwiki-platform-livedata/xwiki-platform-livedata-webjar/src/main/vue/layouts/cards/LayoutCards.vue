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
  LayoutCard.vue is the main file for the Card layout component.
  It displays data formatted as cards, with a title that can be
  specified in the `titleProperty` property of its layout descriptor,
  inside the Livedata configuration.
-->
<template>
  <div class="layout-cards">

    <!--
      The layout Topbar
      Add common layout utilities, like the dropdown menu, the refresh button,
      and the pagination.
      It is also has the "select all" button
    -->
    <LivedataTopbar>
      <template #left>
        <LivedataPagination/>
      </template>
      <template #right>
        <LivedataEntrySelectorAll v-if="isSelectionEnabled"/>
        <LivedataDropdownMenu/>
      </template>
    </LivedataTopbar>

    <!-- Entry selector info bar -->
    <LivedataEntrySelectorInfoBar/>

    <!-- Loading bar -->
    <LayoutLoader />


    <!-- Cards layout root -->
    <div class="layout-table-root">

      <!--
        The cards (= the entries)
        Implement property reorder
      -->
      <!-- 
        We include the entry index in the key in case of inconsistent data, in this case duplicated entry IDs.
        That way even if two entries have the same id, the keys will not be equals. 
      -->
      <LayoutCardsCard
        v-for="(entry, idx) in entries"
        :key="`card-${logic.getEntryId(entry)}-${idx}`"
        :entry="entry"
        :entry-idx="idx"
      />

      <!-- Component to create a new entry -->
      <LayoutCardsNewCard v-if="canAddEntry"/>

    </div>
    <LivedataBottombar>
      <div v-if="entriesFetched && entries.length === 0" class="noentries-card">
        {{ $t('livedata.bottombar.noEntries') }}
      </div>
      <LivedataPagination/>
    </LivedataBottombar>

  </div>
</template>


<script>

import LivedataTopbar from "../../LivedataTopbar.vue";
import LivedataDropdownMenu from "../../LivedataDropdownMenu.vue";
import LivedataPagination from "../../LivedataPagination.vue";
import LivedataEntrySelectorInfoBar from "../../LivedataEntrySelectorInfoBar.vue";
import LivedataEntrySelectorAll from "../../LivedataEntrySelectorAll.vue";
import LayoutCardsCard from "./LayoutCardsCard.vue";
import LayoutCardsNewCard from "./LayoutCardsNewCard.vue";
import LayoutLoader from "../LayoutLoader.vue";
import LivedataBottombar from "../../LivedataBottombar";

export default {

  name: "layout-cards",

  components: {
    LivedataBottombar,
    LivedataTopbar,
    LivedataDropdownMenu,
    LivedataEntrySelectorAll,
    LivedataPagination,
    LivedataEntrySelectorInfoBar,
    LayoutCardsCard,
    LayoutCardsNewCard,
    LayoutLoader,
  },

  inject: ["logic"],
  
  data: () => ({
    entriesFetched: false
  }),

  computed: {
    data () { return this.logic.data; },
    entries () { return this.logic.data.data.entries; },
    isSelectionEnabled () { return this.logic.isSelectionEnabled(); },
    canAddEntry () { return this.logic.canAddEntry(); },
  },
  
  mounted() {
    this.logic.onEvent('afterEntryFetch', () => {
      this.entriesFetched = true;
    });
  }

};
</script>


<style>

/*
  The Cards Layout uses css grid to display its cards in a nice grid pattern
  However, IE11 does not support a lot grid layouts, but does not support either
  the `@supports` at-rule, so only browser that support at-rule (everyone but IE)
  and display grid will use the following styles
*/
@supports (display: grid) {

  /* Make the cards 30rem large, and display as many of them on one row */
  .layout-cards .layout-table-root {
    display: grid;
    grid-template-columns: repeat(auto-fill, 30rem);
    grid-auto-rows: min-content;
    gap: 1.5rem;
    grid-gap: 1.5rem; /* safari */
  }

}

.layout-cards .layout-loader {
  margin-top: -0.5rem;
  margin-bottom: 0.5rem;
}

.noentries-card {
  text-align: center;
  color: @text-muted;
  width: 100%;
  padding-bottom: 1em;
}
</style>
