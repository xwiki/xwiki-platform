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
  The LivedataAdvancedPanelProperties component is used to provide
  advance property options edition, whatever layout is being used.
  It allows:
  - Toggling property visibility
  - Changing property order
  - Choosing the property used as the card cover, in the cards layout
-->
<template>
  <!--
    Uses the LivedataBaseAdvancedPanel as root element, as it handles for us
    all the Advanced Panels default behavior
  -->
  <LivedataBaseAdvancedPanel
    class="livedata-advanced-panel-properties"
    :panel-id="panel.id"
  >
    <!-- Provide the panel name and icon to the `header` slot -->
    <template #header>
      <XWikiIcon :icon-descriptor="{ name: panel.icon }" />
      {{ panel.title }}
    </template>

    <!-- Define panel content inside the `body` slot -->
    <template #body>
      <!--
        Remove the card cover, only available in the cards layout. It is the first option of the cover radio group,
        whose other options are displayed next to each property.
      -->
      <div v-if="isCardsLayout" class="no-cover">
        <label class="cover-option">
          <input
            type="radio"
            :name="coverInputName"
            value=""
            :checked="!coverPropertyId"
            @change="setCover('')"
          />
          {{ $t("livedata.panel.properties.noCover") }}
        </label>
      </div>
      <!--
        The properties are wrapped inside a XWikiDraggable component
        in order to allow the user to reorder them easily
      -->
      <draggable
        class="draggable-container"
        :list="properties"
        item-key="id"
        @change="reorderProperty"
        tag="div"
        handle=".handle"
      >
        <!--
            Properties
            Uses the XWikiDraggableItem component that goes along the
            XWikiDraggable one
          -->
        <template #item="{ element: property }">
          <XWikiDraggableItem class="property">
            <!--
              Property visibility checkbox
              Checkbox is surrounded by a div with padding to facilitate the user click
            -->
            <label
              class="visibility"
              @click.self="$event.currentTarget.querySelector('input').click()"
            >
              <input
                type="checkbox"
                :checked="logic.isPropertyVisible(property.id)"
                @change="setVisible(property.id, $event.target.checked)"
              />

              <!-- Property name -->
              <span class="property-name">{{ property.name }}</span>
            </label>

            <!--
              Card cover selection, only available in the cards layout. A hidden property can't be used as the cover.
            -->
            <label
              v-if="isCardsLayout"
              class="cover cover-option"
              :class="{ disabled: !logic.isPropertyVisible(property.id) }"
              :title="
                $t('livedata.panel.properties.cover.hint', [property.name])
              "
            >
              <input
                type="radio"
                :name="coverInputName"
                :value="property.id"
                :checked="coverPropertyId === property.id"
                :disabled="!logic.isPropertyVisible(property.id)"
                :aria-label="
                  $t('livedata.panel.properties.cover.hint', [property.name])
                "
                @change="setCover(property.id)"
              />
              {{ $t("livedata.panel.properties.cover") }}
            </label>
          </XWikiDraggableItem>
        </template>
      </draggable>
    </template>
  </LivedataBaseAdvancedPanel>
</template>

<script>
import LivedataBaseAdvancedPanel from "./LivedataBaseAdvancedPanel.vue";
import {
  CARDS_LAYOUT_ID,
  getCoverPropertyId,
  setCoverPropertyId,
} from "../layouts/cards/cardsLayout.js";
import XWikiDraggableItem from "../utilities/XWikiDraggableItem.vue";
import XWikiIcon from "../utilities/XWikiIcon.vue";
import draggable from "vuedraggable/src/vuedraggable";

export default {
  name: "LivedataAdvancedPanelProperties",

  components: {
    XWikiIcon,
    LivedataBaseAdvancedPanel,
    draggable,
    XWikiDraggableItem,
  },

  inject: ["logic"],

  props: { panel: Object },

  computed: {
    data() {
      return this.logic.data;
    },

    properties() {
      return this.logic.getPropertyDescriptors();
    },

    // The card cover can only be chosen in the cards layout.
    isCardsLayout() {
      return this.logic.currentLayoutId?.value === CARDS_LAYOUT_ID;
    },

    coverPropertyId() {
      return getCoverPropertyId(this.logic);
    },

    // The name grouping the cover radio buttons of this Live Data.
    coverInputName() {
      return `${this.data.id}-card-cover`;
    },
  },

  methods: {
    // Show or hide a property. Hiding the property used as the card cover removes the cover.
    setVisible(propertyId, visible) {
      // Check the cover before hiding the property, since a hidden property is not used as the cover.
      const isCover = this.coverPropertyId === propertyId;
      this.logic.setPropertyVisible(propertyId, visible);
      if (!visible && isCover) {
        this.setCover("");
      }
    },

    // Use the given property as the card cover, or remove the cover when the property is empty.
    setCover(propertyId) {
      setCoverPropertyId(this.logic, propertyId);
    },

    // event handler called when properties are dragged and dropped
    reorderProperty(e) {
      this.logic.reorderProperty(e.moved.oldIndex, e.moved.newIndex);
    },
  },
};
</script>

<style>
.livedata-advanced-panel-properties .no-cover {
  display: flex;
  padding: 6px 0;
  border-bottom: 1px solid color-mix(in srgb, currentColor 15%, transparent);
}

/*
  The cover options are displayed at the end of their row, with the same minimum width, so that their radio buttons
  are aligned by their left border whatever the length of their label.
*/
.livedata-advanced-panel-properties .cover-option {
  display: inline-flex;
  align-items: center;
  gap: 0.25em;
  min-width: 7em;
  margin: 0 1rem 0 auto;
  font-weight: var(--font-weight-regular);
  white-space: nowrap;
}

.livedata-advanced-panel-properties .cover-option input {
  margin: 0;
}

.livedata-advanced-panel-properties .cover-option.disabled {
  color: var(--text-muted);
}

.livedata-advanced-panel-properties .property {
  display: flex;
  flex-direction: row;
  justify-content: flex-start;
  align-items: center;
}

.livedata-advanced-panel-properties .handle {
  padding: 6px;
  cursor: grab;
  opacity: 0;
}

.livedata-advanced-panel-properties .property:hover .handle {
  opacity: 1;
  transition: opacity 0.2s;
}

.livedata-advanced-panel-properties .visibility {
  padding: 6px 1rem;
  margin-bottom: 0;
  font-weight: var(--font-weight-regular);
}

.livedata-advanced-panel-properties .visibility input {
  margin: 0px;
}

.livedata-advanced-panel-properties .property-name {
  padding: 6px 0px;
  margin-left: 1rem;
}
</style>
