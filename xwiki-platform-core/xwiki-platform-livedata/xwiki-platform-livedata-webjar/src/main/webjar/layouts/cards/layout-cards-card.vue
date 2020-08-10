<template>
  <div class="card">
    <!-- Cartd title-->
    <div class="card-title">
      <livedata-entry-selector
        :entry="entry"
      ></livedata-entry-selector>

      <h2 v-if="!!titlePropertyId && logic.isPropertyVisible(titlePropertyId)">
        <livedata-displayer
          :property-id="titlePropertyId"
          :entry="entry"
        ></livedata-displayer>
      </h2>
    </div>

    <!-- Card properties-->

    <xwiki-draggable
      :value="logic.propertyOrder"
      @change="reorderProperty"
    >
      <xwiki-draggable-item
        class="card-property"
        v-for="property in properties"
        :key="property.id"
        v-show="logic.isPropertyVisible(property.id) && property.id !== titlePropertyId"
      >
        <template #handle>
          <span class="fa fa-ellipsis-v"></span>
        </template>
        <strong class="property-name">{{ property.name }}:</strong>
        <span
          class="value"
        >
          <livedata-displayer
            :property-id="property.id"
            :entry="entry"
          ></livedata-displayer>
        </span>
      </xwiki-draggable-item>
    </xwiki-draggable>
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
  "vue!livedata-entry-selector",
  "vue!displayers/livedata-displayer",
  "vue!utilities/xwiki-draggable",
  "vue!utilities/xwiki-draggable-item",
], function (
  Vue
) {

  Vue.component("layout-cards-card", {

    name: "layout-cards-card",

    template: template,

    inject: ["logic"],

    props: {
      entry: Object,
    },

    computed: {
      data: function () { return this.logic.data; },

      properties: function () {
        return this.logic.getDisplayablePropertyDescriptors();
      },

      titlePropertyId: function () {
        return this.logic.getLayoutDescriptor("cards").titleProperty;
      },

    },


    methods: {
      reorderProperty: function (e) {
        this.logic.reorderProperty(e.moved.element, e.moved.newIndex);
      },
    },

  });
});
</script>


<style>

.layout-cards .card {
  display: inline-block;
  padding: 1rem 2rem;
  margin: 1rem;
  border: 1px solid lightgray;
  border-radius: 1rem;
}

.layout-cards .card-title {
  display: flex;
  flex-direction: row;
  justify-content: flex-start;
  align-items: center;
}

.layout-cards .card-property {
  display: flex;
  flex-direction: row;
  justify-content: flex-start;
  align-items: center;
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

@supports (display: grid) {

  .layout-cards .livedata-cards {
    display: grid;
    grid-template-columns: repeat(auto-fill, 30rem);
    gap: 1.5rem;
  }

  .layout-cards .card {
    margin: 0;
  }

}


</style>
