<!--
 * See the LICENSE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 *
 * This file is part of the Cristal Wiki software prototype
 * @copyright  Copyright (c) 2023 XWiki SAS
 * @license    http://opensource.org/licenses/AGPL-3.0 AGPL-3.0
 *
-->
<script lang="ts" setup>
import CConfigMenu from "./c-config-menu.vue";
import CSidebarPanel from "./c-sidebar-panel.vue";
import CHelp from "./c-help.vue";
import xlogo from "../images/xwiki-logo-color.svg";
import { CIcon } from "@cristal/icons";

const logo = xlogo;
</script>
<template>
  <div class="collapsed-sidebar">
    <!-- When the user click this icon (visible only on MOBILE or with a COLLAPSED sidebar) a function should ADD the class is-visible to the element #sidebar. -->
    <c-icon name="list"></c-icon>
  </div>
  <c-navigation-drawer id="sidebar" class="left-sidebar">
    <UIX uixname="sidebar.before" />
    <div class="sidebar-collapse-controls">
      <!-- When the user click this icon (visible only on DESKTOP and MOBILE) a function should REMOVE the class is-visible to the element #sidebar..-->
      <c-icon name="x-lg" class="close-sidebar"></c-icon>

      <!-- When the user click this icon (visible only on DESKTOP) a function should REMOVE the class sidebar-is-collapsed of the element .wrapper in c-view.vue..-->
      <c-icon name="pin" class="pin-sidebar"></c-icon>
    </div>
    <div class="sidebar-header">
      <!-- When the user click this icon (visible only on DESKTOP) a function should ADD the class sidebar-is-collapsed to the element .wrapper in c-view.vue.-->
      <c-icon name="list" class="hide-sidebar"></c-icon>
      <x-img class="logo" :src="logo" />
      <c-icon name="bell"></c-icon>
      <c-config-menu></c-config-menu>
      <x-avatar class="avatar"></x-avatar>
    </div>
    <div class="search">
      <x-search></x-search>
    </div>
    <div class="panel-container">
      <c-sidebar-panel name="Wiki Name"></c-sidebar-panel>
      <c-sidebar-panel name="Applications"></c-sidebar-panel>
      <UIX uixname="sidebar.after" />
    </div>
    <c-help></c-help>

    <!-- This handle should update the value of the CSS var --cr-sizes-left-sidebar-width in a <style> tag. This variable could also be saved and retrieved from local storage, if unavailable the default value on style.css from the DS will be used-->
    <div class="resize-handle"></div>
  </c-navigation-drawer>
</template>
<style scoped>
.panel-container {
  display: flex;
  flex-flow: column;
  height: 100%;
  gap: var(--cr-spacing-x-small);
  padding: 0 var(--cr-spacing-x-small);
  overflow: auto;
}

.search {
  padding: 0 var(--cr-spacing-x-small);
}

.sidebar-header {
  display: flex;
  flex-wrap: wrap;
  flex-flow: row;
  align-items: center;
  gap: var(--cr-spacing-x-small);
  padding: var(--cr-spacing-small) var(--cr-spacing-x-small) 0;
}

.avatar {
  --size: 2rem;
}

.logo {
  margin-right: auto;
}

.resize-handle {
  width: 16px;
  border-right: 2px solid transparent;
  position: absolute;
  right: 0;
  top: 0;
  bottom: 0;
  transition: border-color var(--cr-transition-medium) ease;
}

.resize-handle:hover {
  cursor: col-resize;
  border-right: 2px solid var(--cr-color-neutral-300);
}
</style>
