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
  LayoutTableHeaderFilter is a component for the Table layout that can be used
  to quickly filter a property.
  The LivedataFilter is bind to the first filter found in the property filter list.
-->
<template>
  <tr
    v-if="isFilteringEnabled"
    class="column-filters"
  >

    <!--
      We need to create an empty cell for the entry selector
      so that it align well with the entries selectors of the rows
      and the select-all entries in the header
    -->
    <th
      v-if="isSelectionEnabled"
      class="entry-selector"
    ></th>

    <!-- The filters cells -->
    <th
      v-for="property in properties"
      :key="property.id"
      v-show="logic.isPropertyVisible(property.id)"
    >
      <LivedataFilter
        v-if="logic.isPropertyFilterable(property.id)"
        :property-id="property.id"
        :index="0"
      />
    </th>

  </tr>
</template>


<script>
import LivedataFilter from  "../../filters/LivedataFilter.vue";

export default {

  name: "LayoutTableHeaderFilters",

  components: {
    LivedataFilter,
  },

  inject: ["logic"],

  computed: {
    data () { return this.logic.data; },
    properties () { return this.logic.getPropertyDescriptors(); },

    isSelectionEnabled () {
      return this.logic.isSelectionEnabled();
    },

    isFilteringEnabled () {
      return this.logic.getFilterableProperties().length !== 0;
    }
  },

};
</script>


<style>

.layout-table .column-filters th {
  padding: 2px;
  font-weight: normal;
  vertical-align: middle;
}
.layout-table .column-filters th.entry-selector {
  width: 0;
}

/* Responsive mode */
@media screen and (max-width: @screen-xs-max) {
  .layout-table .column-filters th {
    /* Overwrite the filters width so they don't overflow the table. */
    width: 100%;
    border-top: none;
  }
}

</style>
