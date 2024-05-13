<script setup lang="ts">
import { BubbleMenu, Editor } from "@tiptap/vue-3";
import getMenuActions, {
  BubbleMenuAction,
} from "../components/extensions/bubble-menu";
import { computed, ComputedRef } from "vue";
import { CIcon, Size } from "@cristal/icons";

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
  border-radius: var(--cr-tooltip-border-radius);
  background: white; /* TODO: define a global variable for background color */
  overflow: hidden;
  box-shadow:
    0 0 0 1px rgba(0, 0, 0, 0.1),
    0 10px 20px rgba(0, 0, 0, 0.1);
}

.item {
  text-align: left;
  background: transparent;
  border: none;
  padding: 0.5rem 0.2rem;
}
</style>
