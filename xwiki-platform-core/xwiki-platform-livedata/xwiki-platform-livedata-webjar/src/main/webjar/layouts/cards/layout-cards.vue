<template>
  <div class="layout-cards">

    <!-- Topbar -->
    <livedata-topbar>
      <template #left>
        <livedata-dropdown-menu></livedata-dropdown-menu>
        <livedata-entry-selector-all></livedata-entry-selector-all>
      </template>
      <template #right>
        <livedata-pagination></livedata-pagination>
      </template>
    </livedata-topbar>

    <!-- Entry selector info bar -->
    <livedata-entry-selector-info-bar></livedata-entry-selector-info-bar>


    <!-- Cards component -->
    <div class="livedata-cards">

    <!-- A card -->
      <layout-cards-card
        v-for="entry in entries"
        :key="logic.getEntryId(entry)"
        :entry="entry"
      ></layout-cards-card>

    </div>

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
  "vue!livedata-entry-selector-all",
  "vue!livedata-pagination",
  "vue!livedata-entry-selector-info-bar",
  "vue!layouts/cards/layout-cards-card",
], function (
  Vue,
  vuedraggable
) {

  Vue.component("layout-cards", {

    name: "layout-cards",

    template: template,

    inject: ["logic"],

    computed: {
      data: function () { return this.logic.data; },
      entries: function () { return this.logic.data.data.entries; },
    },

  });
});
</script>


<style>

</style>
