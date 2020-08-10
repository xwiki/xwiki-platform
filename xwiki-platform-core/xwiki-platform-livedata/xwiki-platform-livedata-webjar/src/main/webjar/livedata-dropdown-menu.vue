<template>
  <div class="livedata-dropdown-menu btn-group">

    <!-- Drowpdown button-->
    <a class="btn btn-default dropdown-toggle" title="More Actions" data-toggle="dropdown" aria-haspopup="true" aria-expanded="true" role="button">
      <span class="fa fa-ellipsis-v"></span>
    </a>

    <!-- Drowpdown content-->
    <ul class="dropdown-menu">

      <!-- Change layout-->
      <li class="dropdown-header">Change Layout</li>

      <li
        v-for="layout in data.meta.layouts"
      >
        <a href="#" @click.prevent="logic.changeLayout(layout.id)">
          <xwiki-icon :icon-descriptor="layout.icon"></xwiki-icon>
          {{ layout.name }}
        </a>
      </li>

      <!-- Common actions -->
      <li class="dropdown-header">Actions</li>

      <li>
        <a href="#" @click.prevent="logic.uniqueArrayAdd(logic.openedPanels, 'sortPanel')">
          <span class="fa fa-sort"></span> Advanced Sorting
        </a>
      </li>

      <li>
        <a href="#" @click.prevent="logic.uniqueArrayAdd(logic.openedPanels, 'filterPanel')">
          <span class="fa fa-filter"></span> Advanced Filtering
        </a>
      </li>

      <li>
        <a href="#" @click.prevent="logic.uniqueArrayAdd(logic.openedPanels, 'propertiesPanel')">
          <span class="fa fa-list-ul"></span> Advanced Properties
        </a>
      </li>

    </ul>

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
  "vue!utilities/xwiki-icon"
], function (
  Vue
) {
  Vue.component("livedata-dropdown-menu", {

    name: "livedata-dropdown-menu",

    template: template,

    inject: ["logic"],

    computed: {
      data: function () { return this.logic.data; },
    },

  });
});
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
