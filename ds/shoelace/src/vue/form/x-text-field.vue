<script setup lang="ts">
import { type TextFieldProps } from "@xwiki/cristal-dsapi";
import "@shoelace-style/shoelace/dist/components/input/input";

defineProps<TextFieldProps>();
</script>

<template>
  <sl-input
    :label="label"
    :name="name"
    :required="required"
    type="text"
  ></sl-input>
</template>

<style scoped></style>
