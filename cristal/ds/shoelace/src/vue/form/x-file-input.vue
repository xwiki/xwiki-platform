<!--
  See the LICENSE file distributed with this work for additional
  information regarding copyright ownership.

  This is free software; you can redistribute it and/or modify it
  under the terms of the GNU Lesser General Public License as
  published by the Free Software Foundation; either version 2.1 of
  the License, or (at your option) any later version.

  This software is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
  Lesser General Public License for more details.

  You should have received a copy of the GNU Lesser General Public
  License along with this software; if not, write to the Free
  Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
  02110-1301 USA, or see the FSF site: http://www.fsf.org.
-->
<script setup lang="ts">
import { ref, useId, watch } from "vue";
import type { FileInputModel, TextFieldProps } from "@xwiki/cristal-dsapi";
import "@shoelace-style/shoelace/dist/components/input/input";

const model = defineModel<FileInputModel>();
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

// Generates a unique id shared between the file input and its label.
const inputId = useId();
</script>

<!--
Partial clone of sl-input with a input type=file because the type file
is not supported by shoelace currently.
-->
<template>
  <div class="form-control form-control--medium form-control--has-label">
    <label class="form-control__label" :for="inputId" :aria-hidden="false">
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
          :id="inputId"
          :readonly="true"
          :required="required"
          class="input__control"
          type="file"
          @change="change($event)"
        />
      </div>
    </div>
  </div>
</template>

<style scoped>
.input {
  display: flex;
  align-items: center;
  padding: var(--cr-spacing-small);
}

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
