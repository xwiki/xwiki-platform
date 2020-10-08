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
  <tr class="column-filters">

    <!--
      We need to create an empty cell for the entry selector
      so that it align well with the entries selectors of the rows
      and the select-all entries in the header
    -->
    <th class="entry-selector"></th>

    <!-- The filters cells -->
    <th
      v-for="property in properties"
      :key="property.id"
      v-show="logic.properties.isVisible(property.id)"
    >
      <LivedataFilter
        v-if="logic.filters.isFilterable(property.id)"
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
    properties () { return this.logic.properties.getDescriptors(); },
  },

};
</script>


<style>

.layout-table .column-filters th {
  padding-left: 0;
  padding-right: 0;
  font-weight: normal;
  vertical-align: middle;
  /*
    This width is used to make the filters take up globally the same space
    inside the table header, because by default the table display allocate
    size according to its cell content (but in a way we don't want because
    this could stretch a cell more than half of the table width, which is ugly)
  */
  width: 100vw;
}
.layout-table .column-filters th.entry-selector {
  width: 0;
}

.layout-table tbody > tr:first-child td {
  border-top: unset;
}

</style>
