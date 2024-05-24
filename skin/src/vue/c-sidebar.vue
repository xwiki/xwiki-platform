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
import { Ref, onMounted, ref, watch } from "vue";
import CConfigMenu from "./c-config-menu.vue";
import CNavigationDrawer from "./c-navigation-drawer.vue";
import CSidebarPanel from "./c-sidebar-panel.vue";
import CHelp from "./c-help.vue";
import xlogo from "../images/xwiki-logo-color.svg";
import xavatarImg from "../images/no-one.svg";
import { CIcon } from "@xwiki/cristal-icons";
import { useMouseCoordinates } from "../composables/mouse";
import { ViewportType, useViewportType } from "../composables/viewport";

const logo = xlogo;
const avImg = xavatarImg;
const viewportType: Ref<ViewportType> = useViewportType();
const { x } = useMouseCoordinates();
// By default, left sidebar is closed on mobile only.
const isSidebarClosed: Ref<boolean> = ref(
  viewportType.value == ViewportType.Mobile,
);

defineEmits(["collapseLeftSidebar"]);

onMounted(() => {
  // Load and apply left sidebar size from local storage, if available.
  if (localStorage.leftSidebarWidth) {
    updateLeftSidebarWidth(localStorage.leftSidebarWidth);
  }
  // If left sidebar is collapsed on desktop, it should also be closed.
  if (viewportType.value == ViewportType.Desktop) {
    isSidebarClosed.value = localStorage.isLeftSidebarCollapsed === "true";
  }
});

watch(viewportType, (newViewportType: ViewportType) => {
  // Always close left sidebar when switching to a smaller viewport
  if (newViewportType == ViewportType.Mobile) {
    isSidebarClosed.value = true;
  }
});

// Store the interval for left sidebar resize operation.
let sidebarResizeInterval: number = 0;

function updateLeftSidebarWidth(newSidebarWidth: number) {
  document.documentElement.style.setProperty(
    "--cr-sizes-left-sidebar-width",
    `${newSidebarWidth}px`,
  );
}

function startLeftSidebarResize() {
  // Check that no other interval exists before scheduling one.
  if (sidebarResizeInterval == 0) {
    sidebarResizeInterval = setInterval(() => {
      let newSidebarWidth = x.value + 8;
      updateLeftSidebarWidth(newSidebarWidth);
      localStorage.leftSidebarWidth = newSidebarWidth;
    }, 10);
  }
  window.addEventListener("mouseup", endLeftSidebarResize);
  window.addEventListener("touchend", endLeftSidebarResize);
}

function endLeftSidebarResize() {
  clearInterval(sidebarResizeInterval);
  sidebarResizeInterval = 0;
  window.removeEventListener("mouseup", endLeftSidebarResize);
  window.removeEventListener("touchend", endLeftSidebarResize);
}

function onOpenLeftSidebar() {
  isSidebarClosed.value = false;
  window.addEventListener("click", onClickOutsideLeftSidebar);
}

function onCloseLeftSidebar() {
  isSidebarClosed.value = true;
  window.removeEventListener("click", onClickOutsideLeftSidebar);
}

function onClickOutsideLeftSidebar() {
  // We need to get the actual size of the left sidebar at any time
  // since it may be a relative value in some cases.
  let currentSidebarWidth: number = parseInt(
    window.getComputedStyle(document.getElementById("sidebar")!).width,
  );
  if (x.value > currentSidebarWidth) {
    onCloseLeftSidebar();
  }
}
</script>
<template>
  <div class="collapsed-sidebar" @click="onOpenLeftSidebar">
    <c-icon name="list" class="open-sidebar"></c-icon>
  </div>
  <c-navigation-drawer
    id="sidebar"
    class="left-sidebar"
    :class="{ 'is-visible': !isSidebarClosed }"
  >
    <UIX uixname="sidebar.before" />
    <div class="sidebar-collapse-controls">
      <c-icon
        name="x-lg"
        class="close-sidebar"
        @click="onCloseLeftSidebar()"
      ></c-icon>

      <c-icon
        name="pin"
        class="pin-sidebar"
        @click="$emit('collapseLeftSidebar')"
      ></c-icon>
    </div>
    <div class="sidebar-header">
      <c-icon
        name="list"
        class="hide-sidebar"
        @click="
          $emit('collapseLeftSidebar');
          onCloseLeftSidebar();
        "
      ></c-icon>
      <x-img class="logo" :src="logo" :width="72" />
      <c-icon name="bell"></c-icon>
      <c-config-menu></c-config-menu>
      <x-avatar class="avatar" :image="avImg" size="2rem"></x-avatar>
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

    <div
      class="resize-handle"
      @mousedown="startLeftSidebarResize"
      @touchstart="startLeftSidebarResize"
    ></div>
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
