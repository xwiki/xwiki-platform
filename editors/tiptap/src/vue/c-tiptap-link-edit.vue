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
import { CIcon, Size } from "@xwiki/cristal-icons";
import { BubbleMenuAction } from "../components/extensions/bubble-menu";
import { Range } from "@tiptap/vue-3";
import { type Editor } from "@tiptap/core";
import { inject, onMounted, onUpdated, Ref, ref, toRefs, watch } from "vue";
import { ContentTools } from "@xwiki/cristal-skin";
import type { CristalApp } from "@xwiki/cristal-api";
import CTiptapLinkSuggest from "./c-tiptap-link-suggest.vue";
import { LinkSuggestService, name } from "@xwiki/cristal-link-suggest-api";
import linkSuggestStore, {
  LinkSuggestStore,
} from "../stores/link-suggest-store";
import {
  initSuggestionsService,
  LinkSuggestionActionDescriptor,
} from "../components/extensions/link-suggest";
import { debounce } from "lodash";

const emits = defineEmits(["close"]);

const props = withDefaults(
  defineProps<{
    // Define whether the component is wrapped in a parent component.
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

const { url } = toRefs(props);

watch(url, (value) => {
  linkValue.value = value;
});

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
const tippyContainer: Ref<HTMLElement | undefined> = ref(undefined);
const linkSuggestComp: Ref<typeof CTiptapLinkSuggest | undefined> =
  ref(undefined);

// Capture internal link navigation for the follow link button.
function listenToLinks() {
  const cristal: CristalApp = inject<CristalApp>("cristal")!;
  if (formRoot.value) {
    ContentTools.listenToClicks(formRoot.value, cristal);
  }
}

onMounted(listenToLinks);
onUpdated(listenToLinks);

const cristal: CristalApp = inject<CristalApp>("cristal")!;
let linkSuggest: LinkSuggestService | undefined = undefined;
try {
  linkSuggest = cristal.getContainer().get<LinkSuggestService>(name);
} catch (e) {
  console.debug(`[${name}] service not found`);
}

const store: LinkSuggestStore = linkSuggestStore();

store.updateProps({
  clientRect: () => {
    return tippyContainer.value?.getBoundingClientRect();
  },
  editor: props.editor,
});

const debouncedWatch = debounce(async (query: string) => {
  const links = await initSuggestionsService(
    linkSuggest,
    cristal.getWikiConfig(),
  )({ query });
  store.updateText(query);
  store.updateLinks(links);
}, 400);
watch(
  linkValue,
  async (query) => {
    await debouncedWatch(query || "");
  },
  { immediate: true },
);

function existingLinkAction(link: LinkSuggestionActionDescriptor) {
  linkValue.value = link.url;
}

// Bind the keyboard event of the input field to the actions of the link suggest
// component (which is providing dedicated operations).
const { up, down, enter } = {
  up() {
    linkSuggestComp.value?.upAction();
  },
  down() {
    linkSuggestComp.value?.downAction();
  },
  enter() {
    linkSuggestComp.value?.enterAction();
  },
};
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
        ref="tippyContainer"
        v-model="linkValue"
        type="text"
        placeholder="Link..."
        :disabled="isAmbiguous"
        @keydown.up.prevent="up"
        @keydown.down.prevent="down"
        @keydown.enter="enter"
      />
      <CTiptapLinkSuggest
        ref="linkSuggestComp"
        :has-suggest-service="linkSuggest != null"
        :existing-link-action="existingLinkAction"
      ></CTiptapLinkSuggest>
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
