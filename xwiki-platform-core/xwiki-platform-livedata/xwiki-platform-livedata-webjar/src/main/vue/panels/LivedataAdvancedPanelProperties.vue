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
  On edit mode allows:
  - Toggling property visibility
  - Changing property order
  On design mode, it also allows:
  - changing property type, displayer widget, and filter widget
-->
<template>
  <!--
    Uses the LivedataBaseAdvancedPanel as root element, as it handles for us
    all the Advanced Panels default behavior
  -->
  <LivedataBaseAdvancedPanel
    class="livedata-advanced-panel-properties"
    panel-id="propertiesPanel"
  >
    <!-- Provide the panel name and icon to the `header` slot -->
    <template #header>
      <span class="fa fa-list-ul"></span> Properties
    </template>

    <!-- Define panel content inside the `body` slot -->
    <template #body>
      <!--
        The properties are wrapped inside a XWikiDraggable component
        in order to allow the user to reorder them easily
      -->
      <XWikiDraggable
        class="property-container"
        :value="data.query.properties"
        @change="reorderProperties"
      >
        <!--
          Properties
          Uses the XWikiDraggableItem component that goes along the
          XWikiDraggable one
        -->
        <XWikiDraggableItem
          class="property"
          v-for="property in logic.properties.getDescriptors()"
          :key="property.id"
        >

          <!--
            Container wrapping the edit and design mode
            It is used so that everything is well align to the right
            of the XWikiDraggableItem handle
          -->
          <div class="property-options-container">

            <!--
              Edit Mode section
              Contains properties name with a checkbox to toggle their visibility
            -->
            <div class="property-edit-container">

              <!--
                Property visibility checkbox
                Checkbox is surrounded by a div with padding to facilitate
                the user click
              -->
              <div
                class="visibility"
                @click.self="$event.currentTarget.querySelector('input').click()"
              >
                <input
                  type="checkbox"
                  :checked="logic.properties.isVisible(property.id)"
                  @change="logic.properties.setVisibility(property.id, $event.target.checked)"
                />
              </div>

              <!-- Property name -->
              <span class="property-name">{{ property.name }}</span>

              <!--
                Button to expand design mode pane
                Only visible when design mode is on
              -->
              <a
                class="toggle-edit-panel"
                v-if="logic.designMode.activated"
                @click.prevent="toggleEditPanel(property.id)"
                href="#"
              >
                edit
                <span
                  :class="[
                    'fa',
                    visibleEditPanels[property.id] ? 'fa-angle-up' : 'fa-angle-down'
                  ]"></span>
              </a>
            </div>


            <!--
              Design Mode Pane
              Contains selects to modify properties of Livedata properties,
              like their type, displayer id, filter id
            -->
            <LivedataAdvancedPanelPropertiesDesignPane
              v-if="logic.designMode.activated"
              v-show="visibleEditPanels[property.id]"
              :property="property"
            />

          </div>

        </XWikiDraggableItem>
      </XWikiDraggable>

    </template>

  </LivedataBaseAdvancedPanel>

</template>


<script>
import LivedataBaseAdvancedPanel from "./LivedataBaseAdvancedPanel.vue";
import LivedataAdvancedPanelPropertiesDesignPane from "./LivedataAdvancedPanelPropertiesDesignPane.vue";
import XWikiDraggable from "../utilities/XWikiDraggable.vue";
import XWikiDraggableItem from "../utilities/XWikiDraggableItem.vue";

export default {

  name: "LivedataAdvancedPanelProperties",

  components: {
    LivedataBaseAdvancedPanel,
    LivedataAdvancedPanelPropertiesDesignPane,
    XWikiDraggable,
    XWikiDraggableItem,
  },

  inject: ["logic"],

  data () {
    return {
      // Each key correspond to a Livedata property, and the value is
      // a boolean indicating whether its edit panel is expanded or not.
      visibleEditPanels: {},
    }
  },

  computed: {
    data () { return this.logic.data; },
  },

  methods: {
    // Event handler for when properties are dragged and dropped
    reorderProperties (e) {
      this.logic.properties.reorder(e.moved.element, e.moved.newIndex);
    },

    // Event handler for when user click on button to expand / collapse edit panel
    toggleEditPanel (propertyId) {
      // we cannot add properties to an already created reactive object
      // In order to keep data reactivity, we copy that object
      // modify it, then reassign it, so that all its properties are reactive
      const visibleEditPanels = {...this.visibleEditPanels};
      visibleEditPanels[propertyId] = !this.visibleEditPanels[propertyId];
      this.visibleEditPanels = visibleEditPanels;
    },

  },


};
</script>


<style>

.livedata-advanced-panel-properties .property-container {
  display: flex;
  flex-direction: column;
  justify-content: flex-start;
  align-items: stretch;
}
.livedata-advanced-panel-properties .property {
  display: flex;
  flex-direction: row;
  justify-content: flex-start;
  align-items: flex-start;
}

.livedata-advanced-panel-properties .property-options-container {
  flex-grow: 1;
  display: flex;
  flex-direction: column;
  justify-content: flex-start;
  align-items: stretch;
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
.livedata-advanced-panel-properties .toggle-edit-panel {
  flex-grow: 1;
  margin-left: 1rem;
  font-style: italic;
}

.livedata-advanced-panel-properties .property-edit-container {
  display: flex;
  flex-direction: row;
  justify-content: flex-start;
  align-items: center;
}

</style>
