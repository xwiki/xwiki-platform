<script lang="ts" setup>
import "@shoelace-style/shoelace/dist/components/menu/menu";
import { defineProps } from "vue";
defineProps<{
  value: string;
}>();
</script>
<template>
  <sl-menu-item :value="value">
    <slot />
  </sl-menu-item>
</template>
