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

const route = useRoute();
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
  <div v-if="loading" class="content-loading">
    <!-- TODO: provide a proposer loading UI. -->
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
          <div class="center-content">
            <div ref="editor" class="document-content editor"></div>
          </div>
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
</style>
