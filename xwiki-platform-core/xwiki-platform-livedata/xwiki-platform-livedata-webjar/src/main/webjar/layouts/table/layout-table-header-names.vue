<template>
  <tr
    class="column-header-names"
      is="xwiki-draggable"
      :value="data.query.properties"
      @change="reorderProperty"
      tag="tr"
  >
    <!-- Entry Select All-->
    <th class="entry-selector">
      <livedata-entry-selector-all></livedata-entry-selector-all>
    </th>

    <th
      class="draggable-item"
      v-for="property in properties"
      :key="property.id"
      v-show="logic.isPropertyVisible(property.id)"
    >
      <div
        class="column-name"
        @click="sort(property)"
      >
        <div class="handle">
          <span class="fa fa-ellipsis-v"></span>
        </div>
        <span>{{ property.name }}</span>
        <span class="flex-spacer"></span>
        <span
          v-if="logic.isPropertySortable(property.id)"
          :class="[
            'sort-icon',
            'fa',
            { 'sorted': isFirstSortLevel(property) },
            (isFirstSortLevel(property) && firstSortLevel.descending) ? 'fa-caret-up' : 'fa-caret-down',
          ]"
        ></span>
      </div>
    </th>

  </tr>
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
  "vue!livedata-entry-selector-all",
  "vue!utilities/xwiki-draggable",
], function (
  Vue
) {

  Vue.component("layout-table-header-names", {

    name: "layout-table-header-names",

    template: template,

    inject: ["logic"],

    computed: {
      data: function () { return this.logic.data; },

      properties: function () {
        return this.logic.getPropertyDescriptors();
      },

      firstSortLevel: function () {
        return this.data.query.sort[0] || {};
      },

    },


    methods: {

      isFirstSortLevel: function (property) {
        return this.firstSortLevel.property === property.id
      },

      sort: function (property) {
        this.logic.sort(property.id, 0).catch(function (err) {
          console.warn(err);
        });
      },

      reorderProperty: function (e) {
        // As the draggable plugin is taking in account every child it has for d&d
        // and there is the select-entry-all component as first child
        // we need to substract 2 to the indexes that the draggable plugin handles
        // so that it matches the true property order
        this.logic.reorderProperty(e.moved.oldIndex - 2, e.moved.newIndex - 2);
      },

    },

  });
});
</script>


<style>

th.draggable-item {
  display: table-cell;
}

.layout-table .column-name {
  display: flex;
  flex-direction: row;
  justify-content: flex-start;
  align-items: center;
  cursor: pointer;
}

.layout-table .handle {
  height: 100%;
  margin-left: -8px;
  padding: 0px 8px;
  cursor: pointer; /* IE */
  cursor: grab;
  opacity: 0;
}
.layout-table .column-name:hover .handle {
  opacity: 1;
  transition: opacity 0.2s;
}
.layout-table .handle .fa {
  vertical-align: middle;
}

.layout-table .column-name .flex-spacer {
  flex-grow: 1;
}

.layout-table .sort-icon {
  color: currentColor;
  opacity: 0;
}
.layout-table .sort-icon.sorted {
  opacity: 1;
}
.layout-table .column-name:hover .sort-icon:not(.sorted) {
  opacity: 0.5;
}

</style>
