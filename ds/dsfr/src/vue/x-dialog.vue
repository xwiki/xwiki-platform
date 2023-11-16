<template>
  <span ref="modalOrigin" @click="isModalOpen = true">
    <slot name="activator" />
  </span>
  <!-- TODO: make the title configurable -->
  <DsfrModal
    v-if="isModalOpen"
    title="modalTitle"
    :opened="isModalOpen"
    @close="isModalOpen = false"
  >
    <slot name="default" />
  </DsfrModal>
</template>
<script setup lang="ts">
import { DsfrModal } from "@gouvminint/vue-dsfr";
import { ref } from "vue";
const isModalOpen = ref(false);
</script>
