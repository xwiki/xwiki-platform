<!--
  See the NOTICE file distributed with this work for additional
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
import "@xwiki/platform-editors-blocknote-react/dist/platform-editors-blocknote-react.css";
import messages from "../translations";
import { BlockNoteToUniAstConverter } from "../uniast/bn-to-uniast";
import { UniAstToBlockNoteConverter } from "../uniast/uniast-to-bn";
import { mountBlockNote } from "@xwiki/platform-editors-blocknote-react";
import { LinkModal, parseLinkTarget } from "@xwiki/platform-link-modal-ui";
import { Container } from "inversify";
import { debounce } from "lodash-es";
import {
  onBeforeUnmount,
  onMounted,
  onUnmounted,
  ref,
  shallowRef,
  toRaw,
  useTemplateRef,
} from "vue";
import { useI18n } from "vue-i18n";
import type { Collaboration } from "@xwiki/platform-collaboration-api";
import type {
  BlockNoteViewWrapperProps,
  ContextForMacros,
  EditorType,
  LinkEditionHandlerProps,
} from "@xwiki/platform-editors-blocknote-react";
import type { LinkData } from "@xwiki/platform-link-modal-ui";
import type { MacroWithUnknownParamsType } from "@xwiki/platform-macros-api";
import type {
  RemoteURLParserProvider,
  RemoteURLSerializerProvider,
} from "@xwiki/platform-model-remote-url-api";
import type { UniAst } from "@xwiki/platform-uniast-api";

type Props = {
  /** Main properties for the BlockNote editor */
  editorProps: Omit<
    BlockNoteViewWrapperProps,
    | "depsContainer"
    | "content"
    | "linkEditionHandler"
    | "macroAstToReactJsxConverter"
    | "macros"
  >;

  /** Set to `false` to disable macros entirely */
  macros:
    | {
        list: MacroWithUnknownParamsType[];
        ctx: ContextForMacros;
      }
    | false;

  /** Content to initialize the editor with */
  editorContent: UniAst | Error;

  collaboration?: Collaboration;

  /** Container to inject dependencies from */
  depsContainer: Container;
};

const {
  editorProps,
  editorContent: uniAst,
  macros,
  collaboration = undefined,
  depsContainer,
} = defineProps<Props>();

const editorRef = shallowRef<EditorType | null>(null);

const emit = defineEmits<{
  // Emitted as soon as a user-triggered change happens into the editor
  // The event won't be triggered when the editor is filled with its initial content,
  // or when the editor's content changes due to modifications made by other players in the realtime session
  "instant-change": [];

  // Emitted in the same context as "instant-change", but debounced
  "debounced-change": [content: UniAst];
}>();

defineExpose({
  // Get the editor's content
  getContent: (): UniAst | Error => extractEditorContent(),
});

const remoteURLParser = depsContainer
  .get<RemoteURLParserProvider>("RemoteURLParserProvider")
  .get()!;

const remoteURLSerializer = depsContainer
  .get<RemoteURLSerializerProvider>("RemoteURLSerializerProvider")
  .get()!;

/**
 * Extract the editor's content and convert it to UniAst
 */
function extractEditorContent(): UniAst | Error {
  return blockNoteToUniAst.blocksToUniAst(editorRef.value!.document);
}

/**
 * Notify the parent component the editor's content changed
 */
function notifyChanges(): void {
  const content = extractEditorContent();

  // TODO: error reporting
  if (content instanceof Error) {
    throw content;
  }

  emit("debounced-change", content);
}

const notifyChangesDebounced = debounce(notifyChanges, 500);

const { t } = useI18n({
  messages,
});

// Build the properties object for the React BlockNoteView component
const initializedEditorProps: Omit<BlockNoteViewWrapperProps, "content"> = {
  ...editorProps,
  onChange: () => {
    emit("instant-change");
    notifyChangesDebounced();
  },
  blockNoteOptions: editorProps.blockNoteOptions,
  macros,
  // We need to pass the raw version of the collaboration session (but most importantly for the yjs document inside it),
  // otherwise realtime synchronization fails.
  collaboration: toRaw(collaboration),
  refs: {
    setEditor(editor) {
      editorRef.value = editor;
    },
  },
  depsContainer,
  linkEditionHandler: (props) => {
    editingLink.value = props;
  },
};

const submitEditedLink = ({
  displayText,
  target: { type, config },
}: LinkData) => {
  // TODO: support

  const url =
    type === "url"
      ? config.url
      : type === "email"
        ? `mailto:${config.address}`
        : remoteURLSerializer.serialize(config.ref!)!;

  editingLink.value?.onSubmit({
    title: displayText,
    url,
  });

  editingLink.value = null;
};

