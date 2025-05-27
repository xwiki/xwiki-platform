<script setup lang="ts">
import { ref } from "vue";
import type { Ref } from "vue";

const form: Ref<HTMLFormElement | undefined> = ref(undefined);
const emits = defineEmits(["formSubmit"]);

defineExpose({
  reset,
});

async function reset() {
  form.value?.reset();
}

function submit() {
  if (form.value?.checkValidity()) {
    emits("formSubmit");
  }
}
</script>

<template>
  <form ref="form" @submit.prevent="submit">
    <slot></slot>
  </form>
</template>

<style scoped>
form {
  display: flex;
  flex-direction: column;
  gap: var(--cr-spacing-x-small);
}
</style>
