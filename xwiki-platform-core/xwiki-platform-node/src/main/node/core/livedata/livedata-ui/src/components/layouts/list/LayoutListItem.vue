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
  It displays an entry on a compact line: its main property, in semibold, followed
  by its secondary properties, all separated by a small circle. The secondary
  properties are labelled for screen readers, and the values cut off by the end of
  the line display their full text as a tooltip.
  The values can be edited like in the other layouts. In edit mode, the line wraps
  so that the editors are not cut off, the secondary properties are displayed even
  when they are empty, so that they can be filled, and the entry can be deleted.
-->
<template>
  <li
    class="layout-list-item"
    :class="{ 'layout-list-item-edit-mode': isEditMode }"
    :data-livedata-entry-index="entryIdx"
    :data-livedata-entry-id="logic.getEntryId(entry)"
  >
    <!-- Entry selector -->
    <LivedataEntrySelector
      v-if="isSelectionEnabled"
      v-show="isEntrySelectable"
      :entry="entry"
    />
    <!-- Main property value -->
    <div
      v-if="propertyId"
      class="layout-list-item-value layout-list-item-main"
      @mouseenter="updateTooltip"
    >
      <LivedataDisplayer :property-id="propertyId" :entry="entry" />
    </div>
    <!-- Secondary property values, right after the main one -->
    <div
      v-if="displayedSecondaryPropertyIds.length"
      class="layout-list-item-secondary"
    >
      <template
        v-for="secondaryPropertyId in displayedSecondaryPropertyIds"
        :key="secondaryPropertyId"
      >
        <span class="layout-list-item-separator" aria-hidden="true">•</span>
        <span
          class="layout-list-item-value layout-list-item-secondary-value"
          @mouseenter="updateTooltip"
        >
          <!-- The property names are not displayed, but they are read by screen readers. -->
          <span class="layout-list-item-label">{{
            $t("livedata.list.property.label", [
              getPropertyName(secondaryPropertyId),
            ])
          }}</span>
          <LivedataDisplayer
            :property-id="secondaryPropertyId"
            :entry="entry"
          />
        </span>
      </template>
    </div>
    <!-- Entry actions, only available in edit mode. -->
    <button
      v-if="isEditMode"
      type="button"
      class="btn btn-default btn-xs layout-list-item-delete"
      :title="
        $t(
          isEntrySaved
            ? 'livedata.table.action.delete'
            : 'livedata.table.action.cancel',
        )
      "
      @click="logic.deleteEntry(entry)"
    >
      <XWikiIcon
        :icon-descriptor="{ name: isEntrySaved ? 'trash' : 'cross' }"
      />
    </button>
  </li>
</template>

<script>
import LivedataEntrySelector from "../../LivedataEntrySelector.vue";
import LivedataDisplayer from "../../displayers/LivedataDisplayer.vue";
import XWikiIcon from "../../utilities/XWikiIcon.vue";

export default {
  name: "LayoutListItem",

  components: {
    LivedataEntrySelector,
    LivedataDisplayer,
    XWikiIcon,
  },

  inject: ["logic"],

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
    // The id of the main property to display.
    propertyId: {
      type: String,
      default: undefined,
    },
    // The ids of the secondary properties to display, in the order of the Live Data properties.
    secondaryPropertyIds: {
      type: Array,
      default: () => [],
    },
  },

  computed: {
    // The secondary properties without a value for this entry are skipped, to avoid displaying separators with
    // nothing between them, except in edit mode, where they are displayed so that they can be filled.
    displayedSecondaryPropertyIds() {
      if (this.isEditMode) {
        return this.secondaryPropertyIds;
      }
      return this.secondaryPropertyIds.filter((propertyId) => {
        const value = this.entry[propertyId];
        return (
          value !== undefined && value !== null && String(value).trim() !== ""
        );
      });
    },

    isSelectionEnabled() {
      return this.logic.isSelectionEnabled();
    },

    isEntrySelectable() {
      return this.logic.isSelectionEnabled({ entry: this.entry });
    },

    isEditMode() {
      return this.logic.isEditMode();
    },

    isEntrySaved() {
      return this.logic.getEntryId(this.entry) !== undefined;
    },
  },

  mounted() {
    // Focus the first editable value of a new entry, so that it can be filled right away, like in the other layouts.
    if (this.entry?._new) {
      const tryFocus = (attempt = 0) => {
        const value = this.$el
          .querySelector(".editable")
          ?.closest("[tabindex]");
        if (value) {
          value.focus();
        } else if (attempt < 20) {
          requestAnimationFrame(() => tryFocus(attempt + 1));
        }
      };
      tryFocus();
    }
  },

  methods: {
    getPropertyName(propertyId) {
      return this.logic.getPropertyDescriptor(propertyId)?.name ?? propertyId;
    },

    // Display the full text of a value as a tooltip, only when it is cut off by the end of the line.
    updateTooltip(event) {
      const value = event.currentTarget;
      if (value.scrollWidth > value.clientWidth) {
        value.title = value.textContent.replace(/\s+/g, " ").trim();
      } else {
        value.removeAttribute("title");
      }
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
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/*
  The main property is displayed in semibold. It keeps its size, unless it is too long, so that the secondary
  properties are truncated first.
*/
.layout-list .layout-list-item-main {
  flex: 0 0 auto;
  max-width: 60%;
  font-weight: var(--font-weight-semibold);
}

/* The secondary properties follow the main property, in the order of the Live Data properties. */
.layout-list .layout-list-item-secondary {
  display: flex;
  flex: 0 1 auto;
  align-items: baseline;
  gap: 0.5em;
  min-width: 0;
  overflow: hidden;
  white-space: nowrap;
}

.layout-list .layout-list-item-secondary-value {
  flex: 0 1 auto;
}

.layout-list .layout-list-item-separator {
  flex: none;
  color: var(--text-muted);
}

/* Keep the items on one compact line, whatever the displayed values. */
.layout-list .layout-list-item-value p {
  display: inline;
  margin: 0;
}

.layout-list .layout-list-item-value img {
  width: auto;
  max-height: 1.5em;
  vertical-align: middle;
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

/*
  The editors don't fit on a compact line: in edit mode, and while a value is edited, the line wraps and the values are
  no longer cut off.
*/
.layout-list .layout-list-item-edit-mode,
.layout-list .layout-list-item:has(.livedata-displayer .edit),
.layout-list .layout-list-item-edit-mode .layout-list-item-secondary,
.layout-list
  .layout-list-item:has(.livedata-displayer .edit)
  .layout-list-item-secondary {
  flex-wrap: wrap;
}

.layout-list .layout-list-item-edit-mode .layout-list-item-value,
.layout-list .layout-list-item-edit-mode .layout-list-item-secondary,
.layout-list .layout-list-item-value:has(.livedata-displayer .edit),
.layout-list
  .layout-list-item:has(.livedata-displayer .edit)
  .layout-list-item-secondary {
  max-width: none;
  overflow: visible;
  white-space: normal;
}

/* The labels of the secondary properties are hidden, but read by screen readers. */
.layout-list .layout-list-item-label {
  position: absolute;
  width: 1px;
  height: 1px;
  margin: -1px;
  padding: 0;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}

/* The delete action is displayed at the end of the line. */
.layout-list .layout-list-item-delete {
  flex: none;
  margin-left: auto;
}
</style>
