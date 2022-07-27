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
  The LivedataAdvancedPanelFilter component is used to provide
  advance filtering, whatever layout is being used.
  It allows:
  - Adding / removing multiple filters to a property
  - Changing filter operator
  - Reordering filters
  - Moving filters between properties of same type
  - TODO: switch between "AND" and "OR" filter combining mode
-->
<template>
  <!--
    Uses the LivedataBaseAdvancedPanel as root element, as it handles for us
    all the Advanced Panels default behavior
  -->
  <LivedataBaseAdvancedPanel
    class="livedata-advanced-panel-filter"
    :panel-id="panel.id"
  >
    <!-- Provide the panel name and icon to the `header` slot -->
    <template #header>
      <XWikiIcon :icon-descriptor="{name: panel.icon}"/>
      {{ panel.title }}
    </template>

    <!-- Define panel content inside the `body` slot -->
    <template #body>

      <!-- Explain why the panel might be empty. -->
      <div v-show="!logic.getFilterableProperties().length" class="text-muted">
        {{ $t('livedata.panel.filter.noneFilterable') }}
      </div>

      <!--
        Filter Groups
        i.e. Items of the filter array in the Livedata configuration
        (one item per property, each item contains all the property filters)
      -->
      <div
        class="livedata-filter-group"
        v-for="filterGroup in data.query.filters"
        :key="filterGroup.property"
      >
        <!-- Filter Group title (property name) -->
        <div class="filter-group-title">
          <!-- Property name -->
          <span class="property-name">
            {{ logic.getPropertyDescriptor(filterGroup.property).name }}
          </span>
          <!--
            Filter Group delete button
            Delete the whole filter group (= all the filters of the property)
          -->
          <a
            class="delete-filter-group"
            href="#"
            @click.prevent="logic.removeAllFilters(filterGroup.property)"
            :title="$t('livedata.panel.filter.deleteAll')"
          >
            <XWikiIcon :icon-descriptor="{name: 'trash'}"/>
          </a>
        </div>

        <!--
          Filter Group filters
          Contains the property filters (aka filter entries)
          It uses the dedicated LivedataAdvancedPanelFilterEntry component
          for cleaner and more efficient implementation
        -->
        <div class="filters">
          <!--
            The filter entries are wrapped inside a XWikiDraggable component
            in order to allow the user to reorder them easily
            It also has a group prop set to the property filterId,
            in order to allow to move filter entries between properties
            of same filterId
          -->
          <XWikiDraggable
            class="filter-entries"
            :value="logic.getQueryFilterGroup(filterGroup.property).constraints"
            @change="reorderFilter($event, filterGroup)"
            :group="'filter-panel' + logic.getFilterDescriptor(filterGroup.property).id"
          >
            <!--
              Draggable wrapper for the filter entry
              Uses the XWikiDraggableItem component that goes along the
              XWikiDraggable one
            -->
            <XWikiDraggableItem
                v-for="(filter, filterIdx) in logic.getQueryFilterGroup(filterGroup.property).constraints"
                :key="filterIdx"
              >
              <!-- Filter entries -->
              <LivedataAdvancedPanelFilterEntry
                :filter-index="filterIdx"
                :property-id="filterGroup.property"
              />
            </XWikiDraggableItem>
          </XWikiDraggable>

          <!-- Button to add new filter for the current Filter Group -->
          <a
            class="add-filter"
            href="#"
            @click.prevent="logic.addFilter(filterGroup.property)"
          >
            + {{ $t('livedata.panel.filter.addConstraint') }}
          </a>
        </div>

      </div> <!-- end of Filter Group -->


      <!--
        Add Filter
        When a user select an property from this select,
        it directly adds a filter group in the filter array of the Livedata config.
        If all property are sorting, it is hidden.
      -->
      <select
        class="add-filters-select"
        v-show="unfilteredProperties.length > 0"
        @change="addFilterGroup($event.target.value)"
      >
        <!--
          This is the default option that is always selected
          When the user change the value of the select,
          it adds the filter group corresponging to the option
          then falls back to this default option
        -->
        <option
          value="none"
          ref="selectFilterPropertiesNone"
          selected disabled
        >
          {{ $t('livedata.panel.filter.addProperty') }}
        </option>
        <!--
          Unfiltered properties
          Only display in the select properties that are not already filtering
        -->
        <option
          v-for="property in unfilteredProperties"
          :key="property.id"
          :value="property.id"
          v-text="property.name"
        ></option>
      </select>

    </template>

  </LivedataBaseAdvancedPanel>
