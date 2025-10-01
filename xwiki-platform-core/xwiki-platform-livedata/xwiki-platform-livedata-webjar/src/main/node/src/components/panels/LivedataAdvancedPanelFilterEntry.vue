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
  The LivedataAdvancedPanelFilterEntry component is used by the
  LivedataAdvancedPanelFilterc omponent.
  It displays the filter entry corresponding to the passed props:
  - propertyId, corresponding to the Filter Group containing the filter
  - filterIndex indeicating the filter index inside the Filter Group
-->
<template>
  <div class="livedata-filter-container">
    <!--
      Operator select
      Allow to select operator corresponding to the filter id
    -->
    <select
      class="operator-select"
      @change="logic.filter(propertyId, filterIndex, { operator: $event.target.value })"
    >
      <option
        v-for="operator in logic.getFilterDescriptor(propertyId).operators"
        :key="operator.id"
        :value="operator.id"
        v-text="operator.name"
        :selected="operator.id === filterEntry.operator"
      ></option>
    </select>

    <!--
      Livedata Filter component
      Uses the LivedataFilter component to let the user modify the filter value
    -->
    <LivedataFilter
      :property-id="propertyId"
      :index="filterIndex"
      :is-advanced="true"
    />

    <!-- Delete filter entry button -->
    <a
      class="delete-filter"
      href="#"
      @click.prevent="logic.removeFilter(propertyId, filterIndex)"
      :title="$t('livedata.panel.filter.delete')"
    >
      <XWikiIcon :icon-descriptor="{name: 'trash'}" />
    </a>

  </div>
</template>


<script>
import LivedataFilter from "../filters/LivedataFilter.vue";
import XWikiIcon from "../utilities/XWikiIcon.vue";

export default {

  name: "LivedataAdvancedPanelFilterEntry",

  components: {
    LivedataFilter,
    XWikiIcon,
  },

  inject: ["logic"],

  props: {
    filterIndex: Number,
    propertyId: String,
  },

  computed: {
    // The filter entry corresponding to the props
    filterEntry() {
      return this.logic.getQueryFilterGroup(this.propertyId).constraints[this.filterIndex];
    },
  },

};
</script>


<style>

.livedata-filter-container {
  display: flex;
  flex-direction: row;
  justify-content: flex-start;
  align-items: center;
  width: 100%;
}

.livedata-filter-container .delete-filter {
  display: inline-block;
  visibility: hidden;
  margin-left: 5px;
  padding: 6px 10px;
  border-radius: var(--border-radius-small);
  color: currentColor;
}

.livedata-filter-container:hover .delete-filter {
  visibility: visible;
}

.livedata-filter-container .delete-filter:hover {
  background-color: var(--panel-default-heading-bg);
}

.livedata-filter-container .delete-filter:active {
  background-color: unset;
}
</style>
