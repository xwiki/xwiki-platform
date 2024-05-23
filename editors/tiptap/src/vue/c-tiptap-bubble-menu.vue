<!--
 * See the LICENSE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 *
 * This file is part of the Cristal Wiki software prototype
 * @copyright  Copyright (c) 2023 XWiki SAS
 * @license    http://opensource.org/licenses/AGPL-3.0 AGPL-3.0
 *
 **/
-->
<script setup lang="ts">
import { BubbleMenu, Editor } from "@tiptap/vue-3";
import getMenuActions, {
  BubbleMenuAction,
} from "../components/extensions/bubble-menu";
import { computed, ComputedRef } from "vue";
import { CIcon, Size } from "@xwiki/cristal-icons";

const props = defineProps<{
  editor: Editor;
}>();

const actions: ComputedRef<BubbleMenuAction[]> = computed(() =>
  getMenuActions(props.editor),
);

function apply(action: BubbleMenuAction) {
  action.command({
    editor: props.editor,
    range: props.editor.state.selection,
  });
}

const hideOnEsc = {
  name: "hideOnEsc",
  defaultValue: true,
  fn({ hide }: { hide: () => void }) {
    function onKeyDown(event: KeyboardEvent) {
      if (event.keyCode === 27) {
        hide();
      }
    }

    return {
      onShow() {
        document.addEventListener("keydown", onKeyDown);
      },
      onHide() {
        document.removeEventListener("keydown", onKeyDown);
      },
    };
  },
};
</script>

<template>
  <bubble-menu
    :editor="editor"
    :tippy-options="{
      plugins: [hideOnEsc],
    }"
    class="items"
  >
    <button
      v-for="action in actions"
      :key="action.title"
      class="item"
      :aria-label="action.title"
      :title="action.title"
      @click="apply(action)"
      @submit="apply(action)"
    >
      <c-icon :name="action.icon" :size="Size.Small"></c-icon>
    </button>
  </bubble-menu>
</template>

<style scoped>
.items {
  position: relative;
  display: flex;
  border-radius: var(--cr-tooltip-border-radius);
  background: white; /* TODO: define a global variable for background color */
  overflow: hidden;
  box-shadow:
    0 0 0 1px rgba(0, 0, 0, 0.1),
    0 10px 20px rgba(0, 0, 0, 0.1);
}

.item {
  background: transparent;
  border: none;
  padding: var(--cr-spacing-x-small);
}

.item:hover {
  background-color: var(--cr-color-neutral-200);
  cursor: pointer;
}
</style>
