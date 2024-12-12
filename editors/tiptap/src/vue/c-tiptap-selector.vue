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
import { listNavigation } from "./list-navigation-helper";
import { ActionDescriptor } from "../components/extensions/slash";
import slashStore from "../stores/slash-store";
import { SuggestionProps } from "@tiptap/suggestion";
import { CIcon, Size } from "@xwiki/cristal-icons";
import { storeToRefs } from "pinia";
import tippy, { GetReferenceClientRect, Instance, Props } from "tippy.js";
import { ComputedRef, computed, onMounted, onUnmounted, ref } from "vue";

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
  font-family: var(--cr-font-sans);
  font-size: var(--cr-font-size-2x-small);
  font-weight: var(--cr-font-weight-bold);
  color: var(--cr-color-neutral-500);
  text-transform: uppercase;
  padding: var(--cr-spacing-small);
}

.items {
  display: grid;
  border-radius: var(--cr-tooltip-border-radius);
  background: white; /* TODO: define a global variable for background color */
  overflow: hidden auto;
  border: 1px solid var(--cr-color-neutral-200);
  box-shadow: var(--cr-shadow-x-large);
  max-height: 300px;
}

.item {
  display: block;
  text-align: start;
  background: transparent;
  border: none;
  padding: var(--cr-spacing-small) var(--cr-spacing-small);

  &:hover,
  &.is-selected {
    background-color: var(--cr-color-neutral-100);
    cursor: pointer;
  }
}
</style>
