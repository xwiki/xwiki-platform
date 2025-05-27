<script setup lang="ts">
import { ref } from "vue";
import { VForm } from "vuetify/components/VForm";
import type { Ref } from "vue";

const form: Ref<VForm | undefined> = ref(undefined);
const emits = defineEmits(["formSubmit"]);

defineExpose({
  reset,
});

async function reset() {
  form.value?.reset();
}

async function submit() {
  const validation = await form.value?.validate();
  if (validation?.valid) {
    emits("formSubmit");
  }
}
</script>

<template>
  <v-form ref="form" @submit.prevent="submit">
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
