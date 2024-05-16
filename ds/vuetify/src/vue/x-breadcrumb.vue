<script setup lang="ts">
import { BreadcrumbProps } from "@cristal/dsapi";

defineProps<BreadcrumbProps>();
</script>

<template>
  <v-breadcrumbs :items="items"></v-breadcrumbs>
</template>

<style scoped></style>
