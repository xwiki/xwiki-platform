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
import { ref } from "vue";
import type { FileInputModel } from "@xwiki/cristal-dsapi";
import type { Ref } from "vue";

defineProps<{ isUploading: boolean }>();
const fileInputValue: Ref<FileInputModel> = ref();
const emits = defineEmits<{
  (e: "files-selected", files: File[]): void;
}>();

defineExpose({
  reset() {
    fileInputValue.value = undefined;
  },
});

function submit() {
  if (fileInputValue.value) {
    let files: File[] = [];
    if (Array.isArray(fileInputValue.value)) {
      files = [...fileInputValue.value];
    } else if (fileInputValue.value) {
      files = [fileInputValue.value];
    }
    emits("files-selected", files);
  }
}
</script>

<template>
  <x-form @form-submit="submit">
    <x-file-input
      v-model="fileInputValue"
      label="Attachment"
      name="attachment"
      required
      :disabled="isUploading"
    ></x-file-input>
    <x-btn type="submit" :disabled="!fileInputValue">Upload</x-btn>
  </x-form>
</template>

<style scoped></style>
