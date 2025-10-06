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
  The LivedataAdvancedPanelProperties component is used to provide
  advance property options edition, whatever layout is being used.
  It allows:
  - Toggling property visibility
  - Changing property order
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
      <XWikiIcon :icon-descriptor="{name: panel.icon}" />
      {{ panel.title }}
    </template>

    <!-- Define panel content inside the `body` slot -->
    <template #body>
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
        <template #item="{element: property}">
          <XWikiDraggableItem class="property">
            <!--
              Property visibility checkbox
              Checkbox is surrounded by a div with padding to facilitate the user click
            -->
            <div
              class="visibility"
              @click.self="$event.currentTarget.querySelector('input').click()"
            >
              <input
                type="checkbox"
                :checked="logic.isPropertyVisible(property.id)"
                @change="logic.setPropertyVisible(property.id, $event.target.checked)"
              />
            </div>

            <!-- Property name -->
            <span class="property-name">{{ property.name }}</span>
          </XWikiDraggableItem>
        </template>
      </draggable>
    </template>
  </LivedataBaseAdvancedPanel>
</template>


<script>
import LivedataBaseAdvancedPanel from "./LivedataBaseAdvancedPanel.vue";
import draggable from "vuedraggable/src/vuedraggable";
import XWikiDraggableItem from "../utilities/XWikiDraggableItem.vue";
import XWikiIcon from "../utilities/XWikiIcon.vue";

export default {

  name: "LivedataAdvancedPanelProperties",

  components: {
    XWikiIcon,
    LivedataBaseAdvancedPanel,
    draggable,
    XWikiDraggableItem,
  },

  inject: ["logic"],

  props: { "panel": Object },

  computed: {
    data() {
      return this.logic.data;
    },

    properties() {
      return this.logic.getPropertyDescriptors();
    },
  },

  methods: {
    // event handler called when properties are dragged and dropped
    reorderProperty(e) {
      this.logic.reorderProperty(e.moved.oldIndex, e.moved.newIndex);
    },
  },

};
</script>


<style>


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
}

.livedata-advanced-panel-properties .visibility input {
  margin: 0px;
}

.livedata-advanced-panel-properties .property-name {
  padding: 6px 0px;
}
</style>
