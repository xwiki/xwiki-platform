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

  <div
    class="livedata-filter-container"
    tabindex="0"
  >


    <select
      class="select-operator"
      @change="logic.filter(propertyId, filterIdx, {operator: $event.target.value})"
    >
      <option
        v-for="operator in logic.getFilterDescriptor(propertyId).operators"
        :key="operator.id"
        :value="operator.id"
        v-text="operator.name"
        :selected="operator.id === filter.operator"
      ></option>
    </select>

    <LivedataFilter
      :property-id="propertyId"
      :index="filterIdx"
    />

    <a
      class="delete-filter"
      href="#"
      @click.prevent="logic.removeFilter(propertyId, filterIdx)"
      title="Delete filters"
    >
      <span class="fa fa-trash-o"></span>
    </a>

  </div>


</template>


<script>
import LivedataFilter from "../filters/LivedataFilter.vue";

export default {

  name: "LivedataAdvancedPanelFilterEntry",

  components: {
    LivedataFilter,
  },

  inject: ["logic"],

  props: {
    filter: Object,
    filterIdx: Number,
    propertyId: String,
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
    border-radius: 3px;
    color: currentColor;
  }
  .livedata-filter-container:hover .delete-filter {
    visibility: visible;
  }
  .livedata-filter-container .delete-filter:hover {
    background-color: #ccc4;
  }
  .livedata-filter-container .delete-filter:active {
    background-color: unset;
  }


</style>
