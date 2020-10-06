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
  The LivedataAdvancedPanelPropertiesDesignPane component is used by the
  LivedataAdvancedPanelProperties component.
  It is associated with a LD property, passed as a prop
  It allows changing the type, displayer widget, and filter widget
  of this property
-->
<template>
  <div class="property-design-pane panel panel-default">

    <!-- Change Property Type -->
    <div class="property-design">
      <span class="design-title">Type</span>
      <select
        @change="logic.setPropertyType(property.id, $event.target.value)"
      >
        <!-- Available property types -->
        <option
          v-for="typeDescriptor in data.meta.propertyTypes"
          :key="typeDescriptor.id"
          :value="typeDescriptor.id"
          :selected="typeDescriptor.id === property.type"
        >
          {{ typeDescriptor.name }}, {{ property.type }}
        </option>
      </select>
    </div>


    <!-- Change Property Displayers -->
    <div class="property-design">
      <span class="design-title">Displayer</span>
      <select
        @change="logic.setPropertyDisplayer(property.id, $event.target.value)"
      >
        <!-- Default displayers -->
        <option
          :value="undefined"
          :selected="isDisplayerSelected(undefined)"
        >
          Default ({{ logic.getTypeDisplayerDescriptor(property.type).id }})
        </option>
        <!-- Available displayers -->
        <option
          v-for="displayerDescriptor in data.meta.displayers"
          :key="displayerDescriptor.id"
          :value="displayerDescriptor.id"
          :selected="isDisplayerSelected(displayerDescriptor.id)"
        >
          {{ displayerDescriptor.name }}
        </option>
      </select>
    </div>


    <!-- Change Property Filters -->
    <div class="property-design">
      <span class="design-title">Filter</span>
      <select
        @change="logic.setPropertyFilter(property.id, $event.target.value)"
      >
        <option
          :value="undefined"
          :selected="isFilterSelected(undefined)"
        >
          Default ({{ logic.getTypeFilterDescriptor(property.type).id }})
        </option>
        <option
          v-for="filterDescriptor in data.meta.filters"
          :key="filterDescriptor.id"
          :value="filterDescriptor.id"
          :selected="isFilterSelected(filterDescriptor.id)"
        >
          {{ filterDescriptor.name }}
        </option>
      </select>
    </div>

  </div>
</template>


<script>

export default {

  name: "LivedataAdvancedPanelPropertiesDesignPane",

  inject: ["logic"],

  props: {
    property: Object,
  },

  computed: {
    data () { return this.logic.data; },

    displayerId () {
      return this.logic.getPropertyDisplayerDescriptor(this.property.id).id;
    },

    isDisplayerDefaulted () {
      return this.property.displayer === undefined;
    },

    filterId () {
      return this.logic.getPropertyFilterDescriptor(this.property.id).id;
    },

    isFilterDefaulted () {
      return this.property.filter === undefined;
    },

  },

  methods: {
    isDisplayerSelected (displayerId) {
      if (this.isDisplayerDefaulted) {
        return displayerId === undefined;
      } else {
        return displayerId === this.displayerId;
      }
    },

    isFilterSelected (filterId) {
      if (this.isFilterDefaulted) {
        return filterId === undefined;
      } else {
        return filterId === this.filterId;
      }
    }
  },

};
</script>


<style>

.livedata-advanced-panel-properties .property-design-pane {
  display: flex;
  flex-direction: column;
  justify-content: flex-start;
  align-items: stretch;
  padding: 1rem;
}

.livedata-advanced-panel-properties .property-design-pane .property-design {
  display: flex;
  flex-direction: row;
  justify-content: flex-start;
  align-items: center;
}
.livedata-advanced-panel-properties .property-design-pane .property-design:not(:last-child) {
  margin-bottom: 8px;
}
.livedata-advanced-panel-properties .property-design-pane .design-title::after {
  content: ":";
  margin-right: 1rem;
}
.livedata-advanced-panel-properties .property-design-pane select {
  width: 100%;
}

</style>
