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
import cRealtimeUsers from "./c-realtime-users.vue";
import cSaveStatus, { SaveStatus } from "./c-save-status.vue";
import messages from "../translations";
import {
  Status,
  collaborationManagerProviderName,
} from "@xwiki/cristal-collaboration-api";
import { name as documentServiceName } from "@xwiki/cristal-document-api";
import {
  BlocknoteEditor as CBlockNoteView,
  DEFAULT_MACROS,
} from "@xwiki/cristal-editors-blocknote-headless";
import { CArticle } from "@xwiki/cristal-skin";
import {
  markdownToUniAstConverterName,
  uniAstToMarkdownConverterName,
} from "@xwiki/cristal-uniast-markdown";
import { debounce } from "lodash-es";
import {
  inject,
  onMounted,
  onUnmounted,
  ref,
  shallowRef,
  useTemplateRef,
  watch,
} from "vue";
import { useI18n } from "vue-i18n";
import { onBeforeRouteLeave } from "vue-router";
import type { AlertsService } from "@xwiki/cristal-alerts-api";
import type { CristalApp, PageData } from "@xwiki/cristal-api";
import type { StorageProvider } from "@xwiki/cristal-backend-api";
import type {
  CollaborationInitializer,
  CollaborationManagerProvider,
  User,
} from "@xwiki/cristal-collaboration-api";
import type { DocumentService } from "@xwiki/cristal-document-api";
import type { ModelReferenceHandlerProvider } from "@xwiki/cristal-model-reference-api";
import type { UniAst } from "@xwiki/cristal-uniast-api";
import type {
  MarkdownToUniAstConverter,
  UniAstToMarkdownConverter,
} from "@xwiki/cristal-uniast-markdown";
import type { Ref } from "vue";

const { t } = useI18n({
  messages,
});

const cristal = inject<CristalApp>("cristal")!;
const container = cristal.getContainer();
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
let collaborationProvider: () => CollaborationInitializer;
let status: Ref<Status> | undefined;
let users: Ref<User[]> | undefined;
if (realtimeServerURL) {
  const collaborationManager = container
    .get<CollaborationManagerProvider>(collaborationManagerProviderName)
    .get();
  status = collaborationManager.status();
  users = collaborationManager.users();
  collaborationProvider = await collaborationManager.get();
}

const title = ref("");
const titlePlaceholder = modelReferenceHandler?.getTitle(
  currentPageReference.value!,
);

const editorProps = shallowRef<
  InstanceType<typeof CBlockNoteView>["$props"]["editorProps"] | null
>(null);

const editorContent = shallowRef<UniAst | Error | null>(null);

const editorInstance =
  useTemplateRef<InstanceType<typeof CBlockNoteView>>("editorInstance");

// Tools for UniAst handling
const markdownToUniAst = container.get<MarkdownToUniAstConverter>(
  markdownToUniAstConverterName,
);
const uniAstToMarkdown = container.get<UniAstToMarkdownConverter>(
  uniAstToMarkdownConverterName,
);

// Saving status
const saveStatus = ref<SaveStatus>(SaveStatus.SAVED);

/**
 * Setup the editor and title input using the fetched page's content
 *
 * @param currentPage - The fetched current page
 */
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
    theme: "light",
    // TODO: make this customizable
    // https://jira.xwiki.org/browse/CRISTAL-457
    lang: "en",
    macros: {
      buildable: Object.values(DEFAULT_MACROS),
      openMacroParamsEditor(/*macro, params, update*/) {
        alert("TODO: params editor for macros in Cristal");
      },
    },
  };

  editorContent.value = await markdownToUniAst.parseMarkdown(
    currentPage.source,
  );

  title.value = documentService.getTitle().value ?? "";
}

/**
 * Go to the view route
 */
function navigateToView() {
  // Navigate to view mode.
  const viewRouterParams = {
    name: "view",
    params: { page: currentPageName.value ?? "" },
  };

  cristal?.getRouter().push(viewRouterParams);
}

/**
 * Save a content into the current page document
 *
 * @param content - The content to save
 */
async function save(content: UniAst) {
  saveStatus.value = SaveStatus.SAVING;

  try {
    const markdown = await uniAstToMarkdown.toMarkdown(content);

    if (markdown instanceof Error) {
      throw error;
    }

    // TODO: html does not make any sense here.
    await storage.save(
      currentPageName.value ?? "",
      title.value,
      markdown,
      "html",
    );

    saveStatus.value = SaveStatus.SAVED;
  } catch (e) {
    // lastSaveSucceeded = false;
    console.error(e);
    alertsService.error(t("blocknote.editor.save.error"));

    saveStatus.value = SaveStatus.UNSAVED;
  }
}

/**
 * Save the editor's content into the page
 */
async function saveContent() {
  const editor = editorInstance.value!;
  const content = editor.getContent();

  if (content instanceof Error) {
    // TODO: error reporting
    return;
  }

  await save(content);
}

/**
 * Save the editor's content and navigate to the view page
 */
async function submit() {
  await saveContent();

  // TODO: hold back user in case of error
  navigateToView();
}

// Wait for the page to be fetched before loading the editor
watch(
  loading,
  (loading) => {
    if (!loading) {
      loadEditor(currentPage.value);
    }
  },
  { immediate: true },
);

/**
 * Save the edited title in realtime
 */
watch(
  title,
  debounce(async () => {
    if (editorInstance.value) {
      await saveContent();
    }
  }, 500),
);

function beforeUnload(evt: BeforeUnloadEvent): string | void {
  if (saveStatus.value !== SaveStatus.SAVED) {
    evt.preventDefault();

    // NOTE: the message won't actually be shown in most browsers nowadays, it will be replaced with a generic message instead.
    // This is not a bug.
    return t("blocknote.editor.save.unsavedChanges");
  }
}

onMounted(() => {
  window.addEventListener("beforeunload", beforeUnload);
});

onUnmounted(() => {
  window.removeEventListener("beforeunload", beforeUnload);
});

onBeforeRouteLeave(() => {
  if (saveStatus.value !== SaveStatus.SAVED) {
    // NOTE: the message won't actually be shown in most browsers nowadays, it will be replaced with a generic message instead.
    // This is not a bug.
    return confirm(t("blocknote.editor.save.unsavedChanges"));
  }
});
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
                :collaboration-provider
                @instant-change="saveStatus = SaveStatus.UNSAVED"
                @debounced-change="save"
              />
            </div>
          </div>

          <form class="pagemenu" @submit="submit">
            <div class="pagemenu-status">
              <c-realtime-users v-if="status && users" :status :users />
              <c-save-status :save-status />
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
