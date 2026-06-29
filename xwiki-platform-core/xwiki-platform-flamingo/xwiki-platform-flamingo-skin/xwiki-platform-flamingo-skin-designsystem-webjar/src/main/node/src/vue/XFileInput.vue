<!--
  See the NOTICE file distributed with this work for additional
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
import { useId, useTemplateRef, watch } from "vue";
import type { FileInputModel, FileInputProps } from "@xwiki/platform-dsapi";

const model = defineModel<FileInputModel>();
const { label } = defineProps<FileInputProps>();

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

const inputElement = useTemplateRef("inputElement");
const fileInputId = useId();

watch(model, (model) => {
  if (!model) {
    inputElement.value!.value = "";
  }
});

defineOptions({
  // See https://vuejs.org/api/options-misc.html#inheritattrs
  // Unknown attrs are by default added to the root element (i.e., the dl), but we want to disable that and instead add
  // them to the input field. That way it's possible for instance to add a name to the input field without having to
  // explicitly declare it on the props.
  inheritAttrs: false,
});
</script>

<template>
  <dl>
    <dt>
      <label :for="fileInputId">{{ label }}</label>
    </dt>
    <dd>
      <input
        ref="inputElement"
        :id="fileInputId"
        type="file"
        v-bind="$attrs"
        @change="change($event)"
      />
    </dd>
  </dl>
</template>

<style scoped></style>
