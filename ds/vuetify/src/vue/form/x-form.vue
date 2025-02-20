<script setup lang="ts">
import { VForm } from "vuetify/components/VForm";
const emits = defineEmits(["formSubmit"]);
function submit() {
  emits("formSubmit");
}
</script>

<template>
  <v-form @submit.prevent="submit">
    <slot></slot>
  </v-form>
</template>

<style scoped>
.v-form {
  display: flex;
  flex-direction: column;
  gap: var(--cr-spacing-x-small);
}
</style>
