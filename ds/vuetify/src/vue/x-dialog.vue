<script lang="ts" setup>
import { ref, defineProps } from "vue";

const props = defineProps<{
  isActive: boolean;
  title: string;
  type: number;
  width: number;
}>();
const innerIsActive = ref(props.isActive);
</script>

<template>
  <v-dialog v-model="innerIsActive" :width="width">
    <template #activator>
      <span v-bind="props">
        <slot name="activator" />
      </span>
    </template>
    <template #default>
      <v-card :title="title">
        <v-card-text>
          <slot name="default" v-bind="innerIsActive" />
        </v-card-text>
      </v-card>
    </template>
  </v-dialog>
</template>
