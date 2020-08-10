<template>
  <livedata-base-advanced-panel
    class="livedata-advanced-panel-sort"
    panel-id="sortPanel"
  >

    <template #header>
      <span class="fa fa-sort"></span> Sort
    </template>

    <template #body>

      <xwiki-draggable
        :value="data.query.sort"
        @change="reorderSorts"
      >

        <!-- A sort entry -->
        <xwiki-draggable-item
          class="sort-entry"
          v-for="(sortEntry, level) in data.query.sort"
          :key="level"
        >
          <!-- property name -->
          <span>{{ logic.getPropertyDescriptor(sortEntry.property).name }}</span>

          <span style="margin: 0 1rem"> - </span>

          <!-- direction select -->
          <select
            @change="logic.sort(sortEntry.property, level, $event.target.value === 'true')"
          >
            <option
              value="false"
              :selected="!sortEntry.descending"
            >Ascending</option>
            <option
              value="true"
              :selected="sortEntry.descending"
            >Descending</option>
          </select>

          <a
            class="delete-sort"
            href="#"
            @click.prevent="logic.removeSort(sortEntry.property)"
            title="Delete Sort"
          >
            <span class="fa fa-trash-o"></span>
          </a>
        </xwiki-draggable-item>

      </xwiki-draggable>

      <!-- Add Sort -->
      <select
        class="select-properties"
        v-show="unsortedProperties.length > 0"
        @change="addSortLevel($event.target.value)"
      >
        <option
          value="none"
          ref="selectPropertiesNone"
          selected disabled
        >Add Sort level</option>
        <option
          v-for="property in unsortedProperties"
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
], function (
  Vue
) {

  Vue.component("livedata-advanced-panel-sort", {

    name: "livedata-advanced-panel-sort",

    template: template,

    inject: ["logic"],

    computed: {
      data: function () { return this.logic.data; },

      // property descriptors that does not have sort
      unsortedProperties: function () {
        var self = this;
        return this.logic.getSortablePropertyDescriptors().filter(function (propertyDescriptor) {
          var sort = self.logic.getQuerySort(propertyDescriptor.id);
          return !sort;
        });
      },
    },

    methods: {
      addSortLevel: function (value) {
        if (value === "none") { return; }
        this.logic.addSort(value);
        this.$refs.selectPropertiesNone.selected = true;
      },
      reorderSorts: function (e) {
        this.logic.reorderSort(e.moved.element.property, e.moved.newIndex)
        .catch(function(err) { console.warn(err); });
      },
    },

  });
});
</script>


<style>

.livedata-advanced-panel-sort .sort-entry {
  display: flex;
  flex-direction: row;
  justify-content: flex-start;
  align-items: center;
}

.livedata-advanced-panel-sort .draggable-item .handle {
  width: 3rem;
}

.livedata-advanced-panel-sort .delete-sort {
  display: inline-block;
  visibility: hidden;
  margin-left: 5px;
  padding: 6px 10px;
  border-radius: 3px;
  color: currentColor;
}
.livedata-advanced-panel-sort .sort-entry:hover .delete-sort {
  visibility: visible;
}
.livedata-advanced-panel-sort .delete-sort:hover {
  background-color: #ccc4;
}
.livedata-advanced-panel-sort .delete-sort:active {
  background-color: unset;
}

.livedata-advanced-panel-sort .select-properties {
  margin-top: 1rem;
}

</style>
