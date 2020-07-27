<template>
  <livedata-base-advanced-panel
    class="livedata-advanced-panel-properties"
    :logic="logic"
    trigger-event-name="triggerPropertiesPanel"
  >

    <template #header>
      <span class="fa fa-list-ul"></span> Properties
    </template>


    <template #body>

      <draggable
        v-model="data.meta.propertyDescriptors"
        v-bind="dragOptions"
      >
        <div
          class="property"
          v-for="property in data.meta.propertyDescriptors"
          :key="property.id"
        >
          <div class="handle">
            <span class="fa fa-bars"></span>
          </div>
          <div
            class="visibility"
            @click.self="$event.currentTarget.querySelector('input').click()"
          >
            <input
              type="checkbox"
              :checked="logic.isPropertyVisible(property.id)"
              @change="logic.setPropertyVisibility(property.id, $event.target.checked)"
            />
          </div>
          <span class="property-name">{{ property.name }}</span>
        </div>
      </draggable>

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
  "vuedraggable",
  "vue!panels/livedata-base-advanced-panel",
], function (
  Vue,
  vuedraggable
) {

  Vue.component("livedata-advanced-panel-properties", {

    name: "livedata-advanced-panel-properties",

    template: template,

    components: {
      "draggable": vuedraggable,
    },

    props: {
      logic: Object,
    },

    computed: {
      data: function () { return this.logic.data; },

      dragOptions() {
        return {
          animation: 200,
          group: "advanced-panel-properties",
          handle: ".handle",
        };
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
