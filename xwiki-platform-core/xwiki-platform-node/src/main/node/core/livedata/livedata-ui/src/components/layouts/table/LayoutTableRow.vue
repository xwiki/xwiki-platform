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
  LayoutCardsCard is a row component for the Table Layout.
  It format an entry as an html row, with an entry selector on the left
-->
<template>
  <tr
    :data-livedata-entry-index="entryIdx"
    :data-livedata-entry-id="logic.getEntryId(entry)"
  >
    <!-- Entry Select -->
    <td v-if="isSelectionEnabled && isEntrySelectable" class="entry-selector">
      <LivedataEntrySelector :entry="entry" />
    </td>
    <!-- If selection is enable but entry is not selectable -->
    <td v-else-if="isSelectionEnabled"></td>

    <!-- Entry cells -->
    <td
      class="cell"
      v-for="property in properties"
      :key="property.id"
      v-show="logic.isPropertyVisible(property.id)"
      :data-title="property.name"
    >
      <LivedataDisplayer :property-id="property.id" :entry="entry" />
    </td>

    <td v-if="logic.isEditMode()">
      <template v-if="entry._new">
        <button
          type="button"
          class="btn btn-default"
          :title="$t('livedata.table.action.save')"
          @click="logic.saveNewEntry()"
        >
          <XWikiIcon :icon-descriptor="{ name: 'check' }" />
        </button>
        <button
          type="button"
          class="btn btn-default"
          :title="$t('livedata.table.action.cancel')"
          @click="logic.cancelNewEntry()"
        >
          <XWikiIcon :icon-descriptor="{ name: 'cross' }" />
        </button>
      </template>
    </td>
  </tr>
</template>

<script>
import LivedataEntrySelector from "../../LivedataEntrySelector.vue";
import LivedataDisplayer from "../../displayers/LivedataDisplayer.vue";
import XWikiIcon from "../../utilities/XWikiIcon.vue";

export default {
  name: "LayoutTableRow",

  components: {
    LivedataEntrySelector,
    LivedataDisplayer,
    XWikiIcon,
  },

  inject: ["logic"],

  props: {
    entry: Object,
    /**
     * Index of the entry in the entries array.
     * @since 14.10.20
     * @since 15.5.5
     * @since 15.10.1
     * @since 16.0.0RC1
     */
    entryIdx: {
      type: Number,
      required: true,
    },
  },

  computed: {
    properties() {
      return this.logic.getPropertyDescriptors();
    },
    isSelectionEnabled() {
      return this.logic.isSelectionEnabled();
    },
    isEntrySelectable() {
      return this.logic.isSelectionEnabled({ entry: this.entry });
    },
  },

  mounted() {
    // Autofocus the first editable cell of a new row.
    if (this.entry?._new) {
      const tryFocus = (attempt = 0) => {
        const cell = this.$el.querySelector(".editable")?.closest("[tabindex]");
        if (cell) {
          cell.focus();
        } else if (attempt < 20) {
          requestAnimationFrame(() => tryFocus(attempt + 1));
        }
      };
      tryFocus();
    }
  },
};
</script>

<style>
.layout-table .livedata-entry-selector {
  align-items: flex-start;
  padding: 10px 2rem;
}

.layout-table .entry-selector {
  padding: 0;
  height: 100%;
  width: 0;
}

.layout-table td.cell {
  /* Sets the height to 100% to allow children div to use the full cell height
  (see https://stackoverflow.com/a/18488334/657524). */
  height: 100%;
}
</style>
