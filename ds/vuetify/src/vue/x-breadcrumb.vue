<script setup lang="ts">
// eslint-disable-next-line @typescript-eslint/no-explicit-any
const items: Array<Record<string, any>> = [
  {
    title: "Home",
    href: "breadcrumbs_dashboard",
  },
  {
    title: "First",
    href: "breadcrumbs_dashboard",
  },
  {
    title: "Second",
    href: "breadcrumbs_dashboard",
  },
  {
    title: "Third",
    href: "breadcrumbs_dashboard",
  },
];
</script>

<template>
  <v-breadcrumbs :items="items"></v-breadcrumbs>
</template>

<style scoped></style>
