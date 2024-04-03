<script lang="ts" setup>
import { defineProps } from "vue";
defineProps<{
  title: string;
}>();
</script>
<template>
  <v-list-item>
    <slot name="default" />
  </v-list-item>
</template>