const blockNoteToUniAst = new BlockNoteToUniAstConverter(
  depsContainer,
  macros ? macros.list : [],
);

const uniAstToBlockNote = new UniAstToBlockNoteConverter(depsContainer);

const content =
  uniAst instanceof Error
    ? uniAst
    : uniAstToBlockNote.uniAstToBlockNote(uniAst);

const blockNoteContainer = useTemplateRef<HTMLElement>("blocknote-container");
const linkModalContainer = useTemplateRef<HTMLElement>("link-modal-container");

const mountedBlockNote = ref<{ unmount: () => void }>();

const editingLink = shallowRef<LinkEditionHandlerProps | null>(null);

function handleLinkEditorOutsideClick(e: MouseEvent) {
  if (!editingLink.value || !linkModalContainer.value) {
    return;
  }

  if (!e.composedPath().includes(linkModalContainer.value)) {
    editingLink.value = null;
  }
}

onMounted(() => {
  if (content instanceof Error) {
    throw content;
  }

  if (!blockNoteContainer.value) {
    throw new Error("Missing DOM container for BlockNote!");
  }

  mountedBlockNote.value = mountBlockNote(blockNoteContainer.value, {
    ...initializedEditorProps,
    content,
  });

  window.addEventListener("mousedown", handleLinkEditorOutsideClick);
});

onBeforeUnmount(() => {
  if (!mountedBlockNote.value) {
    throw new Error("BlockNote mounted data are absent");
  }

  mountedBlockNote.value.unmount();
});

onUnmounted(() => {
  window.removeEventListener("mousedown", handleLinkEditorOutsideClick);
});
</script>

<template>
  <h1 v-if="content instanceof Error">
    {{ t("blocknote.document.parsingError", { reason: content }) }}
  </h1>

  <div ref="blocknote-container" />

  <div ref="link-modal-container" v-if="editingLink">
    <LinkModal
      :current="{
        displayText: editingLink.current.title,
        target: parseLinkTarget(editingLink.current.url, remoteURLParser),
      }"
      :deps-container="depsContainer"
      @submit="submitEditedLink"
      @cancel="editingLink = null"
    />
  </div>
</template>

<style scoped>
.shadow {
  box-shadow: 0px 4px 12px #cfcfcf;
  border-radius: 6px;
  padding: 2px;
}

:deep(.bn-editor) {
  font-family: var(--cr-font-sans);
  font-size: var(--cr-base-font-size);
  font-weight: var(--cr-font-weight-normal);
  color: var(--cr-base-text-color);
  letter-spacing: var(--cr-letter-spacing-normal);
  line-height: var(--cr-line-height-normal);
  padding-inline-start: var(--cr-spacing-large);

  /* Note: font sizes are inconsistent here, but that's how they are rendered at the end. So we keep it the same here. */
  & h1 {
    font-size: var(--cr-font-size-x-large);
  }

  & h2 {
    font-size: var(--cr-font-size-x-large);
  }

  & h3 {
    font-size: var(--cr-font-size-large);
  }

  & h4 {
    font-size: var(--cr-font-size-medium);
  }

  & h5 {
    font-size: var(--cr-font-size-medium);
  }

  & h6 {
    font-size: var(--cr-font-size-medium);
  }

  /* Remove left border on lists */
  & .bn-block-group,
  .bn-block-group .bn-block-outer:not([data-prev-depth-changed])::before {
    border-left: none;
  }

  & [data-content-type="bulletListItem"] {
    padding-inline-start: var(--cr-spacing-large);
  }

  & blockquote {
    background-color: var(--cr-color-neutral-50);
    color: var(--cr-color-neutral-600);
    font-size: var(--cr-font-size-large);
    border-inline-start: 2px solid var(--cr-color-neutral-200);
    padding-inline-start: var(--cr-spacing-large);
    margin: 0;
  }

  & [data-content-type="codeBlock"] {
    background: white;
    border-radius: var(--cr-border-radius-medium);
    font-family: var(--cr-font-mono);
    color: var(--cr-base-text-color);

    & pre {
      margin: 0;
      padding: 0;
    }
  }

  /* Since BlockNote 0.51 the image element no longer gets a fallback "alt" attribute, so a broken or not-yet-loaded
    image (e.g. a missing attachment) collapses to a zero size and can no longer be selected or clicked in the editor
    (to edit or remove it). Give images a minimum size so they stay visible and selectable. Real images are larger
    than this minimum so they are not affected. */
  & .bn-visual-media {
    /* 2em minimum width to keep some space to click on the image even with the resize handles displayed on hover,
    this is especially useful for very small images, of missing images. */
    min-width: 2em;
    min-height: 1em;
  }
}
</style>
