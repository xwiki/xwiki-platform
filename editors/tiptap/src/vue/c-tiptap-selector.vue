<script setup lang="ts">
import {
  computed,
  ComputedRef,
  nextTick,
  onMounted,
  onUnmounted,
  ref,
  watch,
} from "vue";

import tippy, { GetReferenceClientRect, Instance, Props } from "tippy.js";
import { SuggestionProps } from "@tiptap/suggestion";
import {
  ActionCategoryDescriptor,
  ActionDescriptor,
} from "../components/extensions/slash";
import { CIcon, Size } from "@cristal/icons";

const container = ref();

const props = defineProps<{
  props: SuggestionProps<unknown>;
}>();

const items: ComputedRef<ActionCategoryDescriptor[]> = computed(
  () => props.props.items as ActionCategoryDescriptor[],
);

const actions: ComputedRef<ActionDescriptor[]> = computed(() => {
  return items.value.flatMap((category) => category.actions);
});

let popup: Instance<Props>[];

onMounted(() => {
  popup = tippy("body", {
    getReferenceClientRect: props.props.clientRect as GetReferenceClientRect,
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

const index = ref(0);

function down() {
  index.value = (index.value + 1) % actions.value.length;
}

function up() {
  const actionsLength = actions.value.length;
  index.value = (index.value + actionsLength - 1) % actionsLength;
}

function enter() {
  apply(index.value);
}

function apply(index: number) {
  const item = actions.value[index];
  if (item) {
    props.props.command(item);
  }
}

// Make sure the newly selected item is visible on element focus change.
watch(index, async () => {
  // Wait for the container to be re-pained to run the selector on the newly
  // selected element.
  await nextTick();
  container.value.querySelector(".is-selected").scrollIntoView();
});
</script>

<template>
  <!--
  Defines a root elemement that is not part of the tippy component.
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
