<script setup lang="ts">
const emits = defineEmits(["formSubmit"]);
function submit() {
  emits("formSubmit");
}
</script>

<template>
  <form @submit.prevent="submit">
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
