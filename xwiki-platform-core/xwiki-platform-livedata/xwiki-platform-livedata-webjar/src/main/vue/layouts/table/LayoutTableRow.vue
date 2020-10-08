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
  <tr>

    <!-- Entry Select -->
    <td class="entry-selector">
      <LivedataEntrySelector :entry="entry"/>
    </td>

    <!-- Entry cells -->
    <td
      class="cell"
      v-for="property in properties"
      :key="property.id"
      v-show="logic.properties.isVisible(property.id)"
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
  },

  computed: {
    data () { return this.logic.data; },
    properties () { return this.logic.properties.getDescriptors(); },
  },

};
</script>


<style>

.layout-table .cell {
  padding: 0;
  height: 100%;
}

.layout-table .cell .livedata-displayer.view {
  padding: 8px 12px;
}

.layout-table .entry-selector {
    padding: 0;
    height: 100%;
    width: 0;
}

.layout-table .livedata-entry-selector {
    justify-content: flex-start;
    padding: 0 2rem;
}

</style>
