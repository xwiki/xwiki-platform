<script lang="ts" setup>
import { defineProps } from "vue";
defineProps<{
  title: string;
  width: string | number | undefined;
}>();

// const innerIsActive = ref(props.isActive);
</script>

<template>
  <v-dialog :width="width">
    <template #activator="{ props }">
      <span v-bind="props">
        <slot name="activator" />
      </span>
    </template>
    <template #default>
      <v-card :title="title">
        <v-card-text>
          <slot name="default" />
        </v-card-text>
      </v-card>
    </template>
  </v-dialog>
</template>
