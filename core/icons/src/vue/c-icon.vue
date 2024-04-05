<script setup lang="ts">
import "bootstrap-icons/font/bootstrap-icons.css";

defineProps<{
  name: string;
}>();
// TODO: currently names are bound to bootstrap class names. We'll need to add
// an indirection as soon as we want to support several icon sets.
</script>

<template>
  <span :class="`bi-${name}`"></span>
</template>

<style scoped>
span {
  font-size: 24px;
}
</style>
