<script setup lang="ts">
import { DsfrModal } from "@gouvminint/vue-dsfr";
import { ref } from "vue";
defineProps<{
  title: string;
  type: number;
  width: number;
}>();
const isModalOpen = ref(false);
</script>
<template>
  <span ref="modalOrigin" @click="isModalOpen = true">
    <slot name="activator" />
  </span>
  <!-- TODO: make the title configurable -->
  <DsfrModal
    v-if="isModalOpen"
    :title="title"
    :opened="isModalOpen"
    @close="isModalOpen = false"
  >
    <slot name="default" />
  </DsfrModal>
</template>
