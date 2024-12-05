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
<script setup lang="ts">
import { CIcon, Size } from "@xwiki/cristal-icons";

defineProps<{
  name: string;
}>();
</script>
<template>
  <div class="c-sidebar-panel">
    <div class="header">
      <span class="title">{{ name }}</span>
      <c-icon name="list-nested" :size="Size.Small"></c-icon>
    </div>
    <div class="sidebar-content">
      <slot></slot>
    </div>
  </div>
</template>
<style scoped>
.c-sidebar-panel {
  display: flex;
  flex-flow: column;
  gap: var(--cr-spacing-x-small);
}

.sidebar-content {
  display: flex;
  flex-flow: column;
  gap: var(--cr-spacing-x-small);
}

.header {
  display: flex;
  flex-flow: row;
}

.title {
  color: var(--cr-color-neutral-500);
  font-size: var(--cr-font-size-small);
  font-weight: var(--cr-font-weight-semibold);
  margin-right: auto;
}
</style>
