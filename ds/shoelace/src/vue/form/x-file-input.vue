<script setup lang="ts">
import { defineModel, ref, watch } from "vue";
import type { TextFieldModel, TextFieldProps } from "@xwiki/cristal-dsapi";
import "@shoelace-style/shoelace/dist/components/input/input";

const model = defineModel<TextFieldModel>();
defineProps<TextFieldProps>();

function change(event: Event) {
  const files = (event.target as HTMLInputElement).files;
  if (!files || files.length == 0) {
    model.value = undefined;
  } else if (files.length == 1) {
    model.value = files[0];
  } else {
    const value = [];
    for (let i = 0; i < files.length; i++) {
      value.push(files[i]);
    }
    model.value = value;
  }
}

const inputElement = ref();

watch(model, (model) => {
  if (!model) {
    inputElement.value.value = "";
  }
});
</script>

<!--
Partial clone of sl-input with a input type=file because the type file
is not supported by shoelace currently.
-->
<template>
  <div class="form-control form-control--medium form-control--has-label">
    <label class="form-control__label" for="input" :aria-hidden="false">
      <slot name="label">{{ label }}</slot>
    </label>

    <div class="form-control-input">
      <div
        :class="{
          input: true,
          // Sizes
          'input--medium': true,
          // States
          'input--standard': true,
          'input--empty': !model,
          'input--no-spin-buttons': true,
        }"
      >
        <input
          ref="inputElement"
          :disabled="false"
          :name="name"
          :readonly="true"
          :required="required"
          class="input__control"
          type="file"
          @change="change($event)"
        />
      </div>
    </div>
  </div>
  <br />
</template>

<style scoped>
.input--medium {
  border-radius: var(--sl-input-border-radius-medium);
  font-size: var(--sl-input-font-size-medium);
  height: var(--sl-input-height-medium);
}

.input--standard {
  background-color: var(--sl-input-background-color);
  border: solid var(--sl-input-border-width) var(--sl-input-border-color);
}

.form-control--has-label .form-control__label::after {
  content: " " var(--sl-input-required-content);
  margin-inline-start: var(--sl-input-required-content-offset);
  color: var(--sl-input-required-content-color);
}
</style>
