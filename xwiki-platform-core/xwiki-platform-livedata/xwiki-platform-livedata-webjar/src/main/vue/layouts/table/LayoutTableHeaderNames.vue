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
  LayoutTableHeaderNames is a component for the Table layout that displays
  the property names in the table header of the table
  It also allow the user to sort by a property by clicking on it (it sets
  the property as the first level of sort)
  and also allow the user to reorder properties by dragind and dropping them
-->
<template>
  <!--
    The table properties are wrapped inside a XWikiDraggable component
    in order to allow the user to reorder them easily
  -->
  <XWikiDraggable
    class="column-header-names"
      :value="data.query.properties"
      @change="reorderProperty"
      tag="tr"
  >
    <!-- Entry Select All -->
    <th class="entry-selector">
      <LivedataEntrySelectorAll/>
    </th>

    <!--
      Table Properties
      Here we can't use the XWikiDraggableItem component as it returns
      a div element, that would be invalid inside the table structure.
      So we need to implement the XWikiDraggableItem structure from scratch
    -->
    <th
      class="draggable-item"
      v-for="property in properties"
      :key="property.id"
      v-show="logic.isPropertyVisible(property.id)"
    >
      <!-- Wrapper for the column header -->
      <div
        class="column-name"
        @click="sort(property)"
      >
        <!-- Specify the handle to drag properties -->
        <div class="handle">
          <span class="fa fa-ellipsis-v"></span>
        </div>
        <!-- Property Name -->
        <span>{{ property.name }}</span>
        <!-- Spacer between the property name and the sort icon -->
        <span class="flex-spacer"></span>
        <!--
          Sort icon
          Only show the icon for the first-level sort property
        -->
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
import LivedataEntrySelectorAll from "../../LivedataEntrySelectorAll.vue";
import XWikiDraggable from "../../utilities/XWikiDraggable.vue";

export default {

  name: "LayoutTableHeaderNames",

  components: {
    LivedataEntrySelectorAll,
    XWikiDraggable,
  },

  inject: ["logic"],

  computed: {
    data () { return this.logic.data; },

    properties () {
      return this.logic.getPropertyDescriptors();
    },

    // The first sort entry in the Livedata configuration sort array
    firstSortLevel () {
      return this.data.query.sort[0] || {};
    },

  },


  methods: {

    /**
     * Return whether the given property the one of `this.firstSortLevel`
     * @param {property} Object A property descriptor
     * @returns {Boolean}
     */
    isFirstSortLevel (property) {
      return this.firstSortLevel.property === property.id
    },

    sort (property) {
      this.logic.sort(property.id, 0).catch(err => {
        console.warn(err);
      });
    },

    reorderProperty (e) {
      // As the draggable plugin is taking in account every child it has for d&d
      // and there is the select-entry-all component as first child
      // we need to substract 1 to the indexes that the draggable plugin handles
      // so that it matches the true property order
      this.logic.reorderProperty(e.moved.oldIndex - 1, e.moved.newIndex - 1);
    },

  },

};
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
