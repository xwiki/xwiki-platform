<template>
  <div class="livedata-layout-cards">

    <!-- Topbar -->
    <livedata-topbar :logic="logic">
      <template #left>
        <livedata-dropdown-menu :logic="logic"></livedata-dropdown-menu>
        <livedata-entry-selector-all :logic="logic"></livedata-entry-selector-all>
      </template>
      <template #right>
        <livedata-pagination :logic="logic"></livedata-pagination>
      </template>
    </livedata-topbar>

    <!-- Entry selector info bar -->
    <livedata-entry-selector-info-bar
      :logic="logic"
    ></livedata-entry-selector-info-bar>


    <!-- Cards component -->
    <div class="livedata-cards">

    <!-- A card -->
      <div
        class="card"
        v-for="(entry, entryId) in entries"
        :key="entryId"
      >
        <!-- Cartd title-->
        <div class="card-title">
          <livedata-entry-selector
            :entry="entry"
            :logic="logic"
          ></livedata-entry-selector>

          <h2 v-if="!!titlePropertyId && logic.isPropertyVisible(titlePropertyId)">
            <livedata-displayer
              :property-id="titlePropertyId"
              :entry="entry"
              :logic="logic"
            ></livedata-displayer>
          </h2>
        </div>

        <!-- Card properties-->

        <draggable
          v-model="data.meta.propertyDescriptors"
          v-bind="dragOptions"
        >
          <div
            class="card-property"
            v-for="property in properties"
            :key="property.id"
            v-show="logic.isPropertyVisible(property.id) && property.id !== titlePropertyId"
          >
            <div class="handle">
              <span class="fa fa-ellipsis-v"></span>
            </div>
            <strong class="property-name">{{ property.name }}:</strong>
            <span
              class="value"
            >
              <livedata-displayer
                :property-id="property.id"
                :entry="entry"
                :logic="logic"
              ></livedata-displayer>
            </span>
          </div>
        </div>
      </draggable>


    </div>

  </div>
</template>


<script>
/*
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
 */
define([
  "Vue",
  "vuedraggable",
  "vue!livedata-topbar.vue",
  "vue!livedata-dropdown-menu",
  "vue!livedata-entry-selector-all",
  "vue!livedata-pagination",
  "vue!displayers/livedata-displayer.vue",
  "vue!livedata-entry-selector.vue",
  "vue!livedata-entry-selector-info-bar",
], function (
  Vue,
  vuedraggable
) {

  Vue.component("livedata-layout-cards", {

    name: "livedata-layout-cards",

    template: template,

    components: {
      "draggable": vuedraggable,
    },

    props: {
      logic: Object,
    },

    computed: {
      data: function () { return this.logic.data; },
      entries: function () { return this.logic.data.data.entries; },

      properties: function () {
        return this.logic.getVisiblePropertyDescriptors();
      },

      titlePropertyId: function () {
        return this.logic.getLayoutDescriptor("cards").titleProperty;
      },

      dragOptions: function () {
        return {
          animation: 200,
          handle: ".handle",
        };
      },

    },

  });
});
</script>


<style>

.livedata-layout-cards .card {
  display: inline-block;
  padding: 1rem 2rem;
  margin: 1rem;
  border: 1px solid lightgray;
  border-radius: 1rem;
}

.livedata-layout-cards .card-title {
  display: flex;
  flex-direction: row;
  justify-content: flex-start;
  align-items: center;
}

.livedata-layout-cards .card-property {
  display: flex;
  flex-direction: row;
  justify-content: flex-start;
  align-items: center;
  transition: height 0.5s;
}

.livedata-layout-cards .handle {
  height: 100%;
  margin-left: -8px;
  padding: 0px 8px;
  cursor: pointer; /* IE */
  cursor: grab;
  opacity: 0;
}
.livedata-layout-cards .card-property:hover .handle {
  opacity: 1;
  transition: opacity 0.2s;
}
.livedata-layout-cards .handle .fa {
  vertical-align: middle;
}

.livedata-layout-cards .livedata-entry-selector {
  width: unset;
  padding: 10px;
  margin-left: -10px;
}
.livedata-layout-cards h2 {
  margin-top: 10px;
  font-size: 20px;
}


.livedata-layout-cards .property-name {
  margin-right: 0.5em;
}

.livedata-layout-cards .value {
  flex-grow: 1;
  align-self: stretch;
}

@supports (display: grid) {

  .livedata-layout-cards .livedata-cards {
    display: grid;
    grid-template-columns: repeat(auto-fill, 30rem);
    gap: 1.5rem;
  }

  .livedata-layout-cards .card {
    margin: 0;
  }

}


</style>
