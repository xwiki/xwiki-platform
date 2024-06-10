<script setup lang="ts">
import { CIcon, Size } from "@xwiki/cristal-icons";
import { BubbleMenuAction } from "../components/extensions/bubble-menu";
import { Editor, Range } from "@tiptap/vue-3";
import { inject, onMounted, onUpdated, Ref, ref } from "vue";
import { ContentTools } from "@xwiki/cristal-skin";
import type { CristalApp } from "@xwiki/cristal-api";
import type { XBtn } from "@xwiki/cristal-dsapi";

const emits = defineEmits(["close"]);

const props = withDefaults(
  defineProps<{
    // Define wheter the component is wrapped in a parent component.
    // When it's the case, a "back" button is displayed, sending a cancel event
    // in click
    hasWrapper?: boolean;
    // The action to perform on link creation/update validation
    action: BubbleMenuAction;
    // The current editor
    editor: Editor;
    // The current range selection
    range: Range;
    // The current url
    url?: string;
    // When true, indicated that the selection is ambiguous and does not allow
    // to know exactly which link must be editor.
    isAmbiguous?: boolean;
  }>(),
  {
    hasWrapper: true,
    url: undefined,
    isAmbiguous: false,
  },
);
// We don't propose the link when the selection is ambiguous.
const linkValue = ref(props.isAmbiguous ? "" : props.url);
function submitLink() {
  const { action, editor, range } = props;
  action.command({ editor, range }, { linkValue: linkValue.value });
}

function removeLink() {
  const { action, editor, range } = props;
  action.command({ editor, range }, { removeLink: true });
  close();
}

function close() {
  emits("close");
}

const formRoot: Ref<HTMLElement | undefined> = ref(undefined);

// Campture internal link navigation for the follow link button.
function listenToLinks() {
  const cristal: CristalApp = inject<CristalApp>("cristal")!;
  if (formRoot.value) {
    ContentTools.listenToClicks(formRoot.value, cristal);
  }
}
onMounted(listenToLinks);
onUpdated(listenToLinks);
</script>

<template>
  <div class="container">
    <form
      ref="formRoot"
      class="edit-link"
      title="Press enter to validate"
      @submit.prevent="submitLink"
    >
      <!-- TODO: integrate the link suggestion there too -->
      <!-- TODO: introduce a x-input component in the abstract DS. -->
      <input
        v-model="linkValue"
        type="text"
        placeholder="Link..."
        :disabled="isAmbiguous"
      />
      <!-- TODO: distinguish between following internal and external links? -->
      <x-btn
        color="primary"
        title="Follow link"
        variant="default"
        size="small"
        :disabled="!url || isAmbiguous"
      >
        <a v-if="url" :href="url">
          <c-icon name="box-arrow-up-right" :size="Size.Small"></c-icon>
        </a>
        <c-icon v-else name="box-arrow-up-right" :size="Size.Small"></c-icon>
      </x-btn>
      <x-btn
        title="Remove link"
        variant="default"
        size="small"
        @click="removeLink"
        @keydown.enter="removeLink"
      >
        <c-icon name="x-circle-fill" :size="Size.Small"></c-icon>
      </x-btn>
      <x-btn
        v-if="hasWrapper"
        title="Go back"
        variant="default"
        size="small"
        @click="close"
        @keydown.enter="close"
      >
        <c-icon name="arrow-up-left"></c-icon>
      </x-btn>
    </form>
  </div>
</template>

<style scoped>
.edit-link {
  display: flex;
  gap: 8px;
}
.container {
  padding: var(--cr-spacing-x-small) var(--cr-spacing-small);
}
input {
  width: 250px;
  border-radius: 4px;
}
:deep(.button) {
  padding: 0 var(--cr-spacing-x-small);
  min-width: unset;
  background-color: transparent !important;
  border: 0;
}
:deep(.cr-icon) {
  color: var(--cr-color-neutral-700);
}
</style>
