<!--
  See the NOTICE file distributed with this work for additional
  information regarding copyright ownership.

  This is free software; you can redistribute it and/or modify it
  under the terms of the GNU Lesser General Public License as
  published by the Free Software Foundation; either version 2.1 of
  the License, or (at your option) any later version.

  This software is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
  Lesser General Public License for more details.

  You should have received a copy of the GNU Lesser General Public
  License along with this software; if not, write to the Free
  Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
  02110-1301 USA, or see the FSF site: http://www.fsf.org.
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
  <draggable
    class="column-header-names draggable-container"
    :list="properties"
    item-key="id"
    @change="reorderProperty"
    tag="tr"
    handle=".column-name"
  >
    <!-- Entry Select All -->
    <template #header>
      <th v-if="isSelectionEnabled" class="entry-selector">
        <LivedataEntrySelectorAll />
      </th>
    </template>

    <!--
      Table Properties
      Here we can't use the XWikiDraggableItem component as it returns
      a div element, that would be invalid inside the table structure.
      So we need to implement the XWikiDraggableItem structure from scratch
    -->
    <template #item="{ element: property }">
      <th
        class="draggable-item"
        :title="property.description"
        v-show="logic.isPropertyVisible(property.id)"
      >
        <!-- Wrapper for the column header -->
        <div class="column-name">
          <!-- Property Name -->
          <button
            type="button"
            class="handle"
            @click="sort(property)"
            @keydown.left="keyboardDragNDrop($event, -1)"
            @keydown.right="keyboardDragNDrop($event, 1)"
            :title="
              logic.isPropertySortable(property.id)
                ? $t('livedata.action.columnName.sortable.hint')
                : $t('livedata.action.columnName.default.hint')
            "
          >
            <span class="property-name">{{ property.name }}</span>
            <!--
              Sort icon
              Only show the icon for the first-level sort property
            -->
            <XWikiIcon
              v-if="logic.isPropertySortable(property.id)"
              :icon-descriptor="{
                name:
                  isFirstSortLevel(property) && firstSortLevel.descending
                    ? 'caret-down'
                    : 'caret-up',
              }"
              :class="['sort-icon', isFirstSortLevel(property) ? 'sorted' : '']"
            />
          </button>
        </div>
        <!--
            Specify the handle to resize properties
          -->
        <button
          type="button"
          class="resize-handle btn btn-xs btn-default"
          :title="$t('livedata.action.resizeColumn.hint')"
          v-mousedownmove="mouseResizeColumnInit"
          @mousedownmove="mouseResizeColumn"
          @keydown.left="keyboardResizeColumn($event, -10)"
          @keydown.right="keyboardResizeColumn($event, 10)"
          @dblclick="resetColumnSize"
          @keydown.esc="resetColumnSize"
        ></button>
      </th>
    </template>
  </draggable>
</template>

<script>
import LivedataEntrySelectorAll from "../../LivedataEntrySelectorAll.vue";
import { mousedownmove } from "../../directives.js";
import XWikiIcon from "../../utilities/XWikiIcon.vue";
import draggable from "vuedraggable/src/vuedraggable";

