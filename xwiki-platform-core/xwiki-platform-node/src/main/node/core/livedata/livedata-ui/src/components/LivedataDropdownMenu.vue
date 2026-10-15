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
  LivedataDropdownMenu is a component that provides different actions
  to the user: switching layouts, opening advanced configuration panels, ...
  It should be included once in every layout component,
  generally at the very left of its top bar so it stays at a consistent place.
-->
<template>
  <!--
    The Livedata Dropdown
    Uses the Bootstrap 3 dropdown syntax.
  -->
  <div ref="dropdown" class="livedata-dropdown-menu btn-group">
    <!-- Drowpdown open / close button-->
    <button
      ref="toggle"
      class="btn btn-default dropdown-toggle"
      :title="$t('livedata.dropdownMenu.title')"
      data-toggle="dropdown"
      aria-haspopup="true"
      aria-expanded="false"
    >
      <XWikiIcon :icon-descriptor="{ name: 'more-vertical' }" />
    </button>

    <!-- Drowpdown body -->
    <ul ref="menu" class="dropdown-menu dropdown-menu-right" :style="menuStyle">
      <!-- Actions -->
      <li>
        <span class="dropdown-header">{{
          $t("livedata.dropdownMenu.actions")
        }}</span>
        <ul>
          <li>
            <!-- Refresh -->
            <a
              href="#"
              @click.prevent="logic.updateEntries()"
              class="livedata-action-refresh"
            >
              <XWikiIcon :icon-descriptor="{ name: 'repeat' }" />
              {{ $t("livedata.action.refresh") }}
            </a>
          </li>
          <!-- Maximized view, which displays the Live Data on its own, over the rest of the
          page. -->
          <li>
            <a
              href="#"
              @click.prevent="logic.toggleMaximized()"
              class="livedata-action-maximize"
            >
              <XWikiIcon
                :icon-descriptor="{
                  name: isMaximized ? 'minimize' : 'maximize',
                }"
              />
              {{
                isMaximized
                  ? $t("livedata.action.minimize")
                  : $t("livedata.action.maximize")
              }}
            </a>
          </li>
        </ul>
      </li>

      <!-- Layouts -->
      <li>
        <span class="dropdown-header">{{
          $t("livedata.dropdownMenu.layouts")
        }}</span>
        <ul>
          <!-- Layout options -->
          <li
            v-for="layout in data.meta.layouts"
            :key="layout.id"
            :class="{
              disabled: isCurrentLayout(layout.id),
            }"
          >
            <a href="#" @click.prevent="changeLayout(layout.id)">
              <XWikiIcon :icon-descriptor="layout.icon"></XWikiIcon>
              {{ layout.name }}
            </a>
          </li>
        </ul>
      </li>

      <!-- Panels -->
      <li>
        <span class="dropdown-header">{{
          $t("livedata.dropdownMenu.panels")
        }}</span>
        <ul>
          <li v-for="panel in logic.panels" :key="panel.id">
            <a
              href="#"
              @click.prevent="
                logic.uniqueArrayToggle(logic.openedPanels, panel.id)
              "
            >
              <XWikiIcon :icon-descriptor="{ name: panel.icon }" />
              {{ panel.name }}
            </a>
          </li>
        </ul>
      </li>
    </ul>
  </div>
</template>

<script>
import XWikiIcon from "./utilities/XWikiIcon.vue";

export default {
  name: "LivedataDropdownMenu",

  components: {
    XWikiIcon,
  },

  inject: ["logic", "jQuery"],

  data() {
    return {
      menuStyle: {},
    };
  },

  computed: {
    data() {
      return this.logic.data;
    },
    isMaximized() {
      return this.logic.isMaximized();
    },
  },

  methods: {
    isCurrentLayout(layoutId) {
      return this.logic.currentLayoutId.value === layoutId;
    },
    changeLayout(layoutId) {
      if (!this.isCurrentLayout(layoutId)) {
        this.logic.changeLayout(layoutId);
      }
    },
    /**
     * The Live Data is often displayed inside an element that clips its overflowing content, such
     * as the page content. A menu positioned relatively to its button would then be cut when the
     * Live Data has only a few entries. The menu is thus positioned relatively to the viewport,
     * below its button, or above it when there isn't enough room below. Its height is limited to
     * the room left on that side, so that a small or zoomed in viewport makes the menu scroll
     * instead of hiding its last actions.
     */
    placeMenu() {
      const button = this.$refs.toggle.getBoundingClientRect();
      const viewport = document.documentElement;
      const roomBelow = viewport.clientHeight - button.bottom;
      const roomAbove = button.top;
      const placeAbove =
        roomBelow < this.$refs.menu.offsetHeight && roomAbove > roomBelow;
      this.menuStyle = {
        position: "fixed",
        top: placeAbove ? "auto" : `${button.bottom}px`,
        bottom: placeAbove ? `${viewport.clientHeight - button.top}px` : "auto",
        right: `${viewport.clientWidth - button.right}px`,
        left: "auto",
        maxHeight: `${placeAbove ? roomAbove : roomBelow}px`,
        overflowY: "auto",
      };
    },
    onMenuShown() {
      this.placeMenu();
      // Listen during the capture phase to also catch the scroll of the containers of the Live
      // Data, which doesn't bubble.
      window.addEventListener("scroll", this.placeMenu, true);
      window.addEventListener("resize", this.placeMenu);
    },
    onMenuHidden() {
      window.removeEventListener("scroll", this.placeMenu, true);
      window.removeEventListener("resize", this.placeMenu);
      this.menuStyle = {};
    },
  },

  mounted() {
    // Bootstrap triggers its dropdown events through jQuery, they can't be listened to with Vue.
    this.jQuery(this.$refs.dropdown)
      .on("shown.bs.dropdown", this.onMenuShown)
      .on("hidden.bs.dropdown", this.onMenuHidden);
  },

  beforeUnmount() {
    this.jQuery(this.$refs.dropdown)
      .off("shown.bs.dropdown", this.onMenuShown)
      .off("hidden.bs.dropdown", this.onMenuHidden);
    this.onMenuHidden();
  },
};
</script>

<style>
.livedata-dropdown-menu {
  /* Style each section of the dropdown */
  ul.dropdown-menu > li > ul {
    list-style: none;
    padding-left: 0;
  }
}

/*
 * The icons are not all the same width,
 * so we set a fix width for the icons
 * so that all dropdown options aligned with each others
 */
.livedata-dropdown-menu li a .fa {
  width: 1.4rem;
  text-align: center;
}

.livedata-dropdown-menu .dropdown-menu {
  /* Dividers within the dropdown with an improved semantic representation */
  li:has(> ul) + li:has(> ul) {
    padding-top: calc(0.5lh - 1px);
    border-top: solid 1px var(--dropdown-divider-bg);
  }

  li:has(> ul):has(+ li > ul) {
    margin-bottom: calc(0.5lh - 1px);
  }
}
</style>
