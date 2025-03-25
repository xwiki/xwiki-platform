<script setup lang="ts">
import type { TextFieldProps } from "@xwiki/cristal-dsapi";
import "@shoelace-style/shoelace/dist/components/input/input";

defineProps<TextFieldProps>();

const input = defineModel<string>();
</script>

<template>
  <sl-input
    :label="label"
    :name="name"
    :help-text="help"
    :required="required"
    :autofocus.attr="autofocus"
    :value="input"
    :readonly="readonly"
    :type="type ?? 'text'"
    @input="input = $event.target.value"
  ></sl-input>
</template>

<style scoped></style>