export default {
  name: "LayoutTableHeaderNames",

  components: {
    XWikiIcon,
    LivedataEntrySelectorAll,
    draggable,
  },

  directives: {
    mousedownmove,
  },

  inject: ["logic"],

  computed: {
    data() {
      return this.logic.data;
    },

    properties() {
      return this.logic.getPropertyDescriptors();
    },

    // The first sort entry in the Livedata configuration sort array
    firstSortLevel() {
      return this.data.query.sort[0] || {};
    },

    isSelectionEnabled() {
      return this.logic.isSelectionEnabled();
    },
  },

  methods: {
    /**
     * Return whether the given property the one of `this.firstSortLevel`
     * @param property - Object A property descriptor
     * @returns true if the property is from the first sort level
     */
    isFirstSortLevel(property) {
      return this.firstSortLevel.property === property.id;
    },

    sort(property) {
      this.logic.sort(property.id, 0).catch((err) => {
        console.warn(err);
      });
    },

    reorderProperty(e) {
      this.logic.reorderProperty(e.moved.oldIndex, e.moved.newIndex);
    },

    getNextVisibleProperty(th) {
      while (th.nextElementSibling) {
        th = th.nextElementSibling;
        if (th.style.display !== "none") {
          return th;
        }
      }
    },

    keyboardDragNDrop(e, deltaIndex) {
      let handles = e.currentTarget.closest("tr").querySelectorAll(".handle");
      let oldIndex = Array.from(handles).indexOf(e.currentTarget);
      let newIndex = oldIndex + deltaIndex;
      if (newIndex >= handles.length) {
        this.logic.reorderProperty(oldIndex, newIndex - handles.length);
      } else if (newIndex <= -1) {
        this.logic.reorderProperty(oldIndex, handles.length + newIndex);
      } else {
        this.logic.reorderProperty(oldIndex, newIndex);
      }
      this.$nextTick(() => {
        handles[oldIndex].focus();
      });
    },

    mouseResizeColumnInit(e) {
      const th = e.currentTarget.closest("th");
      e.data.leftColumn = th.querySelector(".column-name");
      e.data.leftColumnBaseWidth =
        e.data.leftColumn.getBoundingClientRect()?.width;
      e.data.rightColumn =
        this.getNextVisibleProperty(th)?.querySelector(".column-name");
      e.data.rightColumnBaseWidth =
        e.data.rightColumn?.getBoundingClientRect()?.width;
      this.resizeColumnInit(th);
    },

    resizeColumnInit(th) {
      // Give all column names a fixed width so that relative widths don't change when resizing (in case the current
      // widths are not the actual column widths).
      // First, collect all widths, then set them all to avoid that due to the first values being set the other values
      // change.
      const widths = [];
      let columns = th.closest("tr").querySelectorAll(".column-name");
      // Filter columns that aren't visible to avoid setting a width of zero on them.
      columns = Array.from(columns).filter(
        (column) => column.closest("th").style.display !== "none",
      );
      for (const column of columns) {
        widths.push(column.getBoundingClientRect().width);
      }
      for (let i = 0; i < columns.length; i++) {
        columns[i].style.width = `${widths[i]}px`;
      }
    },

    resizeColumn(
      offsetX,
      leftColumn,
      rightColumn,
      leftColumnBaseWidth,
      rightColumnBaseWidth,
    ) {
      // Resize left column
      let leftColumnWidth = leftColumnBaseWidth + offsetX;
      leftColumn.style.width = `${leftColumnWidth}px`;
      // Resize right column
      if (rightColumn) {
        let rightColumnWidth = rightColumnBaseWidth - offsetX;
        rightColumn.style.width = `${rightColumnWidth}px`;
      }
    },

    mouseResizeColumn(e) {
      let offsetX = e.clientX - e.data.clickEvent.clientX;
      let leftColumn = e.data.leftColumn;
      let rightColumn = e.data.rightColumn;
      let leftColumnBaseWidth = e.data.leftColumnBaseWidth;
      let rightColumnBaseWidth = e.data.rightColumnBaseWidth;
      this.resizeColumn(
        offsetX,
        leftColumn,
        rightColumn,
        leftColumnBaseWidth,
        rightColumnBaseWidth,
      );
    },

    keyboardResizeColumn(e, offsetX) {
      const th = e.currentTarget.closest("th");
      this.resizeColumnInit(th);

      let leftColumn = th.querySelector(".column-name");
      let leftColumnBaseWidth = leftColumn.getBoundingClientRect()?.width;
      let rightColumn =
        this.getNextVisibleProperty(th)?.querySelector(".column-name");
      let rightColumnBaseWidth = rightColumn?.getBoundingClientRect()?.width;
      this.resizeColumn(
        offsetX,
        leftColumn,
        rightColumn,
        leftColumnBaseWidth,
        rightColumnBaseWidth,
      );
    },

    resetColumnSize(e) {
      // Reset all column sizes as resizing a single column sets sizes for all columns.
      for (const column of e.currentTarget
        .closest("tr")
        .querySelectorAll(".column-name")) {
        column.style.removeProperty("width");
      }
    },
  },
};
</script>

<style>
.layout-table th.draggable-item {
  display: table-cell;
  min-width: 4rem;
  padding: 0;
}

.layout-table .column-name {
  display: flex;
  justify-content: space-between;
  /* Ensure that the name is never smaller than the width of the column, i.e., it always fills the available space even
 when the column has been resized to a smaller width that is prevented by some table cell. */
  min-width: 100%;
}

.layout-table .draggable-item .resize-handle {
  /* Position the resize handle at the right edge of the column name and ensure it spans the full height. */
  position: absolute;
  right: 0;
  top: 0;
  bottom: 0;
  /* Hide the resize handle by default. */
  opacity: 0;
  /* Reset button styles. */
  padding: 0;
  margin: 0;
  min-width: 0;
  border: none;
  border-radius: 0;
  /* Indicate with the mouse cursor that this is a resize handle. */
  cursor: col-resize;
  /* Style the resize handle as 4px wide in default state. */
  width: 4px;
  /* Ensure that the resize handle is above the next column name. */
  z-index: 1;
}

.layout-table .draggable-item:focus-within .resize-handle,
.layout-table .draggable-item:hover .resize-handle {
  /* Show the resize handle and increase its width when the column is focused or hovered. */
  opacity: 1;
  background: var(--text-muted);
  width: 6px;
}

/* Center the resize handle between the current and the next column name, if there is one. */
.layout-table .draggable-item:not(:last-child) .resize-handle {
  margin-right: -2px;
}

.layout-table .draggable-item:not(:last-child):focus-within .resize-handle,
.layout-table .draggable-item:not(:last-child):hover .resize-handle {
  margin-right: -3px;
}

.layout-table .draggable-item .handle {
  opacity: 1;
  overflow: hidden;
  display: flex;
  align-items: baseline;
  white-space: nowrap;
  background: transparent;
  border: 0;
  text-align: left;
  padding: var(--table-cell-padding);
}

.layout-table .draggable-item .property-name {
  overflow: hidden;
  text-overflow: ellipsis;
}

.layout-table .sort-icon {
  color: currentColor;
  opacity: 0;
  padding-left: var(--table-cell-padding);
  cursor: pointer;
}

.layout-table .sort-icon.sorted {
  opacity: 1;
}

.layout-table .column-name:hover .sort-icon:not(.sorted),
.layout-table .column-name:focus-within .sort-icon:not(.sorted) {
  opacity: 0.5;
}

.draggable-container.column-header-names .draggable-item {
  position: relative;
}
</style>
