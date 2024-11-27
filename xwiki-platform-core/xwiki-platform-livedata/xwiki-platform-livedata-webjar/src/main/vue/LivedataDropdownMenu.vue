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
  <div class="livedata-dropdown-menu btn-group">

    <!-- Drowpdown open / close button-->
    <a
      class="btn btn-default dropdown-toggle"
      :title="$t('livedata.dropdownMenu.title')"
      data-toggle="dropdown"
      aria-haspopup="true"
      aria-expanded="false"
      role="button"
    >
      <XWikiIcon :icon-descriptor="{name: 'more-vertical'}" />
    </a>

    <!-- Drowpdown body -->
    <ul class="dropdown-menu dropdown-menu-right">

      <!-- Actions -->
      <li><span class="dropdown-header">{{ $t('livedata.dropdownMenu.actions') }}</span><ul>
        <li>
          <!-- Refresh -->
          <a href="#" @click.prevent="logic.updateEntries()" class="livedata-action-refresh">
            <XWikiIcon :icon-descriptor="{name: 'repeat'}" /> 
            {{ $t('livedata.action.refresh') }}
          </a>
        </li>
      </ul></li>

      <!-- Layouts -->
      <li><span class="dropdown-header">{{ $t('livedata.dropdownMenu.layouts') }}</span><ul>
        <!-- Layout options -->
        <li
          v-for="layout in data.meta.layouts"
          :key="layout.id"
          :class="{
            'disabled': isCurrentLayout(layout.id),
          }"
        >
          <a href="#" @click.prevent="changeLayout(layout.id)">
            <XWikiIcon :icon-descriptor="layout.icon"></XWikiIcon>
            {{ layout.name }}
          </a>
        </li>
      </ul></li>

      <!-- Panels -->
      <li><span class="dropdown-header">{{ $t('livedata.dropdownMenu.panels') }}</span><ul>
        <li v-for="panel in logic.panels" :key="panel.id">
          <a href="#" @click.prevent="logic.uniqueArrayToggle(logic.openedPanels, panel.id)">
            <XWikiIcon :icon-descriptor="{name: panel.icon}"/>
            {{ panel.name }}
          </a>
        </li>
      </ul></li>

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

  inject: ["logic"],

  computed: {
    data () { return this.logic.data; },
  },

  methods: {
    isCurrentLayout(layoutId) {
      return this.logic.currentLayoutId === layoutId
    },
    changeLayout(layoutId) {
      if (!this.isCurrentLayout(layoutId)) {
        this.logic.changeLayout(layoutId)
      }
    }
  }

};
</script>


<style>

.livedata-dropdown-menu {
  /* Similar to .flat-buttons() */
  .btn-default {
    background-color: @breadcrumb-bg;
    background-image: none;
    border-color: @dropdown-divider-bg;
    box-shadow: none;
    color: @dropdown-link-color;
    text-shadow: none;
  }

  .btn-default:hover, .btn-default:active, .btn-default:focus, .open .dropdown-toggle {
      border-color: darken(@dropdown-divider-bg, 10%);
  }

  /* Style each section of the dropdown */
  ul.dropdown-menu > li > ul {
    list-style: none;
    padding-left: 0;
  }
}

.livedata-dropdown-menu .btn-default span {
  vertical-align: middle;
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

</style>
