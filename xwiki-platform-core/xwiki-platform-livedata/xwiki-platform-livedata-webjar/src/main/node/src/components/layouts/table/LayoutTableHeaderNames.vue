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
  <draggable
    class="column-header-names draggable-container"
    :list="properties"
    item-key="id"
    @change="reorderProperty"
    tag="tr"
  >
    <!-- Entry Select All -->
    <template #header>
      <th
        v-if="isSelectionEnabled"
        class="entry-selector"
      >
        <LivedataEntrySelectorAll />
      </th>
    </template>

    <!--
      Table Properties
      Here we can't use the XWikiDraggableItem component as it returns
      a div element, that would be invalid inside the table structure.
      So we need to implement the XWikiDraggableItem structure from scratch
    -->
    <template #item="{element: property}">
      <th
        class="draggable-item"
        :title="property.description"
        v-show="logic.isPropertyVisible(property.id)"
      >
        <!-- Wrapper for the column header -->
        <div class="column-name">
          <!-- Property Name -->
          <button type="button" class="handle"
                  @click="sort(property)"
                  @keydown.left="keyboardDragNDrop($event, -1)"
                  @keydown.right="keyboardDragNDrop($event, 1)"
                  :title="logic.isPropertySortable(property.id) ?
            $t('livedata.action.columnName.sortable.hint') :
            $t('livedata.action.columnName.default.hint')"
          >
            <span class="property-name">{{ property.name }}</span>
            <!--
              Sort icon
              Only show the icon for the first-level sort property
            -->
            <XWikiIcon
              v-if="logic.isPropertySortable(property.id)"
              :icon-descriptor="{name: isFirstSortLevel(property) && firstSortLevel.descending? 'caret-down': 'caret-up'}"
              :class="['sort-icon',  isFirstSortLevel(property)? 'sorted': '']" />
          </button>
        </div>
        <!--
            Specify the handle to resize properties
          -->
        <button type="button" class="resize-handle btn btn-xs btn-default"
                :title="$t('livedata.action.resizeColumn.hint')"
                v-mousedownmove="mouseResizeColumnInit"
                @mousedownmove="mouseResizeColumn"
                @keydown.left="keyboardResizeColumn($event, -10)"
                @keydown.right="keyboardResizeColumn($event, 10)"
                @dblclick="resetColumnSize"
                @keydown.esc="resetColumnSize"
        >
        </button>
      </th>
    </template>
  </draggable>
</template>


<script>
import LivedataEntrySelectorAll from "../../LivedataEntrySelectorAll.vue";
import draggable from "vuedraggable/src/vuedraggable";
import { mousedownmove } from "../../directives.js";
import XWikiIcon from "../../utilities/XWikiIcon.vue";

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
     * @param {property} Object A property descriptor
     * @returns {Boolean}
     */
    isFirstSortLevel(property) {
      return this.firstSortLevel.property === property.id;
    },

    sort(property) {
      this.logic.sort(property.id, 0).catch(err => {
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
      e.data.leftColumnBaseWidth = e.data.leftColumn.getBoundingClientRect()?.width;
      e.data.rightColumn = this.getNextVisibleProperty(th)?.querySelector(".column-name");
      e.data.rightColumnBaseWidth = e.data.rightColumn?.getBoundingClientRect()?.width;
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
      columns = Array.from(columns).filter(column => column.closest("th").style.display !== "none");
      for (const column of columns) {
        widths.push(column.getBoundingClientRect().width);
      }
      for (let i = 0; i < columns.length; i++) {
        columns[i].style.width = `${widths[i]}px`;
      }
    },

    resizeColumn(offsetX, leftColumn, rightColumn, leftColumnBaseWidth, rightColumnBaseWidth) {
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
      this.resizeColumn(offsetX, leftColumn, rightColumn, leftColumnBaseWidth,
        rightColumnBaseWidth);
    },

    keyboardResizeColumn(e, offsetX) {
      const th = e.currentTarget.closest("th");
      this.resizeColumnInit(th);

      let leftColumn = th.querySelector(".column-name");
      let leftColumnBaseWidth = leftColumn.getBoundingClientRect()?.width;
      let rightColumn = this.getNextVisibleProperty(th)?.querySelector(".column-name");
      let rightColumnBaseWidth = rightColumn?.getBoundingClientRect()?.width;
      this.resizeColumn(offsetX, leftColumn, rightColumn, leftColumnBaseWidth,
        rightColumnBaseWidth);
    },

    resetColumnSize(e) {
      // Reset all column sizes as resizing a single column sets sizes for all columns.
      for (const column of e.currentTarget.closest("tr").querySelectorAll(".column-name")) {
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
  padding: 8px 0 8px 4px;
}

.layout-table .column-name {
  display: flex;
  justify-content: space-between;
}

.layout-table .draggable-item .resize-handle {
  position: absolute;
  right: 0;
  top: 0.5rem;
  bottom: 0.5rem;
  /* TODO: Discussion about the exact display of resize handles.
      See https://jira.xwiki.org/browse/XWIKI-21816 */
  opacity: 0;
  padding: 0;
  cursor: col-resize;
  min-width: 0;
  width: 0;
  border-width: 2px;
  border-radius: 0;
  margin-left: 2px;
}

.layout-table .draggable-item:focus-within .resize-handle,
.layout-table .draggable-item:hover .resize-handle {
  opacity: 1;
  border-color: var(--text-muted);
  border-width: 3px;
  margin-left: 0;
}

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
  text-overflow: ellipsis;
  white-space: nowrap;
  background: transparent;
  border: 0;
  text-align: left;
}

.layout-table .sort-icon {
  color: currentColor;
  opacity: 0;
  padding-left: var(--table-cell-padding);
  cursor: pointer;
}

.layout-table .property-name + .sort-icon {
  vertical-align: baseline;
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
