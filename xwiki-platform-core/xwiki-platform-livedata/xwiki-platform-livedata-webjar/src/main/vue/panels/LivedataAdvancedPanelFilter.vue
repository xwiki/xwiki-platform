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
  <LivedataBaseAdvancedPanel
    class="livedata-advanced-panel-filter"
    panel-id="filterPanel"
  >

    <template #header>
      <span class="fa fa-filter"></span> Filter
    </template>

    <template #body>

      <!-- Filter groups -->
      <div
        class="livedata-filter-group"
        v-for="filterGroup in data.query.filters"
        :key="filterGroup.property"
      >

        <div class="filter-group-title">
          <span class="property-name">{{ logic.getPropertyDescriptor(filterGroup.property).name }}</span>
          <a
            class="delete-filter-group"
            href="#"
            @click.prevent="logic.removeAllFilters(filterGroup.property)"
            title="Delete the whole property filters"
          >
            <span class="fa fa-trash-o"></span>
          </a>
        </div>

        <div class="filters">
          <XWikiDraggable
            class="filter-entries"
            :value="logic.getQueryFilterGroup(filterGroup.property).constrains"
            @change="reorderFilter($event, filterGroup)"
            :group="'filter-panel' + logic.getFilterDescriptor(filterGroup.property).id"
          >
            <XWikiDraggableItem
                v-for="(filter, filterIdx) in logic.getQueryFilterGroup(filterGroup.property).constrains"
                :key="filterIdx"
              >
              <LivedataAdvancedPanelFilterEntry
                :filter="filter"
                :filter-idx="filterIdx"
                :property-id="filterGroup.property"
              />
            </XWikiDraggableItem>
          </XWikiDraggable>

          <a
            class="add-filter"
            href="#"
            @click.prevent="logic.addFilter(filterGroup.property)"
          >
            + Add filter
          </a>
        </div>

      </div>


      <!-- Add filters -->
      <select
        class="select-properties"
        v-show="unfilteredProperties.length > 0"
        @change="addFilterGroup($event.target.value)"
      >
        <option
          value="none"
          ref="selectFilterPropertiesNone"
          selected disabled
        >Add filters</option>
        <option
          v-for="property in unfilteredProperties"
          :key="property.id"
          :value="property.id"
          v-text="property.name"
        ></option>
      </select>

    </template>

  </LivedataBaseAdvancedPanel>

</template>


<script>
import LivedataBaseAdvancedPanel from "./LivedataBaseAdvancedPanel.vue";
import LivedataAdvancedPanelFilterEntry from "./LivedataAdvancedPanelFilterEntry.vue";
import XWikiDraggable from "../utilities/XWikiDraggable.vue";
import XWikiDraggableItem from "../utilities/XWikiDraggableItem.vue";

export default {

  name: "LivedataAdvancedPanelFilter",

  components: {
    LivedataBaseAdvancedPanel,
    LivedataAdvancedPanelFilterEntry,
    XWikiDraggable,
    XWikiDraggableItem,
  },

  inject: ["logic"],

  computed: {

    data () { return this.logic.data; },

    // property descriptors that does not have filters
    unfilteredProperties () {
      return this.logic.getFilterablePropertyDescriptors().filter(propertyDescriptor => {
        const filter = this.logic.getQueryFilterGroup(propertyDescriptor.id);
        return !filter || filter.constrains.length === 0;
      });
    },

  },


  methods: {

    addFilterGroup (value) {
      if (value === "none") { return; }
      this.logic.addFilter(value);
      this.$refs.selectFilterPropertiesNone.selected = true;
    },

    reorderFilter (e, filterGroup) {
      if (e.moved) {
        this.logic.filter(filterGroup.property, e.moved.oldIndex, {
          index: e.moved.newIndex,
        })
        .catch(err => void console.warn(err));
      }
      else if (e.added) {
        this.logic.addFilter(filterGroup.property, e.added.element.operator, e.added.element.value, e.added.newIndex)
        .catch(err => void console.warn(err));
      }
      else if (e.removed) {
        this.logic.removeFilter(filterGroup.property, e.removed.oldIndex)
        .catch(err => void console.warn(err));
      }
    },

  },

};
</script>


<style>


.livedata-advanced-panel-filter .property-name {
  display: inline-block;
  margin-right: 0.5rem;
  padding: 5px;
  font-weight: bold;
}
.livedata-advanced-panel-filter .delete-filter-group {
  display: none;
  padding: 5px;
  color: currentColor;
}
.livedata-advanced-panel-filter .filter-group-title:hover .delete-filter-group {
  display: inline-block;
}

.livedata-advanced-panel-filter .draggable-item .handle {
  width: 3rem;
}

.livedata-advanced-panel-filter .add-filter {
  margin-left: 3rem;
  display: inline-block;
  margin-top: 3px;
  font-style: italic;
}
.livedata-advanced-panel-filter .add-filter:hover {
  text-decoration: none;
}

.livedata-advanced-panel-filter .livedata-filter-group {
  margin-bottom: 1rem;
}

.livedata-advanced-panel-filter .select-properties {
  margin-top: 1rem;
}

</style>