</template>


<script>
import LivedataBaseAdvancedPanel from "./LivedataBaseAdvancedPanel.vue";
import LivedataAdvancedPanelFilterEntry from "./LivedataAdvancedPanelFilterEntry.vue";
import XWikiDraggable from "../utilities/XWikiDraggable.vue";
import XWikiDraggableItem from "../utilities/XWikiDraggableItem.vue";
import XWikiIcon from "../utilities/XWikiIcon";

export default {

  name: "LivedataAdvancedPanelFilter",

  components: {
    XWikiIcon,
    LivedataBaseAdvancedPanel,
    LivedataAdvancedPanelFilterEntry,
    XWikiDraggable,
    XWikiDraggableItem,
  },

  inject: ["logic"],

  props: {'panel': Object},

  computed: {

    data () { return this.logic.data; },

    // The filterable properties that don't have a filter group in the live data query.
    unfilteredProperties () {
      return this.logic.getUnfilteredProperties().map(property => this.logic.getPropertyDescriptor(property));
    },

  },


  methods: {
    // Change event handler called by the add-filters select
    addFilterGroup (value) {
      if (value === "none") { return; }
      this.logic.addFilter(value);
      this.$refs.selectFilterPropertiesNone.selected = true;
    },
    // Event handler called when filter entries are dragged and dropped
    // When a property is simply reodered, it dispatches only one "move" event
    // When a property is move between two different properties
    // it dispatches two event:
    // - a "remove" event for the property that had the moved property
    // - a "added" evnet for the property that receive the moved property
    reorderFilter (e, filterGroup) {
      // Filter entry reordered in the same property
      if (e.moved) {
        this.logic.filter(filterGroup.property, e.moved.oldIndex, {
          index: e.moved.newIndex,
        })
        .catch(err => void console.warn(err));
      }
      // Filter entry moved to another property (add handler)
      else if (e.added) {
        this.logic.addFilter(filterGroup.property, e.added.element.operator, e.added.element.value, e.added.newIndex)
        .catch(err => void console.warn(err));
      }
      // Filter entry moved to another property (remove handler)
      else if (e.removed) {
        this.logic.removeFilter(filterGroup.property, e.removed.oldIndex)
        .catch(err => void console.warn(err));
      }
    },
  },

};
</script>


<style>


.livedata-advanced-panel-filter .property-name {
  display: inline-block;
  margin-right: 0.5rem;
  padding: 5px;
  font-weight: bold;
}
.livedata-advanced-panel-filter .delete-filter-group {
  display: none;
  padding: 5px;
  color: currentColor;
}
.livedata-advanced-panel-filter .filter-group-title:hover .delete-filter-group {
  display: inline-block;
}

.livedata-advanced-panel-filter .draggable-item {
  margin-bottom: .5em;
}

.livedata-advanced-panel-filter .draggable-item .handle {
  width: 3rem;
}

.livedata-advanced-panel-filter .add-filter {
  margin-left: 3rem;
  display: inline-block;
  margin-top: 3px;
  font-style: italic;
}
.livedata-advanced-panel-filter .add-filter:hover {
  text-decoration: none;
}

.livedata-advanced-panel-filter .livedata-filter-group {
  margin-bottom: 1rem;
}

.livedata-advanced-panel-filter .add-filters-select {
  margin-top: 1rem;
}

/* Responsive mode */
@media screen and (max-width: @screen-xs-max) {
  .livedata-advanced-panel-filter .delete-filter-group {
    /* Always show the delete icon on small screens because we can't rely on hover. */
    display: inline-block;
  }
}

</style>
