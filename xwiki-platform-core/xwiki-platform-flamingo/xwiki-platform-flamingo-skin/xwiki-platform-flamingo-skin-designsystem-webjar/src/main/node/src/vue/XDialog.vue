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
import { loadById } from "@xwiki/platform-xwiki-utils";
import {
  onBeforeUnmount,
  onMounted,
  ref,
  useTemplateRef,
  watchEffect,
} from "vue";
import type { DialogProps } from "@xwiki/platform-dsapi";
import type { Ref } from "vue";

// Without an explicit width, the dialog keeps the default width of the Bootstrap modals.
const { width, title, persistent } = defineProps<DialogProps>();

// requiring bootstrap is needed to be able to access the modal method once the component is mounted.
const jQuery: Promise<JQueryStatic> = loadById<[JQueryStatic, unknown]>(
  "jquery",
  "bootstrap",
).then(([$]) => $);

defineSlots<{ default(): void; activator(): void; footer(): void }>();
const root = useTemplateRef("root");
const open = defineModel<boolean>({ default: false });

const modal: Ref = ref(undefined);
// jQuery and Bootstrap are loaded asynchronously, so the component can be unmounted before they are (e.g. when the
// dialog is closed right after being opened). The modal must not be initialized then: it would be shown with nothing
// left to hide it.
let unmounted = false;
onMounted(async () => {
  const $ = await jQuery;
  if (unmounted) {
    return;
  }
  const modalElement = $(root.value!);
  // A static backdrop doesn't close the modal when clicked.
  // @ts-expect-error - bootstrap modal not typed on JQuery
  modal.value = modalElement.modal({
    show: open.value,
    backdrop: persistent ? "static" : true,
  });
  // Bootstrap focuses the modal itself once it is shown, i.e. once its opening animation is done. Keep track of the
  // field focused in its content before that, so that the focus can be moved back to it.
  let focusedField: HTMLElement | null = null;
  root.value!.addEventListener("focusin", (event) => {
    if (event.target !== root.value) {
      focusedField = event.target as HTMLElement;
    }
  });
  modalElement.on("show.bs.modal", () => {
    focusedField = null;
    open.value = true;
  });
  modalElement.on("shown.bs.modal", () => {
    // Move the focus back to the field the user was already in, if any, otherwise to the element of the content that
    // asks for it, as the native dialog element does.
    (focusedField?.isConnected
      ? focusedField
      : root.value?.querySelector<HTMLElement>("[autofocus]")
    )?.focus();
  });
  modalElement.on("hidden.bs.modal", () => {
    open.value = false;
  });
});

onBeforeUnmount(() => {
  unmounted = true;
  // The modal backdrop and the modal-open class of the body are managed by Bootstrap, outside of this component, so
  // they would be left behind if the component was unmounted while the modal is shown.
  modal.value?.modal("hide");
});

watchEffect(() => {
  if (modal.value && open.value) {
    modal.value.modal("show");
  } else if (modal.value && !open.value) {
    modal.value.modal("hide");
  }
});

function click() {
  open.value = true;
}
</script>

<template>
  <span @click="click">
    <slot name="activator" />
  </span>
  <teleport to="body">
    <div class="modal fade" tabindex="-1" role="dialog" ref="root">
      <div class="modal-dialog" role="document" :style="{ width }">
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
  </teleport>
</template>
