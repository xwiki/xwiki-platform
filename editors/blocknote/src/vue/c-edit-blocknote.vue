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
import messages from "../translations";
import { AlertsService } from "@xwiki/cristal-alerts-api";
import { CristalApp, PageData } from "@xwiki/cristal-api";
import {
  DocumentService,
  name as documentServiceName,
} from "@xwiki/cristal-document-api";
import {
  BlocknoteEditor as CBlockNoteView,
  BlocknoteEditorRealtimeUsers,
  BlocknoteRealtimeStatus,
} from "@xwiki/cristal-editors-blocknote-headless";
import { ModelReferenceHandlerProvider } from "@xwiki/cristal-model-reference-api";
import { CArticle } from "@xwiki/cristal-skin";
import {
  MarkdownToUniAstConverter,
  UniAst,
  createConverterContext,
} from "@xwiki/cristal-uniast";
import { debounce } from "lodash-es";
import { inject, ref, shallowRef, useTemplateRef, watch } from "vue";
import { useI18n } from "vue-i18n";
import type { StorageProvider } from "@xwiki/cristal-backend-api";
import type { EditorType } from "@xwiki/cristal-editors-blocknote-headless";

const { t } = useI18n({
  messages,
});

const cristal = inject<CristalApp>("cristal")!;
const container = cristal.getContainer();
const skinManager = cristal.getSkinManager();
const documentService = container.get<DocumentService>(documentServiceName);
const loading = documentService.isLoading();
const error = documentService.getError();
const unknownSyntax = ref();
const currentPage = documentService.getCurrentDocument();
const currentPageName = documentService.getCurrentDocumentReferenceString();
const currentPageReference = documentService.getCurrentDocumentReference();
const modelReferenceHandler = container
  .get<ModelReferenceHandlerProvider>("ModelReferenceHandlerProvider")
  .get();
const alertsService = container.get<AlertsService>("AlertsService")!;
const storage = container.get<StorageProvider>("StorageProvider").get();

const { realtimeURL: realtimeServerURL } = cristal.getWikiConfig();

const title = ref("");
const titlePlaceholder = modelReferenceHandler?.getTitle(
  currentPageReference.value!,
);

const editor = shallowRef<EditorType | null>(null);

const editorProps = shallowRef<
  InstanceType<typeof CBlockNoteView>["$props"]["editorProps"] | null
>(null);

const editorContent = shallowRef<UniAst | null>(null);

const editorInstance =
  useTemplateRef<InstanceType<typeof CBlockNoteView>>("editorInstance");

async function loadEditor(currentPage: PageData | undefined): Promise<void> {
  if (!currentPage) {
    // TODO
    return;
  }

  if (currentPage.syntax !== "markdown/1.2") {
    // TODO add a translation
    unknownSyntax.value = `Syntax [${currentPage.syntax}] is not editable with this editor.`;
    return;
  }

  editorProps.value = {
    editorRef: editor,
    theme: "light",
    formattingToolbarOnlyFor: [],
  };

  const markdownConverter = new MarkdownToUniAstConverter(
    createConverterContext(container),
  );

  editorContent.value = markdownConverter.parseMarkdown(currentPage.source);

  title.value = documentService.getTitle().value ?? "";
}

function view() {
  // Destroy the editor instance.
  // editor.value?.destroy();
  // Navigate to view mode.
  const viewRouterParams = {
    name: "view",
    params: { page: currentPageName.value ?? "" },
  };

  cristal?.getRouter().push(viewRouterParams);
}

async function submit() {
  // Perform a last save before quitting.
  await save(await editorInstance.value!.getContent());
  view();
}

watch(
  loading,
  (loading) => {
    if (!loading) {
      loadEditor(currentPage.value);
    }
  },
  { immediate: true },
);

async function save(content: string) {
  try {
    // TODO: html does not make any sense here.
    await storage.save(
      currentPageName.value ?? "",
      content,
      title.value,
      "html",
    );
  } catch (e) {
    // lastSaveSucceeded = false;
    console.error(e);
    alertsService.error(t("blocknote.editor.save.error"));
  }

  // If this save operation just created the document, the current document
  // will be undefined. So we update it.
  if (!currentPage.value) {
    documentService.setCurrentDocument(currentPageName.value ?? "");
  }
  documentService.notifyDocumentChange("update", currentPageReference.value!);
}

watch(
  title,
  debounce(async () => {
    save(await editorInstance.value!.getContent());
  }, 500),
);
</script>

<template>
  <c-article
    :loading
    :error
    :current-page
    :current-page-reference
    page-exist
    before-u-i-x-p-id="edit.before"
    after-u-i-x-p-id="edit.after"
  >
    <template #title>
      <input
        v-model="title"
        type="text"
        :placeholder="titlePlaceholder"
        class="doc-title"
      />
    </template>
    <template #default>
      <div class="doc-content">
        <span v-if="unknownSyntax">{{ unknownSyntax }}</span>
        <span v-else-if="!editorProps || !editorContent">Loading...</span>
        <template v-else>
          <div class="editor-centerer">
            <div class="editor">
              <CBlockNoteView
                ref="editorInstance"
                :editor-props
                :editor-content
                :container
                :skin-manager
                :realtime-server-u-r-l
                @blocknote-save="save"
              />
            </div>
          </div>

          <form class="pagemenu" @submit="submit">
            <div class="pagemenu-status">
              <BlocknoteEditorRealtimeUsers />
              <BlocknoteRealtimeStatus />
            </div>

            <div class="pagemenu-actions">
              <x-btn size="small" variant="primary" @click="submit">
                Close
              </x-btn>
            </div>
          </form>
        </template>
      </div>
    </template>
  </c-article>
</template>

<style scoped>
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

.editor-centerer {
  display: flex;
  flex-direction: row;
  justify-content: center;
}

.editor {
  outline: none;
  max-width: var(--cr-sizes-max-page-width);
  width: 100%;
}
</style>
