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
import CTiptapBubbleMenu from "./c-tiptap-bubble-menu.vue";
import CSaveStatus from "./c-save-status.vue";
import CConnectionStatus from "./c-connection-status.vue";
import { Slash } from "../components/extensions/slash";
import { loadLinkSuggest } from "../components/extensions/link-suggest";
import { CollaborationKit, User } from "../extensions/collaboration";
import Link from "../extensions/link";
import Markdown from "../extensions/markdown";
import { computed, type ComputedRef, inject, type Ref, ref, watch } from "vue";
import { CristalApp, PageData } from "@xwiki/cristal-api";
import { useRoute } from "vue-router";
import { Editor, EditorContent } from "@tiptap/vue-3";
import StarterKit from "@tiptap/starter-kit";
import Placeholder from "@tiptap/extension-placeholder";
import Image from "@tiptap/extension-image";
import Table from "@tiptap/extension-table";
import TableRow from "@tiptap/extension-table-row";
import TableHeader from "@tiptap/extension-table-header";
import TableCell from "@tiptap/extension-table-cell";
import {
  type LinkSuggestService,
  type LinkSuggestServiceProvider,
} from "@xwiki/cristal-link-suggest-api";
import {
  type DocumentService,
  name as documentServiceName,
} from "@xwiki/cristal-document-api";

const route = useRoute();
const cristal: CristalApp = inject<CristalApp>("cristal")!;

const documentService = cristal
  .getContainer()
  .get<DocumentService>(documentServiceName);
const loading = documentService.isLoading();
const error: Ref<Error | undefined> = documentService.getError();
const currentPage: Ref<PageData | undefined> =
  documentService.getCurrentDocument();

// TODO: load this content first, then initialize the editor.
// Make the loading status first.
const content = ref("");
const title = ref("");
const titlePlaceholder = ref("");

const currentPageName: ComputedRef<string> = computed(() => {
  // TODO: define a proper abstraction.
  return (
    (route.params.page as string) || cristal.getCurrentPage() || "Main.WebHome"
  );
});

const viewRouterParams = {
  name: "view",
  params: { page: currentPageName.value },
};
const view = () => {
  // Destroy the editor instance.
  editor.value?.destroy();
  // Navigate to view mode.
  cristal?.getRouter().push(viewRouterParams);
};
const save = async (authors: User[]) => {
  console.log(
    "Saving changes made by: ",
    authors.map((author) => author.name).join(", "),
  );
  // TODO: html does not make any sense here.
  await cristal
    ?.getWikiConfig()
    .storage.save(
      currentPageName.value,
      editor.value?.storage.markdown.getMarkdown(),
      title.value,
      "html",
    );
};
const submit = async () => {
  await editor.value?.storage.cristalCollaborationKit.autoSaver.save();
  view();
};

// TODO: Remove this when we add support for user authentication.
const names = [
  "Lea Thompson",
  "Cyndi Lauper",
  "Tom Cruise",
  "Madonna",
  "Jerry Hall",
  "Joan Collins",
  "Winona Ryder",
  "Christina Applegate",
  "Alyssa Milano",
  "Molly Ringwald",
  "Ally Sheedy",
  "Debbie Harry",
  "Olivia Newton-John",
  "Elton John",
  "Michael J. Fox",
  "Axl Rose",
  "Emilio Estevez",
  "Ralph Macchio",
  "Rob Lowe",
  "Jennifer Grey",
  "Mickey Rourke",
  "John Cusack",
  "Matthew Broderick",
  "Justine Bateman",
  "Lisa Bonet",
];
const randomName = () => names[Math.floor(Math.random() * names.length)];
const currentUser = {
  name: randomName(),
};

let editor: Ref<Editor | undefined> = ref(undefined);

