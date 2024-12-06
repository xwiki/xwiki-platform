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
import CMain from "./c-main.vue";
import CTemplate from "./c-template.vue";
import { ViewportType, useViewportType } from "../composables/viewport";
import { Ref, onMounted, ref, watch } from "vue";
import "../css/main.css";

const viewportType: Ref<ViewportType> = useViewportType();
// By default, main sidebar is collapsed on mobile only.
const isMainSidebarCollapsed: Ref<boolean> = ref(
  viewportType.value == ViewportType.Mobile,
);

onMounted(() => {
  // Attempt to load collapsed state from local storage.
  if (viewportType.value == ViewportType.Desktop) {
    isMainSidebarCollapsed.value =
      localStorage.isMainSidebarCollapsed === "true";
  }
});

watch(viewportType, (newViewportType: ViewportType) => {
  // Collapse main sidebar on smaller viewport,
  // load previous state from local storage on larger viewport.
  if (newViewportType == ViewportType.Mobile) {
    isMainSidebarCollapsed.value = true;
  } else {
    isMainSidebarCollapsed.value =
      localStorage.isMainSidebarCollapsed === "true";
  }
});

function onCollapseMainSidebar() {
  // main sidebar should always be collapsed on mobile.
  if (viewportType.value == ViewportType.Desktop) {
    isMainSidebarCollapsed.value = !isMainSidebarCollapsed.value;
    localStorage.isMainSidebarCollapsed = isMainSidebarCollapsed.value;
  }
}
</script>
<template>
  <div>
    <!-- Lazy component in charge of loading design-system specific resources.
    For instance, CSS sheets. -->
    <x-load></x-load>
    <div
      id="view"
      class="wrapper"
      :class="{ 'sidebar-is-collapsed': isMainSidebarCollapsed }"
    >
      <UIX uixname="view.before" />
      <CTemplate
        name="sidebar"
        @collapse-main-sidebar="onCollapseMainSidebar"
      />
      <CTemplate name="header" />

      <c-main></c-main>

      <!-- TODO CRISTAL-165: Eventually we will need a right sidebar-->
      <!-- <c-secondary-sidebar></c-secondary-sidebar> -->

      <UIX uixname="view.after" />
    </div>
  </div>
</template>
<style scoped>
:global(*) {
  box-sizing: border-box;
}
:global(.xw-cristal) {
  container: xwCristal;
  container-type: size;
  overflow: hidden;
  position: relative;
  height: 100%;
  font: var(--cr-base-font-size) var(--cr-font-sans);
  font-weight: var(--cr-font-weight-normal);
  line-height: var(--cr-line-height-normal);
  -moz-osx-font-smoothing: grayscale;
  -webkit-font-smoothing: antialiased;
}
:global(.xw-cristal),
:global(.xw-cristal > div) {
  height: 100%;
}

:deep(.doc-content),
:deep(.doc-header-inner),
:deep(.doc-info-extra) {
  max-width: var(--cr-sizes-max-page-width);
  width: 100%;
  justify-self: center;
}
:deep(.doc-header) {
  position: sticky;
  top: 0;
}
.wrapper {
  display: grid;
  grid-template-columns: auto 1fr;
  grid-template-rows: auto 1fr;
  grid-column-gap: var(--cr-spacing-2x-large);
  grid-row-gap: 0px;
  grid-template-areas:
    "main-sidebar wiki-header"
    "main-sidebar main-content"
    "main-sidebar wiki-footer";
  height: 100%;
}
.wrapper:has(.secondary-sidebar) {
  grid-template-columns: auto 1fr auto;
  grid-template-rows: auto 1fr auto;
  grid-template-areas:
    "main-sidebar wiki-header secondary-sidebar"
    "main-sidebar main-content secondary-sidebar"
    "main-sidebar wiki-footer secondary-sidebar";
}

main {
  grid-area: main-content;
  overflow: hidden;
}

:deep(.main-sidebar) {
  grid-area: main-sidebar;
  width: var(--cr-sizes-main-sidebar-width);
  min-width: var(--cr-sizes-main-sidebar-min-width);
  max-width: 100%;
  position: relative;
  background-color: var(--cr-color-neutral-50);
  display: flex;
  flex-flow: column;
  gap: var(--cr-spacing-small);
  padding: var(--cr-spacing-small);
  overflow: hidden;
  border-right: 1px solid var(--cr-color-neutral-200);
  box-shadow: var(--cr-shadow-large);
  transition: var(--cr-transition-medium) translate ease-in-out;
  z-index: 3;
}

/*
TODO: these rules about opening and closing the sidebar should be better organized and described
*/

:deep(.close-sidebar) {
  display: none;
}

:deep(.pin-sidebar) {
  display: none;
}

