<script lang="ts" setup>
import { defineProps } from "vue";
defineProps<{
  title: string;
}>();
</script>
<template>
  <v-menu open-on-hover>
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
