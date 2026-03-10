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
<script setup lang="ts">
import { dropdownKey } from "./inject/keys";
import { Fragment, h, inject } from "vue";

defineSlots<{ default(): void }>();

// Retrieve the eventual parent dropdown context as we need to display the menu differently if it is in a dropdown.
const dropdownContext = inject(dropdownKey);

// Renders the div with the open class only if we are not in a dropdown
// eslint-disable-next-line @typescript-eslint/no-explicit-any
const WrapIf = (props: unknown, { slots }: { slots: any }) =>
  dropdownContext?.inDropdown !== true
    ? h("div", { class: "open" }, slots.default())
    : h(Fragment, slots.default());
</script>

<template>
  <WrapIf :class="dropdownContext?.inDropdown !== true ? $style['open'] : ''">
    <ul
      :class="[
        'dropdown-menu',
        dropdownContext?.inDropdown !== true ? $style['dropdown-menu'] : '',
      ]"
      :aria-labelledby="dropdownContext?.activatorId"
    >
      <slot></slot>
    </ul>
  </WrapIf>
</template>

<style module>
.open .dropdown-menu {
  display: inline-block;
}
.dropdown-menu {
  float: initial;
  position: initial;
}
</style>
