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
  LayoutCardsCard is a row component for the Table Layout.
  It format an entry as an html row, with an entry selector on the left
-->
<template>
  <tr 
      :data-livedata-entry-index="entryIdx"
      :data-livedata-entry-id="logic.getEntryId(entry)"
  >

    <!-- Entry Select -->
    <td
      v-if="isSelectionEnabled && isEntrySelectable"
      class="entry-selector"
    >
      <LivedataEntrySelector :entry="entry"/>
    </td>
    <!-- If selection is enable but entry is not selectable -->
    <td v-else-if="isSelectionEnabled"></td>

    <!-- Entry cells -->
    <td
      class="cell"
      v-for="property in properties"
      :key="property.id"
      v-show="logic.isPropertyVisible(property.id)"
      :data-title="property.name"
    >
      <LivedataDisplayer
        :property-id="property.id"
        :entry="entry"
      />
    </td>

  </tr>
</template>


<script>
import LivedataEntrySelector from "../../LivedataEntrySelector.vue";
import LivedataDisplayer from "../../displayers/LivedataDisplayer.vue";

export default {

  name: "LayoutTableRow",

  components: {
    LivedataEntrySelector,
    LivedataDisplayer,
  },

  inject: ["logic"],

  props: {
    entry: Object,
    /**
     * Index of the entry in the entries array.
     * @since 14.10.20
     * @since 15.5.5
     * @since 15.10.1
     * @since 16.0.0RC1
     */
    entryIdx: {
      type: Number,
      required: true
    }
  },

  computed: {
    data () { return this.logic.data; },
    properties () { return this.logic.getPropertyDescriptors(); },
    isSelectionEnabled () { return this.logic.isSelectionEnabled(); },
    isEntrySelectable () {
      return this.logic.isSelectionEnabled({ entry: this.entry });
    },
  },

};
</script>


<style>

.layout-table .livedata-entry-selector {
  align-items: flex-start;
  padding: 10px 2rem;
}

.layout-table .entry-selector {
  padding: 0;
  height: 100%;
  width: 0;
}

.layout-table td.cell {
  /* Sets the height to 100% to allow children div to use the full cell height 
  (see https://stackoverflow.com/a/18488334/657524). */
  height: 100%;
}
</style>
