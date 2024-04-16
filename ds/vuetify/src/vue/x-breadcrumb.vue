<script setup lang="ts">
// eslint-disable-next-line @typescript-eslint/no-explicit-any
const items: Array<Record<string, any>> = [
  {
    title: "Dashboard",
    href: "breadcrumbs_dashboard",
  },
];
</script>

<template>
  <v-breadcrumbs :items="items"></v-breadcrumbs>
</template>

<style scoped></style>
