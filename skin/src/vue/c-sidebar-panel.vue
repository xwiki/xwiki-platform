<script setup lang="ts">
import { CIcon, Size } from "@cristal/icons";

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
    <div class="content">
      <slot></slot>
    </div>
  </div>
</template>
<style scoped>
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
