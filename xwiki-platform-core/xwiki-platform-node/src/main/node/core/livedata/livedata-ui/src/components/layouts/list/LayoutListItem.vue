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
  LayoutListItem is an item component for the List Layout.
  It displays a single property of an entry, on a compact line.
  The value is read-only: it cannot be edited from the List layout.
-->
<template>
  <li
    class="layout-list-item"
    :data-livedata-entry-index="entryIdx"
    :data-livedata-entry-id="logic.getEntryId(entry)"
  >
    <!-- Entry selector -->
    <LivedataEntrySelector
      v-if="isSelectionEnabled"
      v-show="isEntrySelectable"
      :entry="entry"
    />
    <!-- Displayed property value -->
    <div v-if="propertyId" class="layout-list-item-value">
      <LivedataDisplayer :property-id="propertyId" :entry="entry" />
    </div>
  </li>
</template>

<script>
import LivedataEntrySelector from "../../LivedataEntrySelector.vue";
import LivedataDisplayer from "../../displayers/LivedataDisplayer.vue";
import { createReadOnlyLogic } from "../readOnlyLogic.js";

export default {
  name: "LayoutListItem",

  components: {
    LivedataEntrySelector,
    LivedataDisplayer,
  },

  inject: ["logic"],

  // Provide a read-only logic to the displayers, so that they don't offer to edit the value.
  provide() {
    return {
      logic: createReadOnlyLogic(this.logic),
    };
  },

  props: {
    entry: {
      type: Object,
      required: true,
    },
    // Index of the entry in the entries array.
    entryIdx: {
      type: Number,
      required: true,
    },
    // The id of the property to display.
    propertyId: {
      type: String,
      default: undefined,
    },
  },

  computed: {
    isSelectionEnabled() {
      return this.logic.isSelectionEnabled();
    },

    isEntrySelectable() {
      return this.logic.isSelectionEnabled({ entry: this.entry });
    },
  },
};
</script>

<style>
/*
  The items are displayed as regular text lines: they inherit the font, color and line height of the surrounding
  content, without the borders and backgrounds of the table layout.
*/
.layout-list .layout-list-item {
  display: flex;
  flex-direction: row;
  align-items: baseline;
  gap: 0.5em;
  padding: 0.125em 0;
}

.layout-list .layout-list-item .livedata-entry-selector {
  width: unset;
  height: auto;
  padding: 0;
}

.layout-list .layout-list-item .livedata-entry-selector input {
  margin: 0;
}

.layout-list .layout-list-item-value {
  flex-grow: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/*
  The displayers are sized to fill a table cell by default. In a list, their value is displayed inline, like regular
  text, so that it is aligned on the start of the line and truncated with an ellipsis when too long.
*/
.layout-list .layout-list-item-value .livedata-displayer,
.layout-list .layout-list-item-value .livedata-displayer > .view,
.layout-list .layout-list-item-value .livedata-displayer > .view > div {
  display: inline;
  width: auto;
  height: auto;
  text-align: inherit;
}
</style>
