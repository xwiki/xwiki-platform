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
import { onMounted, ref, useTemplateRef, watchEffect } from "vue";
import type { DialogProps } from "@xwiki/platform-dsapi";
import type { Ref } from "vue";

const { width, title } = defineProps<DialogProps>();

// eslint-disable-next-line no-undef
const jQuery: Promise<JQuery> = new Promise((resolve) => {
  // eslint-disable-next-line @typescript-eslint/no-require-imports,no-undef
  require(["jquery"], ($: JQuery) => resolve($));
});

const root = useTemplateRef("root");
const open = defineModel<boolean>();

const modal: Ref = ref(undefined);
onMounted(async () => {
  const $ = await jQuery;
  const modalElement = $(root.value!);
  modal.value = modalElement.modal({ show: false });
  modalElement.on("show.bs.modal", () => {
    open.value = true;
  });
  modalElement.on("hidden.bs.modal", () => {
    open.value = false;
  });
});

watchEffect(() => {
  if (modal.value && open.value) {
    modal.value.modal("show");
  } else if (modal.value && !open.value) {
    modal.value.modal("hide");
  }
});
</script>

<template>
  <div class="modal fade" tabindex="-1" role="dialog" ref="root">
    <div class="modal-dialog" role="document">
      <div class="modal-content">
        <div class="modal-header">
          <button
            type="button"
            class="close"
            data-dismiss="modal"
            aria-label="Close"
          >
            <span aria-hidden="true">&times;</span>
          </button>
          <h4 class="modal-title">{{ title }}</h4>
        </div>
        <div class="modal-body">
          <slot></slot>
        </div>
        <div class="modal-footer" v-if="$slots.footer">
          <slot name="footer"></slot>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.modal-dialog {
  width: v-bind(width);
}
</style>