:deep(.open-sidebar:hover),
:deep(.close-sidebar:hover),
:deep(.pin-sidebar:hover),
:deep(.hide-sidebar:hover) {
  cursor: pointer;
}

:deep(.wrapper .sidebar-collapse-controls) {
  display: none;
}
/*Rule for the sidebar when floating over content*/
:deep(.wrapper.sidebar-is-collapsed #sidebar:not(.is-visible)) {
  display: none;
}

:deep(.wrapper.sidebar-is-collapsed main) {
  left: var(--cr-sizes-collapsed-main-sidebar-width);
}

/*rules for the sidebar when collapsed */
:deep(.wrapper.sidebar-is-collapsed) {
  grid-template-columns: var(--cr-sizes-collapsed-main-sidebar-width) 1fr;
  .main-sidebar {
    position: absolute;
    top: 0;
    bottom: 0;
  }
}

:deep(.wrapper.sidebar-is-collapsed:has(.is-visible) .collapsed-main-sidebar) {
  display: none;
}

:deep(.wrapper.sidebar-is-collapsed .sidebar-collapse-controls) {
  display: flex;
  flex-flow: row;
  justify-content: space-between;
  z-index: 2;

  & span {
    font-size: 1rem;
    height: 1rem;
    line-height: 1rem;
    display: block;
    padding: 0;
    margin: 0;
  }
}

:deep(.wrapper.sidebar-is-collapsed .hide-sidebar) {
  display: none;
}

:deep(.wrapper.sidebar-is-collapsed .pin-sidebar) {
  display: block;
}

:deep(.wrapper.sidebar-is-collapsed .close-sidebar) {
  display: block;
}

:deep(.collapsed-main-sidebar) {
  display: none;
  width: var(--cr-sizes-collapsed-main-sidebar-width);
  padding: var(--cr-spacing-small) 0;
  text-align: center;
  z-index: 1;
  grid-area: main-sidebar;
}

:deep(.wrapper.sidebar-is-collapsed .collapsed-main-sidebar) {
  display: block;
}

/*LINKS*/

:deep(.content a) {
  text-decoration: underline;
  text-underline-offset: 4px;
  font-weight: var(--cr-font-weight-semibold);
  text-decoration-color: var(--cr-color-neutral-300);
  color: var(--cr-base-link-color);

  &:visited {
    text-decoration-color: var(--cr-color-neutral-300);
    color: var(--cr-base-visited-link-color);
  }
  &:hover {
    text-decoration-color: var(--cr-color-neutral-500);
  }
}

/*TABLE*/
:deep(table) {
  border-collapse: collapse;
  overflow-x: auto;
  font-size: var(--cr-font-size-small);
  line-height: var(--cr-line-height-dense);
  & th,
  td {
    text-align: start;
  }
  & th {
    background-color: var(--cr-color-neutral-100);
    padding: var(--cr-spacing-x-small);
    font-weight: var(--cr-font-weight-semibold);

    &:first-child {
      border-top-left-radius: var(--cr-border-radius-large);
    }
    &:last-child {
      border-top-right-radius: var(--cr-border-radius-large);
    }
  }
  & td {
    padding: var(--cr-spacing-small) var(--cr-spacing-x-small);
  }
  tbody {
    & tr {
      border-bottom: 1px solid var(--cr-color-neutral-200);

      &:last-child {
        border-bottom: 0;
      }
      & .mobile-column-name {
        display: none;
      }
    }
  }
}

/*
WIKI STYLES
TODO: Discuss and move them to a more appropriate place
*/
:deep(.wikiexternallink) {
  font-style: italic;
}

@container xwCristal (max-width: 600px) {
  :deep(.wrapper.sidebar-is-collapsed) {
    &:has(.main-sidebar.is-visible) {
      &:before {
        content: " ";
        background-color: var(--cr-overlay-background-color);
        position: fixed;
        width: unset;
        top: 0;
        left: 0;
        right: 0;
        bottom: 0;
        z-index: 3;
      }
    }
  }

  .main-sidebar {
    width: 80%;
  }

  main {
    left: var(--cr-sizes-collapsed-main-sidebar-width);
  }

  :deep(.wrapper .sidebar-collapse-controls .pin-sidebar),
  :deep(.wrapper.sidebar-is-collapsed .sidebar-collapse-controls .pin-sidebar) {
    display: none;
  }

  .resize-handle {
    display: none;
  }
  :deep(table.mobile-transform) {
    & thead {
      & th {
        display: none;
      }
    }
    & tbody {
      display: grid;
      gap: 8px;
      & tr {
        display: grid;
        border: 1px solid var(--cr-color-neutral-200);
        border-radius: var(--cr-border-radius-medium);
        & td {
          & span.mobile-column-name {
            display: block;
            font-weight: var(--cr-font-weight-bold);
          }
        }
      }
    }
  }
}
</style>
