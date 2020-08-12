<template>
  <livedata-base-advanced-panel
    class="livedata-advanced-panel-properties"
    panel-id="propertiesPanel"
  >

    <template #header>
      <span class="fa fa-list-ul"></span> Properties
    </template>


    <template #body>

      <xwiki-draggable
        :value="data.query.properties"
        @change="reorderProperty"
      >
        <xwiki-draggable-item
          class="property"
          v-for="property in logic.getPropertyDescriptors()"
          :key="property.id"
        >
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
          <span class="property-name">{{ property.name }}</span>
        </xwiki-draggable-item>
      </xwiki-draggable>

    </template>

  </livedata-base-advanced-panel>

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
  "vue!panels/livedata-base-advanced-panel",
  "vue!utilities/xwiki-draggable",
  "vue!utilities/xwiki-draggable-item",
], function (
  Vue
) {

  Vue.component("livedata-advanced-panel-properties", {

    name: "livedata-advanced-panel-properties",

    template: template,

    inject: ["logic"],

    computed: {
      data: function () { return this.logic.data; },

      dragOptions: function () {
        return {
          animation: 200,
          handle: ".handle",
        };
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


.livedata-advanced-panel-properties .property {
  display: flex;
  flex-direction: row;
  justify-content: flex-start;
  align-items: center;
}

.livedata-advanced-panel-properties .handle {
  padding: 6px;
  cursor: pointer; /* IE */
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
