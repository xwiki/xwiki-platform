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
  <div class="layout-cards">

    <!-- Topbar -->
    <LivedataTopbar>
      <template #left>
        <LivedataDropdownMenu/>
        <LivedataEntrySelectorAll/>
        <LivedataRefreshButton/>
      </template>
      <template #right>
        <LivedataPagination/>
      </template>
    </LivedataTopbar>

    <!-- Entry selector info bar -->
    <LivedataEntrySelectorInfoBar/>


    <!-- Cards component -->
    <div class="livedata-cards">

    <!-- A card -->
      <LayoutCardsCard
        v-for="entry in entries"
        :key="logic.getEntryId(entry)"
        :entry="entry"
      />

      <LayoutCardsNewCard/>

    </div>

  </div>
</template>


<script>

import LivedataTopbar from "../../LivedataTopbar.vue";
import LivedataDropdownMenu from "../../LivedataDropdownMenu.vue";
import LivedataEntrySelectorAll from "../../LivedataEntrySelectorAll.vue";
import LivedataRefreshButton from "../../LivedataRefreshButton.vue";
import LivedataPagination from "../../LivedataPagination.vue";
import LivedataEntrySelectorInfoBar from "../../LivedataEntrySelectorInfoBar.vue";
import LayoutCardsCard from "./LayoutCardsCard.vue";
import LayoutCardsNewCard from "./LayoutCardsNewCard.vue";

export default {

  name: "layout-cards",

  components: {
    LivedataTopbar,
    LivedataDropdownMenu,
    LivedataEntrySelectorAll,
    LivedataRefreshButton,
    LivedataPagination,
    LivedataEntrySelectorInfoBar,
    LayoutCardsCard,
    LayoutCardsNewCard,
  },

  inject: ["logic"],

  computed: {
    data () { return this.logic.data; },
    entries () { return this.logic.data.data.entries; },
  },

};
</script>


<style>

@supports (display: grid) {

  .layout-cards .livedata-cards {
    display: grid;
    grid-template-columns: repeat(auto-fill, 30rem);
    grid-auto-rows: min-content;
    gap: 1.5rem;
  }

}

</style>
