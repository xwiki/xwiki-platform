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
<script lang="ts" setup>
import "@shoelace-style/shoelace/dist/components/dialog/dialog";
import { useTemplateRef } from "vue";
import type SlDialog from "@shoelace-style/shoelace/dist/components/dialog/dialog.d.ts";

defineProps<{
  title: string;
  width: string | number | undefined;
}>();

const dialog = useTemplateRef<SlDialog>("dialog");

function click() {
  open.value = true;
}

const open = defineModel<boolean>();
</script>
<template>
  <span @click="click">
    <slot name="activator" />
  </span>
  <!-- We need to place the modal in the root node to avoid overlay issues. -->
  <Teleport to="#view">
    <sl-dialog
      ref="dialog"
      :open="open"
      :label="title"
      class="dialog-overview"
      @sl-show="open = true"
      @sl-hide="open = false"
    >
      <slot name="default" />
      <!-- We use Vue3's `template` tag syntax to manage slots, but Shoelace
           requires the (now deprecated) slot attribute. As such, we define a
           conditional wrapper that will be bound to the sl-dialog component,
           and will hold the contents of our own footer slot (if any). -->
      <!-- @vue-expect-error the slot attribute is shoelace specific and is not know by the typechecker.
      Disabling it for now as I did not find an elegant solution to declare this property. -->
      <!--eslint-disable-next-line vue/no-deprecated-slot-attribute -->
      <div v-if="$slots.footer" slot="footer" class="footer">
        <slot name="footer" />
      </div>
    </sl-dialog>
  </Teleport>
</template>

<style scoped>
sl-dialog {
  --width: v-bind(width);
  --body-spacing: 0 1.25rem 1.25rem;
}
.footer {
  display: flex;
  justify-content: flex-end;
  gap: var(--cr-spacing-x-small);
}
</style>
