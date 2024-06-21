<script setup lang="ts">
import { VForm } from "vuetify/components/VForm";
const emits = defineEmits(["formSubmit"]);
function submit() {
  emits("formSubmit");
}
</script>

<template>
  <v-form @submit.prevent="submit">
    <v-container>
      <slot></slot>
    </v-container>
  </v-form>
</template>

<style scoped></style>
