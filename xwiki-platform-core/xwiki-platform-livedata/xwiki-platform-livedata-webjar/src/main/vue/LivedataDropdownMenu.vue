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
  LivedataDropdownMenu is a component that propose different actions
  to the user: chaging layout, opning advance configuration panels, ...
  It should be included once in every layout component,
  generaly on the leftmost of its topbar so that it stay a consistent place.
-->
<template>
  <!--
    The Livedata Dropdown
    Uses the Bootstrap 3 dropdown syntax.
  -->
  <div class="livedata-dropdown-menu btn-group">

    <!-- Drowpdown open / close button-->
    <a
      class="btn btn-default dropdown-toggle"
      title="More Actions"
      data-toggle="dropdown"
      aria-haspopup="true"
      aria-expanded="true"
      role="button"
    >
      <span class="fa fa-ellipsis-v"></span>
    </a>

    <!-- Drowpdown body -->
    <ul class="dropdown-menu">

      <!-- Change layout Section -->
      <li class="dropdown-header">Change Layout</li>

      <!-- Layout options -->
      <li
        v-for="layout in logic.config.meta.layouts"
        :key="layout.id"
      >
        <a href="#" @click.prevent="logic.layout.change({ layoutId: layout.id })">
          <XWikiIcon :icon-descriptor="layout.icon"></XWikiIcon>
          {{ layout.name }}
        </a>
      </li>

      <!-- Advanced panels Section -->
      <li class="dropdown-header">Actions</li>

      <!-- Properties Panel -->
      <li>
        <a href="#" @click.prevent="logic.uniqueArrayToggle(logic.openedPanels, 'propertiesPanel')">
          <span class="fa fa-list-ul"></span> Advanced Properties
        </a>
      </li>

      <!-- Sort Panel -->
      <li>
        <a href="#" @click.prevent="logic.uniqueArrayToggle(logic.openedPanels, 'sortPanel')">
          <span class="fa fa-sort"></span> Advanced Sorting
        </a>
      </li>

      <!-- Filter Panel -->
      <li>
        <a href="#" @click.prevent="logic.uniqueArrayToggle(logic.openedPanels, 'filterPanel')">
          <span class="fa fa-filter"></span> Advanced Filtering
        </a>
      </li>


      <!-- Design mode Section -->
      <li class="dropdown-header">Design</li>

      <li v-show="!logic.designMode.activated">
        <a href="#" @click.prevent="switchToDesignMode">
          <span class="fa fa-pencil"></span> Switch to Design Mode
        </a>
      </li>

    </ul>

  </div>
</template>


<script>
import XWikiIcon from "./utilities/XWikiIcon.vue";
import { askYesNo } from "./utilities/XWikiDialogYesNo";

export default {

  name: "LivedataDropdownMenu",

  components: {
    XWikiIcon,
  },

  inject: ["logic"],

  methods: {
    async switchToDesignMode () {
      if (/* (TODO) USER IS THE ONLY ONE IN THE REALTIME SESSION */ true
        && !this.logic.temporaryConfig.equals({ configName: "initial" })) {
        const response = await askYesNo({
          title: "Keep changes for design mode?",
          text: "Your Livedata configuration have been modified, do you want to keep it for design mode?",
          yesText: "Keep current",
          noText: "Revert to default",
        });
        if (!response) { return; }
        if (response === "no") {
          this.logic.temporaryConfig.load({ configName: "initial" });
        }
      }
      this.logic.designMode.toggle({ on: true });
    },

  },

};
</script>


<style>

.livedata-dropdown-menu .btn-default {
  background-color: #f8f8f8;
  background-image: none;
  border-color: #e5e5e5;
  box-shadow: none;
  color: #333333;
  text-shadow: none;
}

.livedata-dropdown-menu .btn-default span {
  vertical-align: middle;
}

</style>
