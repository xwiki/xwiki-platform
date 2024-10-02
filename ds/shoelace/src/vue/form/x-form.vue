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
  padding: 16px;
}
</style>