async function loadEditor(page: PageData | undefined) {
  // Push the content to the document.
  // TODO: move to a components based implementation
  if (!editor.value) {
    content.value =
      page?.syntax == "markdown/1.2" ? page?.source : page?.html || "";
    title.value = page?.headlineRaw || "";
    titlePlaceholder.value = page?.name || "";

    const linkSuggestServiceProvider = cristal
      .getContainer()
      .get<LinkSuggestServiceProvider>("LinkSuggestServiceProvider");
    const linkSuggest: LinkSuggestService | undefined =
      linkSuggestServiceProvider.get();

    const realtimeURL = cristal.getWikiConfig().realtimeURL;

    editor.value = new Editor({
      content: content.value || "",
      extensions: [
        StarterKit.configure({
          // Disable the default history in order to use Collaboration's history management so that users undo / redo
          // only their own changes.
          history: false,
        }),
        Placeholder.configure({
          placeholder: "Type '/' to show the available actions",
        }),
        Image,
        Table,
        TableRow,
        TableHeader,
        TableCell,
        Slash,
        // TODO: I did it that way for simplicity but this should really be
        // moved to an actual inversify component.
        loadLinkSuggest(
          cristal.getSkinManager(),
          cristal.getContainer(),
          linkSuggest,
        ),
        Markdown.configure({
          html: true,
        }),
        Link.configure({
          openOnClick: "whenNotEditable",
        }),
        CollaborationKit.configure({
          channel: currentPageName.value,
          user: currentUser,
          saveCallback: save,
          baseUrl: realtimeURL,
        }),
      ],
    });
  }
}

watch(currentPage, (page) => loadEditor(page), { immediate: true });
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
  <div v-show="!loading && !error" class="edit-wrapper">
    <div class="doc-header">
      <div class="doc-header-inner">
        <input
          v-model="title"
          type="text"
          :placeholder="titlePlaceholder"
          class="doc-title"
        />
        <c-tiptap-bubble-menu
          v-if="editor"
          :editor="editor"
        ></c-tiptap-bubble-menu>
      </div>
    </div>
    <editor-content :editor="editor" class="doc-content editor" />
    <form class="pagemenu" @submit="submit">
      <div class="pagemenu-status">
        <c-connection-status
          v-if="editor"
          :provider="editor.storage.cristalCollaborationKit.provider"
        ></c-connection-status>
        <c-save-status
          v-if="editor"
          :auto-saver="editor.storage.cristalCollaborationKit.autoSaver"
        ></c-save-status>
      </div>
      <div class="pagemenu-actions">
        <x-btn size="small" variant="primary" @click="submit">Close</x-btn>
      </div>
    </form>
  </div>
</template>

<style scoped>
.content {
  display: grid;
  grid-template-rows: 56px auto auto 1fr;
  overflow: hidden;
  justify-content: center;
}
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
  position: sticky;
  bottom: 0;
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

.pagemenu-status {
  /* The content of this section may be a mix of inline and block level elements. */
  display: flex;
  /* Push the actions to the right end of the menu. */
  flex-grow: 1;
}

.pagemenu-status > * {
  /* Match the action button padding, which seems to be hard-coded.  */
  padding: 0 12px;
}

:deep(.ProseMirror-menubar) {
  border-radius: var(--cr-input-border-radius-medium);
  border-bottom: none;
  padding: var(--cr-spacing-x-small) var(--cr-spacing-x-small);
  background: var(--cr-color-neutral-100);
}

:deep(.ProseMirror) {
  outline: none;
  max-width: var(--cr-sizes-max-page-width);
  width: 100%;
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

.doc-title {
  max-width: var(--cr-sizes-max-page-width);
  width: 100%;
  display: flex;
  flex-flow: column;
  margin: 0;
  font-size: var(--cr-font-size-2x-large);
  line-height: var(--cr-font-size-2x-large);
  outline: none;
  border: none;
  align-self: center;
  justify-self: center;
}

/*
 * Collaboration styles.
 *
 * TODO: Should we move these styles to a custom TipTap (collaboration) extension?
 */

/* Show where remote users are typing. */
.editor :deep(.collaboration-cursor__caret) {
  border-left: 1px solid var(--cr-input-border-color);
  border-right: 1px solid var(--cr-input-border-color);
  margin-left: -1px;
  margin-right: -1px;
  pointer-events: none;
  position: relative;
  word-break: normal;
}

/* Render the remote user name above the associated caret. */
.editor :deep(.collaboration-cursor__label) {
  border-radius: var(--cr-border-radius-large) var(--cr-border-radius-large)
    var(--cr-border-radius-large) 0;
  color: var(--cr-input-color);
  /* The cursor label is injected in the edited content, read-only, so it inherits the content styles. We want the
    cursor label to have the same styles no matter where the caret is placed inside the edited content (e.g. in a level
    one heading versus a paragraph). */
  font-size: 12px;
  font-style: normal;
  /* Improves the contrast with the background color. */
  font-weight: var(--cr-font-weight-bold);
  left: -1px;
  /* We need to know the height in order to be able to place the cursor label just above the caret. */
  line-height: 1;
  padding: 4px 6px;
  position: absolute;
  top: -20px;
  user-select: none;
  white-space: nowrap;
}
</style>
