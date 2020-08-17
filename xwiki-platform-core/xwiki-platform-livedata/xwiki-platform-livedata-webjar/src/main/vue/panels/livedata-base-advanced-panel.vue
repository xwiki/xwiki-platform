<template>
  <div
    class="livedata-advanced-panel panel panel-default"
    v-show="panelOpened"
  >
    <div class="panel-heading">
      <span>
        <slot name="header"></slot>
      </span>
      <span
        class="close-button"
        @click="panelOpened = false"
      >
        <span class="fa fa-times"></span>
      </span>
    </div>

    <div class="panel-body">
      <slot name="body"></slot>
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
  "vue",
], function (
  Vue
) {

  Vue.component("livedata-base-advanced-panel", {

    name: "livedata-base-advanced-panel",

    props: {
      logic: Object,
      triggerEventName: String,
    },

    data: function () {
      return {
        panelOpened: false,
      };
    },

    computed: {
      data: function () { return this.logic.data; },
    },

    mounted: function () {
      var self = this;
      // listen for event from the dropdown
      this.logic.onEvent(this.triggerEventName, function () {
        self.panelOpened = !self.panelOpened;
      });
    },


  });
});
</script>


<style>

  .livedata-advanced-panel .panel-heading {
    position: relative;
    display: flex;
    flex-direction: row;
    justify-content: space-between;
    align-items: center;
  }

  .close-button {
    position: absolute;
    top: 0; right: 0;
    height: 100%;
    display: flex;
    justify-content: center;
    align-items: center;
    padding: 0 15px;
    cursor: pointer;
  }

</style>
