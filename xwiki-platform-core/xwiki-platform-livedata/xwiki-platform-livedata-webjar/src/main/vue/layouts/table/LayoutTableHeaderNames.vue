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
    <th
      v-if="isSelectionEnabled"
      class="entry-selector"
    >
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
        <!--
          Specify the handle to drag properties.
          Use the stop propagation modifier on click event
          to prevent sorting the column unintentionally.
        -->
        <div
          role="presentation"
          class="handle"
          :title="$t('livedata.action.reorder.hint')"
          @click.stop
        >
          <XWikiIcon :icon-descriptor="{name: 'text_align_justify'}"/>
        </div>
        <!-- Property Name -->
        <span class="property-name">{{ property.name }}</span>
        <!--
          Sort icon
          Only show the icon for the first-level sort property
        -->
        <XWikiIcon
          v-if="logic.isPropertySortable(property.id)"
          :icon-descriptor="{name: isFirstSortLevel(property) && firstSortLevel.descending? 'caret-down': 'caret-up'}"
          :class="['sort-icon',  isFirstSortLevel(property)? 'sorted': '']"/>
      </div>
      <!--
        Resize handle
        Use the stop propagation modifier on click event
        to prevent sorting the column unintentionally.
      -->
      <div role="presentation" class="resize-handle" :title="$t('livedata.action.resizeColumn.hint')"
        v-mousedownmove="resizeColumnInit"
        @mousedownmove="resizeColumn"
        @click.stop
        @dblclick="resetColumnSize"
      ></div>
    </th>

  </XWikiDraggable>
</template>


<script>
import LivedataEntrySelectorAll from "../../LivedataEntrySelectorAll.vue";
import XWikiDraggable from "../../utilities/XWikiDraggable.vue";
import { mousedownmove } from "../../directives.js";
import XWikiIcon from "../../utilities/XWikiIcon";

export default {

  name: "LayoutTableHeaderNames",

  components: {
    XWikiIcon,
    LivedataEntrySelectorAll,
    XWikiDraggable,
  },

  directives: {
    mousedownmove,
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

    isSelectionEnabled () {
      return this.logic.isSelectionEnabled();
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
      /*
        As the draggable plugin is taking in account every child it has for d&d
        and there is the select-entry-all component as first child
        we need to substract 1 to the indexes that the draggable plugin handles
        so that it matches the true property order
        When selection is disabled (and the select-entry-all component hidden)
        we don't need to readjust the offset of the indexes
        as Vue handily creates a html comment where the component shoud be
        So that it does not messes up with indexes
      */
      this.logic.reorderProperty(e.moved.oldIndex - 1, e.moved.newIndex - 1);
    },

    getNextVisibleProperty (th) {
      while (th.nextElementSibling) {
        th = th.nextElementSibling;
        if (th.style.display !== "none") return th;
      }
    },

    resizeColumnInit (e) {
      const th = e.currentTarget.closest("th");
      e.data.leftColumn = th.querySelector(".column-name");
      e.data.leftColumnBaseWidth = e.data.leftColumn.getBoundingClientRect()?.width;
      e.data.rightColumn = this.getNextVisibleProperty(th)?.querySelector(".column-name");
      e.data.rightColumnBaseWidth = e.data.rightColumn.getBoundingClientRect()?.width;
    },

    resizeColumn (e) {
      const offsetX = e.clientX - e.data.clickEvent.clientX;
      // Resize left column
      const leftColumnWidth = e.data.leftColumnBaseWidth + offsetX;
      e.data.leftColumn.style.width = `${leftColumnWidth}px`;

      // Resize right column
      if (e.data.rightColumn) {
        const rightColumnWidth = e.data.rightColumnBaseWidth - offsetX;
        e.data.rightColumn.style.width = `${rightColumnWidth}px`;
      }
    },

    resetColumnSize (e) {
      const th = e.currentTarget.closest("th");
      const column = th.querySelector(".column-name");
      column.style.width = "unset";
    },

  },

};
</script>


<style>

.layout-table th.draggable-item {
  display: table-cell;
  min-width: 4rem;
  overflow: hidden;
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
  margin-left: -@table-cell-padding;
  padding: 0 @table-cell-padding;
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

.layout-table .property-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-right: 10px;
}

.layout-table .sort-icon {
  color: currentColor;
  opacity: 0;
  padding-left: @table-cell-padding;
}
.layout-table .sort-icon.sorted {
  opacity: 1;
}
.layout-table .column-name:hover .sort-icon:not(.sorted) {
  opacity: 0.5;
}

.draggable-container.column-header-names .draggable-item {
  position: relative;
}

.layout-table .resize-handle {
  position: absolute;
  right: 0px;
  top: 0px;
  bottom: 0px;
  transform: translateX(50%);
  width: 10px;
  background-color: transparent;
  cursor: ew-resize;
  user-select: none;
  z-index: 100;

  &:hover {
    background-color: @breadcrumb-bg;
  }
}

/* Responsive mode */
@media screen and (max-width: @screen-xs-max) {
  .layout-table th.draggable-item {
    /* Overwrite the draggable-item display in order to show the property names (table header) as a column. */
    display: block;

    .handle {
      /* Always show the drag handler because hover is not available on mobile. */
      opacity: 1;
    }

    /* Trim long property names. */
    .property-name {
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }
  }

  .layout-table .resize-handle {
    /* The columns cannot be resized in responsive mode. */
    display: none;
  }
}

</style>
