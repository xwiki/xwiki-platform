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
import XBtn from "./XBtn.vue";
import XMenu from "./XMenu.vue";
import XMenuItem from "./XMenuItem.vue";
import XMenuLabel from "./XMenuLabel.vue";
</script>

<template>
  <div>
    <h1>Menus</h1>
    <x-menu>
      <template #activator>
        <x-btn data-toggle="dropdown" variant="primary">menu button</x-btn>
      </template>
      <x-menu-label>menu label</x-menu-label>
      <x-menu-item><span tabindex="-1">menu content</span></x-menu-item>
    </x-menu>
    <x-menu :disabled="true">
      <template #activator>
        <x-btn variant="primary" :disabled="true">menu button</x-btn>
      </template>
      <x-menu-item><span tabindex="-1">menu content</span></x-menu-item>
    </x-menu>
    <div>
      Lorem ipsum dolor sit amet, consectetur adipiscing elit. Suspendisse
      volutpat pulvinar odio sit amet bibendum. Mauris leo erat, convallis eu
      velit nec, laoreet suscipit mi. Fusce tempor sed justo vel tincidunt.
      Phasellus faucibus ex enim, at pellentesque felis facilisis ac. Duis vel
      velit metus. Morbi placerat quam ut aliquam rhoncus. Nunc sed fermentum
      magna. Vivamus cursus velit suscipit tortor malesuada mollis. Vivamus
      purus mi, maximus et mattis non, rhoncus et felis. Ut sodales, orci non
      dignissim sollicitudin, sem nisl vehicula ex, vel tincidunt ante magna sit
      amet ex. Nullam lacinia sapien vel tellus scelerisque, eu dapibus lorem
      convallis. Maecenas porttitor ex molestie ipsum dignissim fringilla. Lorem
      ipsum dolor sit amet, consectetur adipiscing elit.
    </div>
  </div>
</template>

<style scoped></style>
