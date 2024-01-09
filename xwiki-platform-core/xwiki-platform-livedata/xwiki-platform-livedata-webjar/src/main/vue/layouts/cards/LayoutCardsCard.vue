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
  LayoutCardsCard is a card component for the Cards Layout.
  It format an entry as a card, with a title that can be specified
  in the `titleProperty` property of its layout descriptor,
  inside the Livedata configuration.
-->
<template>
  <div
      class="card"
      :data-livedata-entry-index="entryIdx"
      :data-livedata-entry-id="logic.getEntryId(entry)"
  >

    <!-- Card title-->
    <div class="card-title">
      <!-- Entry selector -->
      <LivedataEntrySelector
        v-if="isSelectionEnabled"
        v-show="isEntrySelectable"
        :entry="entry"
      />
      <!-- Title property -->
      <h2 v-if="!!titlePropertyId && logic.isPropertyVisible(titlePropertyId)">
        <LivedataDisplayer
          :property-id="titlePropertyId"
          :entry="entry"
        />
      </h2>
    </div>

    <!--
      The cards properties are wrapped inside a XWikiDraggable component
      in order to allow the user to reorder them easily
    -->
    <XWikiDraggable
      :value="data.query.properties"
      @change="reorderProperty"
    >
      <!--
        Card Properties
        Uses the XWikiDraggableItem component that goes along the
        XWikiDraggable one
      -->
      <XWikiDraggableItem
        class="card-property"
        v-for="property in properties"
        :key="property.id"
        v-show="logic.isPropertyVisible(property.id) && property.id !== titlePropertyId"
      >
        <!-- Specify the handle to drag properties -->
        <template #handle>
          <XWikiIcon :icon-descriptor="{name: 'more-vertical'}"/>
        </template>

        <!-- Property Name -->
        <strong class="property-name">{{ property.name }}:</strong>
        <!-- Property Value -->
        <span class="value">
          <LivedataDisplayer
            :property-id="property.id"
            :entry="entry"
          />
        </span>

      </XWikiDraggableItem>
    </XWikiDraggable>

  </div>
</template>


<script>
import LivedataEntrySelector from "../../LivedataEntrySelector.vue";
import LivedataDisplayer from "../../displayers/LivedataDisplayer.vue";
import XWikiDraggable from "../../utilities/XWikiDraggable.vue";
import XWikiDraggableItem from "../../utilities/XWikiDraggableItem.vue";
import XWikiIcon from "../../utilities/XWikiIcon";

export default {

  name: "LayoutCardsCard",

  components: {
    XWikiIcon,
    LivedataEntrySelector,
    LivedataDisplayer,
    XWikiDraggable,
    XWikiDraggableItem,
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
      required: true
    }
  },

  computed: {
    data () { return this.logic.data; },

    properties () {
      return this.logic.getPropertyDescriptors();
    },

    // The id of the property that is going to be in the card title
    titlePropertyId () {
      return this.logic.getLayoutDescriptor("cards").titleProperty;
    },

    isSelectionEnabled () {
      return this.logic.isSelectionEnabled();
    },

    isEntrySelectable () {
      return this.logic.isSelectionEnabled({ entry: this.entry });
    },

  },

  methods: {
    reorderProperty (e) {
      this.logic.reorderProperty(e.moved.element, e.moved.newIndex);
    },
  },

};
</script>


<style>

.layout-cards .card {
  display: inline-block;
  margin: 1rem;
  padding: 1rem 2rem;
  border: 1px solid lightgray;
  border-radius: 1rem;
}

.layout-cards .card-title {
  display: flex;
  flex-direction: row;
  justify-content: flex-start;
  align-items: center;
}

.layout-cards .card-title h2 {
  width: 100%;
}

.layout-cards .card-property {
  display: flex;
  flex-direction: row;
  justify-content: flex-start;
  align-items: flex-start;
  transition: height 0.5s;
}

.layout-cards .handle {
  height: 100%;
  margin-left: -8px;
  padding: 0px 8px;
  cursor: pointer; /* IE */
  cursor: grab;
  opacity: 0;
}
.layout-cards .card-property:hover .handle {
  opacity: 1;
  transition: opacity 0.2s;
}
.layout-cards .handle .fa {
  vertical-align: middle;
}

.layout-cards .livedata-entry-selector {
  width: unset;
  padding: 10px;
  margin-left: -10px;
}
.layout-cards h2 {
  margin-top: 10px;
  font-size: 20px;
}


.layout-cards .property-name {
  margin-right: 0.5em;
}

.layout-cards .value {
  flex-grow: 1;
  align-self: stretch;
}

/* for not IE11 */
@supports (display: grid) {

  .layout-cards .card {
    margin: 0;
  }

}

</style>
