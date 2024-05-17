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
import { computed, ComputedRef, onMounted, onUnmounted, ref } from "vue";

import tippy, { GetReferenceClientRect, Instance, Props } from "tippy.js";
import { ActionDescriptor } from "../components/extensions/slash";
import { CIcon, Size } from "@cristal/icons";

import slashStore from "../stores/slash-store";
import { storeToRefs } from "pinia";
import { SuggestionProps } from "@tiptap/suggestion";
import { listNavigation } from "./list-navigation-helper";

const container = ref();

const { items, props } = storeToRefs(slashStore());

const piniaProps = computed(() => props.value as SuggestionProps<unknown>);

const actions: ComputedRef<ActionDescriptor[]> = computed(() => {
  return items.value.flatMap((category) => category.actions);
});

let popup: Instance<Props>[];

onMounted(() => {
  popup = tippy("body", {
    getReferenceClientRect: piniaProps.value
      .clientRect as GetReferenceClientRect,
    appendTo: () => document.body,
    content: container.value,
    showOnCreate: true,
    interactive: true,
    trigger: "manual",
    placement: "bottom-start",
  });
});

onUnmounted(() => {
  popup[0].destroy();
});

function apply(index: number) {
  const item = actions.value[index];
  if (item) {
    piniaProps.value.command(item);
  }
}

const { down, up, enter, index } = listNavigation(
  apply,
  computed(() => {
    return actions.value.length;
  }),
  container,
);
</script>

<template>
  <!--
  Defines a root element that is not part of the tippy component.
  It's is useful as a receiver for keyboard events forwarded for the editor.
  -->
  <div @keydown.down="down" @keydown.up="up" @keydown.enter="enter">
    <!--
    This container elements is moved inside tippy and is used as the content of
    the tippy popover.
    -->
    <div ref="container" class="items">
      <template v-for="category in items" :key="category.title">
        <span class="category-title">{{ category.title }}</span>
        <button
          v-for="(item, itemIndex) in category.actions"
          :key="item.title"
          :class="[
            'item',
            item.title == actions[index].title ? 'is-selected' : '',
          ]"
          @click="apply(itemIndex)"
        >
          <c-icon :name="item.icon" :size="Size.Small"></c-icon>&nbsp;
          {{ item.hint }}
        </button>
      </template>
    </div>
  </div>
</template>

<style scoped>
.category-title {
  font-style: italic;
}

.items {
  position: relative;
  border-radius: var(--cr-tooltip-border-radius);
  background: white; /* TODO: define a global variable for background color */
  overflow: hidden auto;
  box-shadow:
    0 0 0 1px rgba(0, 0, 0, 0.1),
    0 10px 20px rgba(0, 0, 0, 0.1);
  max-height: 300px;
}

.item {
  display: block;
  width: 100%;
  text-align: left;
  background: transparent;
  border: none;
  padding: 0.2rem 0.5rem;
}

.item.is-selected,
.item:hover {
  color: var(--cr-color-neutral-500);
  background: var(--cr-color-neutral-100);
}
</style>
