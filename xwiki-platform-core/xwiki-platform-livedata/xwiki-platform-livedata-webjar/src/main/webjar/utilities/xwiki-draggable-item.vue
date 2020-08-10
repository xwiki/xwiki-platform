<template>
  <div class="draggable-item">
    <div class="handle">
      <slot name="handle">
        <span class="fa fa-bars"></span>
      </slot>
    </div>

    <slot></slot>
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
  "vue!panels/livedata-base-advanced-panel",
], function (
  Vue,
  vuedraggable
) {

  Vue.component("xwiki-draggable-item", {

    name: "xwiki-draggable-item",

    template: template,

    components: {
      "draggable": vuedraggable,
    },

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


.draggable-item  {
  display: flex;
  flex-direction: row;
  justify-content: flex-start;
  align-items: center;
}

.draggable-item .handle {
  padding: 6px;
  cursor: pointer; /* IE */
  cursor: grab;
  opacity: 0;
}

.draggable-item:hover .handle {
  opacity: 1;
  transition: opacity 0.2s;
}

.draggable-item .handle .fa {
  vertical-align: middle;
}

</style>
