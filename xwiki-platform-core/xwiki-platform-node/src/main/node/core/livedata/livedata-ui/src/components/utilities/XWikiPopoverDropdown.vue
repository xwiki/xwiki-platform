<!--
  See the NOTICE file distributed with this work for additional
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

<!--
  XWikiPopoverDropdown is a Bootstrap 3 dropdown whose menu opens as a popover of its toggle button.
  The popover is displayed in the top layer, so the containers of the dropdown can't clip its menu,
  for instance when they scroll their overflowing content. The menu is anchored to the toggle: below
  it, or above it when there is more room above, and it scrolls when it doesn't fit on either side.

  The toggle content goes in the "toggle" slot and the menu items in the default slot, for example:

    <XWikiPopoverDropdown
      class="btn-group"
      toggle-title="More actions"
      menu-class="dropdown-menu-right"
    >
      <template #toggle><XWikiIcon :icon-descriptor="{ name: 'more-vertical' }" /></template>
      <li><a href="#">Refresh</a></li>
    </XWikiPopoverDropdown>
-->
<script setup lang="ts">
import { ref, useTemplateRef } from "vue";

const {
  toggleClass = "btn btn-default",
  toggleTitle,
  menuClass,
  closeOnClick = true,
} = defineProps<{
  // The classes of the toggle button, in addition to dropdown-toggle.
  toggleClass?: string | string[] | Record<string, boolean>;
  toggleTitle?: string;
  // The classes of the menu, in addition to dropdown-menu, e.g. dropdown-menu-right to align it
  // with the right side of the toggle.
  menuClass?: string | string[] | Record<string, boolean>;
  // Like a Bootstrap dropdown, the menu closes on any click inside it, unless this is false, e.g.
  // when the menu holds a form.
  closeOnClick?: boolean;
}>();

const menuId = `xwiki-popover-dropdown-${nextMenuNumber()}`;
const isOpen = ref(false);
const menu = useTemplateRef<HTMLElement>("menu");

function onMenuClick() {
  if (closeOnClick) {
    menu.value?.hidePopover();
  }
}

function onMenuToggle(event: ToggleEvent) {
  isOpen.value = event.newState === "open";
}
</script>

<script lang="ts">
// The menu identifiers must be unique in the page, across Vue applications, since there is for
// instance one application per Live Data, which rules out useId.
let menuCount = 0;

function nextMenuNumber() {
  return ++menuCount;
}
</script>

<template>
  <div class="xwiki-popover-dropdown" :class="{ open: isOpen }">
    <!--
      The Bootstrap open class gives the toggle its pressed look while the menu is open. The root
      has no sibling comment, so that the classes given to the component apply to it.
    -->
    <button
      type="button"
      class="dropdown-toggle"
      :class="toggleClass"
      :title="toggleTitle"
      :popovertarget="menuId"
      aria-haspopup="true"
      :aria-expanded="String(isOpen)"
    >
      <slot name="toggle" />
    </button>
    <!--
      The keys handled by the Bootstrap dropdown script are kept from it: that script would prevent
      Escape from closing the popover, and would close the menu on the arrow and space keys.
    -->
    <ul
      :id="menuId"
      ref="menu"
      popover
      class="dropdown-menu"
      :class="menuClass"
      @click="onMenuClick"
      @keydown.esc.up.down.space.stop
      @toggle="onMenuToggle"
    >
      <slot />
    </ul>
  </div>
</template>

<style>
/* The Bootstrap open class styles the toggle, but only the popover shows the menu. */
.xwiki-popover-dropdown > .dropdown-menu:not(:popover-open) {
  display: none;
}

/*
 * The popover keeps its fixed position, which Bootstrap overrides, so that it is placed relatively
 * to the viewport. The scrolling fallbacks come last, so that the full menu is preferred on either
 * side of the toggle.
 */
.xwiki-popover-dropdown > .dropdown-menu:popover-open {
  display: block;
  position: fixed;
  inset: auto;
  position-area: block-end span-inline-end;
  position-try-fallbacks:
    flip-block,
    --xwiki-popover-dropdown-scroll,
    --xwiki-popover-dropdown-scroll flip-block;
  position-try-order: most-block-size;
  overflow-y: auto;
}

.xwiki-popover-dropdown > .dropdown-menu-right:popover-open {
  position-area: block-end span-inline-start;
}

@position-try --xwiki-popover-dropdown-scroll {
  /* Leave room for the margin between the menu and its toggle. */
  max-height: calc(100% - 2px);
}
</style>
