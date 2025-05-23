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
import ImageFilePanel from "./blocks/ImageFilePanel.vue";
import ImageToolbar from "./blocks/ImageToolbar.vue";
import LinkToolbar from "./blocks/LinkToolbar.vue";
import ParagraphToolbar from "./blocks/ParagraphToolbar.vue";
import { EditorType } from "../blocknote";
import { BlockNoteToUniAstConverter } from "../blocknote/bn-to-uniast";
import { UniAstToBlockNoteConverter } from "../blocknote/uniast-to-bn";
import { AutoSaver } from "../components/autoSaver";
import { computeCurrentUser } from "../components/currentUser";
import { createLinkEditionContext } from "../components/linkEditionContext";
import { autoSaverRef, providerRef } from "../components/realtimeState";
import {
  BlockNoteViewWrapper,
  BlockNoteViewWrapperProps,
} from "../react/BlockNoteView";
import messages from "../translations";
import { HocuspocusProvider } from "@hocuspocus/provider";
import {
  DocumentService,
  name as documentServiceName,
} from "@xwiki/cristal-document-api";
import {
  ReactNonSlotProps,
  reactComponentAdapter,
} from "@xwiki/cristal-reactivue";
import {
  UniAst,
  UniAstToMarkdownConverter,
  createConverterContext,
} from "@xwiki/cristal-uniast";
import { Container } from "inversify";

import { debounce } from "lodash-es";
import { shallowRef, watch } from "vue";
import { createI18n, useI18n } from "vue-i18n";
import type { SkinManager } from "@xwiki/cristal-api";
import type { AuthenticationManagerProvider } from "@xwiki/cristal-authentication-api/dist";

const {
  editorProps,
  editorContent: uniAst,
  realtimeServerURL = undefined,
  container,
  skinManager,
} = defineProps<{
  editorProps: Omit<
    ReactNonSlotProps<BlockNoteViewWrapperProps>,
    "content" | "prefixDefaultFormattingToolbarFor" | "pendingSyncMessage"
  >;
  editorContent: UniAst | Error;
  realtimeServerURL?: string | undefined;
  container: Container;
  skinManager: SkinManager;
}>();

const editorRef = shallowRef<EditorType | null>(null);

const emit = defineEmits<{
  // TODO: the type of the content might change!
  "blocknote-save": [content: string];
}>();

defineExpose({
  getContent: (): string | Error => extractEditorContent(),
});

/**
 * Extract the editor's content and convert it to Markdown
 */
function extractEditorContent(): string | Error {
  const editor = editorRef.value!;
  const uniAst = blockNoteToUniAst.blocksToUniAst(editor.document);

  if (uniAst instanceof Error) {
    // TODO: show proper error to user
    throw uniAst;
  }

  return uniAstToMarkdown.toMarkdown(uniAst);
}

/**
 * Notify the parent component the editor's content changed
 */
function notifyChanges(): void {
  const content = extractEditorContent();

  // TODO: error reporting
  if (!(content instanceof Error)) {
    emit("blocknote-save", content);
  }
}

/**
 * This function's purpose is to build the realtime provider that will be used throughout the app
 */
// eslint-disable-next-line max-statements
async function getRealtimeProvider(): Promise<
  NonNullable<BlockNoteViewWrapperProps["blockNoteOptions"]>["collaboration"]
> {
  const documentService = container.get<DocumentService>(documentServiceName);
  const authenticationManager = container
    .get<AuthenticationManagerProvider>("AuthenticationManagerProvider")
    .get()!;

  if (!realtimeServerURL) {
    return undefined;
  }

  const documentReference =
    documentService.getCurrentDocumentReferenceString().value;

  if (!documentReference) {
    throw new Error("Got no document reference!");
  }

  const provider = new HocuspocusProvider({
    url: realtimeServerURL,
    // we distinguish from sessions from other editors with a ':blocknote' suffix.
    name: `${documentReference}:blocknote`,
  });

  autoSaverRef.value = new AutoSaver(provider, async () => notifyChanges());

  const user = await computeCurrentUser(authenticationManager);

  providerRef.value = provider;

  return {
    provider,
    fragment: provider.document.getXmlFragment("document-store"),
    user,
  };
}

