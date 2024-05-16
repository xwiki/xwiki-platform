<script setup lang="ts">
import "@shoelace-style/shoelace/dist/components/breadcrumb/breadcrumb";
import "@shoelace-style/shoelace/dist/components/breadcrumb-item/breadcrumb-item";
import { BreadcrumbProps } from "@cristal/dsapi";

defineProps<BreadcrumbProps>();
</script>

<template>
  <sl-breadcrumb>
    <sl-breadcrumb-item v-for="item in items" :key="item">
      {{ item }}
    </sl-breadcrumb-item>
  </sl-breadcrumb>
</template>

<style scoped></style>
