<!--
See the LICENSE file distributed with this work for additional
information regarding copyright ownership.

This is free software; you can redistribute it and/or modify it
under the terms of the GNU Lesser General Public License as
published by the Free Software Foundation; either version 2.1 of
the License, or (at your option) any later version.

This software is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public
License along with this software; if not, write to the Free
Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
02110-1301 USA, or see the FSF site: http://www.fsf.org.
-->
<script lang="ts" setup>
import CHelp from "./c-help.vue";
import CNavigationDrawer from "./c-navigation-drawer.vue";
import CPageCreationMenu from "./c-page-creation-menu.vue";
import CSidebarPanel from "./c-sidebar-panel.vue";
import { useMouseCoordinates } from "../composables/mouse";
import { ViewportType, useViewportType } from "../composables/viewport";
import xlogo from "../images/xwiki-logo-color.svg";
import {
  DocumentService,
  name as documentServiceName,
} from "@xwiki/cristal-document-api";
import { CIcon } from "@xwiki/cristal-icons";
import { UIExtensions } from "@xwiki/cristal-uiextension-ui";
import { Ref, inject, onMounted, ref, watch } from "vue";
import type { CristalApp, PageData } from "@xwiki/cristal-api";

const logo = xlogo;
const viewportType: Ref<ViewportType> = useViewportType();
const { x } = useMouseCoordinates();
// By default, main sidebar is closed on mobile only.
const isSidebarClosed: Ref<boolean> = ref(
  viewportType.value == ViewportType.Mobile,
);

const cristal: CristalApp = inject<CristalApp>("cristal")!;
const documentService = cristal
  .getContainer()
  .get<DocumentService>(documentServiceName);
const currentPage: Ref<PageData | undefined> =
  documentService.getCurrentDocument();

defineEmits(["collapseMainSidebar"]);

onMounted(() => {
  // Load and apply main sidebar size from local storage, if available.
  if (localStorage.mainSidebarWidth) {
    updateMainSidebarWidth(localStorage.mainSidebarWidth);
  }
  // If main sidebar is collapsed on desktop, it should also be closed.
  if (viewportType.value == ViewportType.Desktop) {
    isSidebarClosed.value = localStorage.isMainSidebarCollapsed === "true";
  }
});

watch(viewportType, (newViewportType: ViewportType) => {
  // Always close main sidebar when switching to a smaller viewport
  if (newViewportType == ViewportType.Mobile) {
    isSidebarClosed.value = true;
  }
});

let mainSidebarResizeInterval: number = 0;

function updateMainSidebarWidth(newSidebarWidth: number) {
  document.documentElement.style.setProperty(
    "--cr-sizes-main-sidebar-width",
    `${newSidebarWidth}px`,
  );
}

function startMainSidebarResize() {
  // Check that no other interval exists before scheduling one.
  if (mainSidebarResizeInterval == 0) {
    mainSidebarResizeInterval = setInterval(() => {
      let newSidebarWidth = x.value + 8;
      updateMainSidebarWidth(newSidebarWidth);
      localStorage.mainSidebarWidth = newSidebarWidth;
    }, 10);
  }
  window.addEventListener("mouseup", endMainSidebarResize);
  window.addEventListener("touchend", endMainSidebarResize);
}

function endMainSidebarResize() {
  clearInterval(mainSidebarResizeInterval);
  mainSidebarResizeInterval = 0;
  window.removeEventListener("mouseup", endMainSidebarResize);
  window.removeEventListener("touchend", endMainSidebarResize);
}

function onOpenMainSidebar() {
  isSidebarClosed.value = false;
  window.addEventListener("click", onClickOutsideMainSidebar);
}

function onCloseMainSidebar() {
  isSidebarClosed.value = true;
  window.removeEventListener("click", onClickOutsideMainSidebar);
}

function onClickOutsideMainSidebar() {
  // We need to get the actual size of the main sidebar at any time
  // since it may be a relative value in some cases.
  let currentSidebarWidth: number = parseInt(
    window.getComputedStyle(document.getElementById("sidebar")!).width,
  );
  if (x.value > currentSidebarWidth) {
    onCloseMainSidebar();
  }
}
</script>
<template>
  <div class="collapsed-main-sidebar" @click="onOpenMainSidebar">
    <c-icon name="list" class="open-sidebar"></c-icon>
  </div>
  <c-navigation-drawer
    id="sidebar"
    class="main-sidebar"
    :class="{ 'is-visible': !isSidebarClosed }"
  >
    <UIX uixname="sidebar.before" />
    <div class="sidebar-collapse-controls">
      <c-icon
        name="x-lg"
        class="close-sidebar"
        @click="onCloseMainSidebar()"
      ></c-icon>

      <c-icon
        name="pin"
        class="pin-sidebar"
        @click="$emit('collapseMainSidebar')"
      ></c-icon>
    </div>
    <div class="sidebar-header">
      <c-icon
        name="list"
        class="hide-sidebar"
        @click="
          $emit('collapseMainSidebar');
          onCloseMainSidebar();
        "
      ></c-icon>
      <x-img class="logo" :src="logo" :width="72" />
      <suspense>
        <u-i-extensions uix-name="sidebar.actions"></u-i-extensions>
      </suspense>
    </div>
    <div class="search">
      <x-search></x-search>
    </div>
    <div class="panel-container">
      <c-sidebar-panel name="Wiki Name">
        <c-page-creation-menu
          :current-page="currentPage!"
        ></c-page-creation-menu>
        <XNavigationTree :current-page="currentPage"></XNavigationTree>
      </c-sidebar-panel>
      <c-sidebar-panel name="Applications"></c-sidebar-panel>
      <UIX uixname="sidebar.after" />
    </div>
    <c-help></c-help>

    <div
      class="resize-handle"
      @mousedown="startMainSidebarResize"
      @touchstart="startMainSidebarResize"
    ></div>
  </c-navigation-drawer>
</template>
<style scoped>
.panel-container {
  display: flex;
  flex-flow: column;
  height: 100%;
  gap: var(--cr-spacing-x-small);
  overflow-y: auto;
  scrollbar-gutter: stable;
  overflow-x: hidden;
}

.search {
  padding: 0;
}

.sidebar-header {
  display: flex;
  flex-wrap: wrap;
  flex-flow: row;
  align-items: center;
  gap: var(--cr-spacing-x-small);
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