const provider = await getRealtimeProvider();

if (!realtimeServerURL && editorRef.value) {
  watch(editorRef.value, (editor) => {
    if (editor) {
      const debouncedSave = debounce(notifyChanges, 500);

      editor?.onChange(debouncedSave);
    }
  });
}

const { t } = useI18n({
  messages,
});

// Build the properties object for the React BlockNoteView component
const initializedEditorProps: Omit<
  ReactNonSlotProps<BlockNoteViewWrapperProps>,
  "content"
> = {
  ...editorProps,
  editorRef,
  pendingSyncMessage: t("blocknote.realtime.syncing"),
  prefixDefaultFormattingToolbarFor: [
    "paragraph",
    "quote",
    "heading",
    "Heading4",
    "Heading5",
    "Heading6",
    "bulletListItem",
    "checkListItem",
    "numberedListItem",
    "column",
    "columnList",
    "codeBlock",
    "table",
  ],
  blockNoteOptions: {
    ...editorProps.blockNoteOptions,
    collaboration: provider,
  },
};

const BlockNoteViewAdapter = reactComponentAdapter(BlockNoteViewWrapper, {
  modifyVueApp: (app) => {
    skinManager.loadDesignSystem(app, container);

    app.use(createI18n({ legacy: false, fallbackLocale: "en" }));

    app.provide("container", container);
  },
});

const linkEditionCtx = createLinkEditionContext(container);
const converterContext = createConverterContext(container);

const blockNoteToUniAst = new BlockNoteToUniAstConverter(converterContext);
const uniAstToMarkdown = new UniAstToMarkdownConverter(converterContext);

const uniAstToBlockNote = new UniAstToBlockNoteConverter(converterContext);

const content =
  uniAst instanceof Error
    ? uniAst
    : uniAstToBlockNote.uniAstToBlockNote(uniAst);
</script>

<template>
  <h1 v-if="content instanceof Error">
    {{ t("blocknote.document.parsingError", { reason: content }) }}
  </h1>

  <BlockNoteViewAdapter v-else v-bind="initializedEditorProps" :content>
    <!-- Custom (popover) formatting toolbar -->
    <template #formattingToolbar="{ editor, currentBlock }">
      <ImageToolbar
        v-if="currentBlock.type === 'image'"
        :editor
        :current-block
        :link-edition-ctx
      />

      <ParagraphToolbar
        v-else-if="currentBlock.type === 'paragraph'"
        :editor
        :current-block
        :link-edition-ctx
      />

      <!--
        NOTE: This is the expected behaviour once we've implemented a custom toolbar for **ALL** block types
        In the meantime, we'll keep using BlockNote's default toolbar when we don't have our own one
      -->

      <!--<strong v-else>Unknown block type: {{ currentBlock.type }}</strong>-->
    </template>

    <!-- Custom (popover) toolbar for link edition -->
    <template #linkToolbar="{ editor, linkToolbarProps }">
      <div class="shadow">
        <LinkToolbar :editor :link-toolbar-props :link-edition-ctx />
      </div>
    </template>

    <!-- Custom (popover) file panel for editing file-like blocks -->
    <template #filePanel="{ editor, filePanelProps }">
      <ImageFilePanel
        v-if="filePanelProps.block.type === 'image'"
        :editor
        :current-block="
          filePanelProps.block as any /* required as filePanelProps.block is not narrowed enough here */
        "
        :link-edition-ctx
      />

      <strong v-else>
        Unexpected file type block: {{ filePanelProps.block.type }}
      </strong>
    </template>
  </BlockNoteViewAdapter>
</template>

<style scoped>
.shadow {
  box-shadow: 0px 4px 12px #cfcfcf;
  border-radius: 6px;
  padding: 2px;
}
</style>
