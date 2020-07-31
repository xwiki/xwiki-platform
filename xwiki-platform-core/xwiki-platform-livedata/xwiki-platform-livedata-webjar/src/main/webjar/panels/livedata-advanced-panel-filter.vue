<template>
  <livedata-base-advanced-panel
    class="livedata-advanced-panel-filter"
    :logic="logic"
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
          <xwiki-draggable
            class="filter-entries"
            :value="logic.getQueryFilterGroup(filterGroup.property).constrains"
            @change="reorderFilter($event, filterGroup)"
            :group="'filter-panel' + logic.getFilterDescriptor(filterGroup.property).id"
          >
            <xwiki-draggable-item
                v-for="(filter, filterIdx) in logic.getQueryFilterGroup(filterGroup.property).constrains"
                :key="filterIdx"
              >
              <livedata-advanced-panel-filter-entry
                :logic="logic"
                :filter="filter"
                :filter-idx="filterIdx"
                :property-id="filterGroup.property"
              ></livedata-advanced-panel-filter-entry>
            </xwiki-draggable-item>
          </xwiki-draggable>

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

  </livedata-base-advanced-panel>

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
  "vue!panels/livedata-base-advanced-panel",
  "vue!utilities/xwiki-draggable",
  "vue!utilities/xwiki-draggable-item",
  "vue!panels/livedata-advanced-panel-filter-entry",
], function (
  Vue
) {

  Vue.component("livedata-advanced-panel-filter", {

    name: "livedata-advanced-panel-filter",

    template: template,

    props: {
      logic: Object,
    },


    computed: {

      data: function () { return this.logic.data; },

      // property descriptors that does not have filters
      unfilteredProperties: function () {
        var self = this;
        return this.logic.getFilterablePropertyDescriptors().filter(function (propertyDescriptor) {
          var filter = self.logic.getQueryFilterGroup(propertyDescriptor.id);
          return !filter || filter.constrains.length === 0;
        });
      },

    },


    methods: {

      addFilterGroup: function (value) {
        if (value === "none") { return; }
        this.logic.addFilter(value);
        this.$refs.selectFilterPropertiesNone.selected = true;
      },

      reorderFilter: function (e, filterGroup) {
        if (e.moved) {
          this.logic.filter(filterGroup.property, e.moved.oldIndex, {
            index: e.moved.newIndex,
          })
          .catch(function (err) { console.warn(err); });
        }
        else if (e.added) {
          this.logic.addFilter(filterGroup.property, e.added.element.operator, e.added.element.value, e.added.newIndex)
          .catch(function (err) { console.warn(err); });
        }
        else if (e.removed) {
          this.logic.removeFilter(filterGroup.property, e.removed.oldIndex)
          .catch(function (err) { console.warn(err); });
        }
      },

    },

  });
});
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
