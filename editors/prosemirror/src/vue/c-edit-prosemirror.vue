<script setup lang="ts">
import { inject, nextTick, onMounted, ref } from "vue";
import { schema } from "prosemirror-schema-basic";
import { EditorState, EditorStateConfig, Plugin } from "prosemirror-state";
import { EditorView } from "prosemirror-view";
import { CristalApp } from "@cristal/api";
import { DOMParser } from "prosemirror-model";
import { baseKeymap } from "prosemirror-commands";
import { emptyLinePlaceholder } from "../plugins/empty-line-placeholder";
import { keymap } from "prosemirror-keymap";

const editor = ref(null);
const content = ref(null);
const cristal = inject<CristalApp>("cristal");

/*
 TODO: add more plugins to make the experience more user-friendly:
 - undo / redo
 - basic styling operations
 - contextual tooltip
 - quick actions
 - ...
*/
const plugins: Plugin[] = [emptyLinePlaceholder, keymap(baseKeymap)];

const htmlContent = ref("");

onMounted(async () => {
  const config: EditorStateConfig = {
    schema,
    plugins,
  };

  // Make sure the page is loaded
  await cristal?.loadPage();
  if (cristal) {
    // Update the html and wait for the component to be re-rendered.
    htmlContent.value = cristal?.getCurrentContent();
    await nextTick();
  }

  if (content.value) {
    // Push the content to the document.
    config.doc = DOMParser.fromSchema(schema).parse(content.value);
  }

  new EditorView(editor.value, { state: EditorState.create(config) });
});
</script>

<template>
  <div ref="editor" class="editor"></div>
  <!-- eslint-disable vue/no-v-html -->
  <div v-show="false" ref="content" v-html="htmlContent"></div>
</template>

<style scoped>
.editor {
  height: 100%;
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
