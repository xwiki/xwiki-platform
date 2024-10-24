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
<script lang="ts" setup></script>
<template>
  <!-- v-menu needs to be attached inside Cristal DOM to fix its z-index.
       We also reset the default z-index to set it in the style section. -->
  <v-menu
    :close-on-content-click="false"
    attach="#xwCristalApp"
    z-index="initial"
  >
    <template #activator="{ props }">
      <span v-bind="props">
        <slot name="activator" />
      </span>
    </template>
    <v-list>
      <slot name="default" />
    </v-list>
  </v-menu>
</template>
<style scoped>
.v-overlay {
  z-index: var(--cr-z-index-dropdown);
}
</style>
