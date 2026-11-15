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
import { onMounted, useTemplateRef, watch } from "vue";
import type { DialogProps } from "@xwiki/platform-dsapi";

// The native modal dialog doesn't close when its backdrop is clicked, so it is always persistent.
const { title, width } = defineProps<DialogProps>();

defineSlots<{ default(): void; activator(): void; footer(): void }>();

const open = defineModel<boolean>({ default: false });

const dialog = useTemplateRef("dialog");

function sync(isOpen: boolean) {
  if (isOpen && !dialog.value?.open) {
    dialog.value?.showModal();
  } else if (!isOpen && dialog.value?.open) {
    dialog.value.close();
  }
}

onMounted(() => sync(open.value));
watch(open, sync);
</script>

<template>
  <span @click="open = true">
    <slot name="activator" />
  </span>
  <dialog
    ref="dialog"
    :aria-label="title"
    :style="{ width }"
    @close="open = false"
  >
    <h2>{{ title }}</h2>
    <slot />
    <div v-if="$slots.footer">
      <slot name="footer" />
    </div>
  </dialog>
</template>
