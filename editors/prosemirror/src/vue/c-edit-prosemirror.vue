<script setup lang="ts">
import {
  computed,
  type ComputedRef,
  inject,
  onBeforeUnmount,
  onBeforeUpdate,
  onUpdated,
  type Ref,
  ref,
  watch,
} from "vue";
import {
  defaultMarkdownParser,
  defaultMarkdownSerializer,
  schema as markdownSchema,
} from "prosemirror-markdown";
import { EditorState, EditorStateConfig, Plugin } from "prosemirror-state";
import { EditorView } from "prosemirror-view";
import { CristalApp, PageData } from "@cristal/api";
import { baseKeymap } from "prosemirror-commands";
import { emptyLinePlaceholder } from "../plugins/empty-line-placeholder";
import { keymap } from "prosemirror-keymap";
import { exampleSetup } from "prosemirror-example-setup";
import "prosemirror-example-setup/style/style.css";
import "prosemirror-view/style/prosemirror.css";
import "prosemirror-menu/style/menu.css";
import { DOMParser, Schema } from "prosemirror-model";
import { schema as basicSchema } from "prosemirror-schema-basic";
import { useRoute } from "vue-router";

const editor = ref(null);
const cristal: CristalApp = inject<CristalApp>("cristal")!;
const loading = ref(false);
const currentPage: Ref<PageData | undefined> = ref(undefined);
const error: Ref<Error | undefined> = ref(undefined);

/*
 TODO: add more plugins to make the experience more user-friendly:
 - undo / redo
 - basic styling operations
 - contextual tooltip
 - quick actions
 - ...
*/
const plugins: Plugin[] = [emptyLinePlaceholder, keymap(baseKeymap)];

let view: EditorView;

const currentPageName: ComputedRef<string> = computed(() => {
  // TODO: define a proper abstraction.
  return cristal?.getCurrentPage() || "XWiki.Main";
});

const route = useRoute();

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

async function loadEditor(page: PageData) {
  const config: EditorStateConfig = {
    plugins,
  };

  // Push the content to the document.
  // TODO: move to a components based implementation
  let schema: Schema = markdownSchema;
  if (page.syntax == "markdown/1.2") {
    config.doc = defaultMarkdownParser.parse(page.source!)!;
  } else {
    const tmpparse = document.createElement("div");
    tmpparse.innerHTML = page.html;
    schema = basicSchema;
    config.doc = DOMParser.fromSchema(schema).parse(tmpparse);
  }

  view = new EditorView(editor.value, {
    state: EditorState.create({
      ...config,
      plugins: exampleSetup({ schema }),
    }),
  });
}

/**
 * Make sure to destroy the editor before creating a new one,
 * or when this component is unmounted.
 */
function destroyEditor() {
  if (view) {
    view.destroy();
  }
}

onBeforeUpdate(destroyEditor);
onUpdated(() => loadEditor(currentPage.value!));
onBeforeUnmount(destroyEditor);

const viewRouterParams = {
  name: "view",
  params: { page: currentPageName.value },
};
const submit = async () => {
  const markdown = defaultMarkdownSerializer.serialize(view.state.doc);
  // TODO: html does not make any sense here.
  await cristal
    ?.getWikiConfig()
    .storage.save(currentPageName.value, markdown, "html");
  cristal?.getRouter().push(viewRouterParams);
};
</script>

<template>
  <div v-if="loading">
    <!-- TODO: provide a proposer loading UI. -->
    LOADING
  </div>
  <div v-else-if="error">
    <!-- TODO: provide a better error reporting. -->
    {{ error }}
  </div>
  <div v-show="!loading && !error">
    <div ref="editor" class="editor"></div>
    <form class="pagemenu" @submit="submit">
      <router-link :to="viewRouterParams">
        <x-btn>Cancel</x-btn>
      </router-link>
      <x-btn @click="submit">Save</x-btn>
    </form>
  </div>
</template>

<style scoped>
.editor {
  max-width: 800px;
  margin: auto;
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
</style>
