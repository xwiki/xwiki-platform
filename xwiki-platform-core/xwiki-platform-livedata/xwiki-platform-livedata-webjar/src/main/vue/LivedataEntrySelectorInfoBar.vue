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
  The LivedataEntrySelectorInfoBar indicate to the user
  what is being selected in the Livedata.
  It gives the number of entry selected compared to the number of
  entries in total.

  The total number of entries is either:
  - The number of entries per page
  - The number of entries mathing the current config (global mode on)
  - TODO: The total number of entries in the Livedata (global mode on without config)
-->
<template>
  <div
    class="livedata-entry-selector-info-bar"
    v-if="selectedCount > 0"
  >
    <span v-if="!logic.entrySelection.isGlobal">
      {{ $t('livedata.selection.infoBar.selectedCount', [selectedCount]) }}
    </span>
    <span v-else-if="logic.entrySelection.isGlobal && !logic.entrySelection.deselected.length">
      {{ $t('livedata.selection.infoBar.allSelected') }}
    </span>
    <span v-else>
      {{ $t('livedata.selection.infoBar.allSelectedBut', [logic.entrySelection.deselected.length]) }}
    </span>
  </div>
</template>


<script>
export default {

  name: "LivedataEntrySelectorInfoBar",

  inject: ["logic"],

  computed: {
    data () { return this.logic.data; },

    // The number of selected entries to be displayed
    selectedCount () {
      if (this.logic.entrySelection.isGlobal) {
        return this.data.data.count - this.logic.entrySelection.deselected.length;
      } else {
        return this.logic.entrySelection.selected.length;
      }
    },
  },

};
</script>


<style>

.livedata-entry-selector-info-bar {
  margin-bottom: 1rem;
  padding: 12px;
  border-radius: @border-radius-base;
  background-color: @panel-default-heading-bg;
}

</style>
