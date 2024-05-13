<script setup lang="ts">
import {
  computed,
  type ComputedRef,
  inject,
  onUpdated,
  type Ref,
  ref,
  watch,
} from "vue";
import { CristalApp, PageData } from "@cristal/api";
import { useRoute } from "vue-router";
import { Editor, EditorContent } from "@tiptap/vue-3";
import StarterKit from "@tiptap/starter-kit";
import Placeholder from "@tiptap/extension-placeholder";
import { Slash } from "../components/extensions/slash";
import { Markdown } from "tiptap-markdown";
import Image from "@tiptap/extension-image";
import Table from "@tiptap/extension-table";
import TableRow from "@tiptap/extension-table-row";
import TableHeader from "@tiptap/extension-table-header";
import TableCell from "@tiptap/extension-table-cell";
import CTiptapBubbleMenu from "./c-tiptap-bubble-menu.vue";
import Blockquote from "@tiptap/extension-blockquote";
import Code from "@tiptap/extension-code";
import CodeBlock from "@tiptap/extension-code-block";

const route = useRoute();
const cristal: CristalApp = inject<CristalApp>("cristal")!;
const loading = ref(false);
const currentPage: Ref<PageData | undefined> = ref(undefined);
const error: Ref<Error | undefined> = ref(undefined);

// TODO: load this content first, then initialize the editor.
// Make the loading status first.
const content = ref("");

const currentPageName: ComputedRef<string> = computed(() => {
  // TODO: define a proper abstraction.
  return (
    (route.params.page as string) || cristal.getCurrentPage() || "Main.WebHome"
  );
});

async function fetchPage() {
  loading.value = true;
  try {
    currentPage.value = await cristal.getPage(currentPageName.value);
  } catch (e) {
    console.error(e);
    error.value = e;
  } finally {
    loading.value = false;
  }
}

watch(() => route.params.page, fetchPage, { immediate: true });

const viewRouterParams = {
  name: "view",
  params: { page: currentPageName.value },
};
const submit = async () => {
  // TODO: html does not make any sense here.
  await cristal
    ?.getWikiConfig()
    .storage.save(
      currentPageName.value,
      editor.value?.storage.markdown.getMarkdown(),
      "html",
    );
  cristal?.getRouter().push(viewRouterParams);
};

let editor: Ref<Editor | undefined> = ref(undefined);

async function loadEditor(page: PageData) {
  // Push the content to the document.
  // TODO: move to a components based implementation
  if (!editor.value) {
    content.value = page.syntax == "markdown/1.2" ? page.source : page.html;
    editor.value = new Editor({
      content: content.value,
      extensions: [
        StarterKit,
        Placeholder.configure({
          placeholder: "Type '/' to show the available actions",
        }),
        Image,
        Table,
        TableRow,
        TableHeader,
        TableCell,
        Slash,
        Markdown.configure({
          html: true,
        }),
        Blockquote,
        Code,
        CodeBlock,
      ],
    });
  }
}

onUpdated(() => loadEditor(currentPage.value!));
</script>

<template>
  <div v-if="loading" class="content-loading">
    <span class="load-spinner"></span>
    <h3>Loading</h3>
  </div>
  <div v-else-if="error" class="editor-error">
    <!-- TODO: provide a better error reporting. -->
    {{ error }}
  </div>
  <div class="inner-content">
    <div v-show="!loading && !error" class="content">
      <div class="content-scroll">
        <div class="whole-content">
          <c-tiptap-bubble-menu
            v-if="editor"
            :editor="editor"
          ></c-tiptap-bubble-menu>
          <editor-content :editor="editor" class="document-content editor" />
        </div>
      </div>
      <form class="pagemenu" @submit="submit">
        <x-btn size="small" variant="primary" @click="submit">Save</x-btn>
        <router-link :to="viewRouterParams">
          <x-btn size="small">Cancel</x-btn>
        </router-link>
      </form>
    </div>
  </div>
</template>

<style scoped>
.content-loading {
  display: flex;
  flex-flow: column;
  height: 100vh;
  align-items: center;
  justify-content: center;
}

.content-loading svg {
  width: 64px;
  height: 64px;
}

.content-loading h3 {
  padding: 0;
  margin: 0;
  color: var(--cr-color-neutral-500);
}

.pagemenu {
  display: flex;
  flex-flow: row;
  gap: var(--cr-spacing-x-small);
  padding: var(--cr-spacing-x-small) var(--cr-spacing-x-small);
  background: var(--cr-color-neutral-100);
  width: var(--cr-spacing-max-page);
  margin: var(--cr-spacing-x-small) auto;
  border-radius: var(--cr-input-border-radius-medium);
  max-width: var(--cr-sizes-max-page-width);
  width: 100%;
}

:deep(.ProseMirror-menubar) {
  border-radius: var(--cr-input-border-radius-medium);
  border-bottom: none;
  padding: var(--cr-spacing-x-small) var(--cr-spacing-x-small);
  background: var(--cr-color-neutral-100);
}

/*
TODO: should be moved to a css specific to the empty line placeholder plugin.
 */
.editor :deep(.placeholder):before {
  display: block;
  pointer-events: none;
  height: 0;
  content: attr(data-empty-text);
}

:deep(.is-empty:before) {
  pointer-events: none;
  float: left;
  height: 0;
  width: 100%;
  color: var(--cr-color-neutral-500);
  content: attr(data-placeholder);
}
</style>
