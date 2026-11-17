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
import { bindVueServices } from "../linkEditor";
import { mountBlockNote } from "@xwiki/platform-editors-blocknote-react";
import { Container } from "inversify";
import { debounce } from "lodash-es";
import {
  getCurrentInstance,
  onBeforeUnmount,
  onMounted,
  onUnmounted,
  ref,
  shallowRef,
  toRaw,
  useTemplateRef,
} from "vue";
import type { HeadlessEditorOptions } from "../editorOptions";
import type { DocumentService } from "@xwiki/platform-document-api";
import type {
  BlockNoteViewWrapperProps,
  BlockType,
  EditorType,
} from "@xwiki/platform-editors-blocknote-react";
import type { DocumentReference } from "@xwiki/platform-model-api";

type Props = {
  /** The configuration of the editor instance */
  options: HeadlessEditorOptions;

  /** Content to initialize the editor with */
  content: BlockType[];
};

const { options, content } = defineProps<Props>();

// The options must be passed as is (raw), most importantly the collaboration session, because the realtime
// synchronization fails with a proxy of the Yjs document it holds.
const rawOptions = toRaw(options);

// The services of the Vue integration (e.g. the link editor showing the link modal) are bound in a child of the given
// service container, so that they are specific to this editor instance, and bound before the editor is created.
const serviceContainer = new Container({ parent: rawOptions.serviceContainer });
bindVueServices(serviceContainer, getCurrentInstance()!.appContext);

/**
 * @returns the reference of the document holding the edited content, as specified by the options, or the current
 *   document otherwise
 */
function getSourceDocumentReference(): DocumentReference | undefined {
  return (
    rawOptions.sourceDocumentReference ??
    (serviceContainer.isBound("DocumentService")
      ? serviceContainer
          .get<DocumentService>("DocumentService")
          .getCurrentDocumentReference().value
      : undefined)
  );
}

// Resolved once, so that the editor and the link editor agree on it.
const sourceDocumentReference = getSourceDocumentReference();

const editorRef = shallowRef<EditorType | null>(null);

let unsubscribeFromLocalChanges: (() => void) | undefined;

const emit = defineEmits<{
  // Emitted as soon as the editor's content changes, whoever caused the change: the local user, the initial content
  // being loaded, or another participant of the realtime session. Use it to invalidate anything derived from the
  // content, and "local-instant-change" to react to what the local user did.
  "instant-change": [];

  // Emitted in the same context as "instant-change", but debounced
  "debounced-change": [content: BlockType[]];

  // The subset of "instant-change" caused by the local user. It is emitted neither when the editor is filled with
  // its initial content, nor when the content changes because of another participant of the realtime session: both
  // of those reach the editor through the Yjs synchronization plugin, which is what this excludes. Note that the
  // initial content load is a local change as far as Yjs itself is concerned, so it cannot be told apart by looking
  // at the shared document.
  "local-instant-change": [];
}>();

function getContent(): BlockType[] {
  return editorRef.value!.document;
}

/**
 * @returns the editor instance, for the integrations that act on the edited content themselves, or nothing before
 *   the editor is mounted
 */
function getEditor(): EditorType | undefined {
  return editorRef.value ?? undefined;
}

defineExpose({
  getContent,
  getEditor,
});

/**
 * Notify the parent component the editor's content changed
 */
function notifyChanges(): void {
  emit("debounced-change", getContent());
}

const notifyChangesDebounced = debounce(notifyChanges, 500);

// Build the properties object for the React BlockNoteView component
const blockNoteProps: Omit<BlockNoteViewWrapperProps, "content"> = {
  options: {
    ...rawOptions,
    serviceContainer,
    // Make sure the source document reference is set (with fallback on the current document).
    sourceDocumentReference,
  },
  onChange: () => {
    emit("instant-change");
    notifyChangesDebounced();
  },
  refs: {
    setInstance({ editor }) {
      editorRef.value = editor;
      unsubscribeFromLocalChanges?.();
      // Ask the editor itself which changes are the local user's, rather than watching the shared Yjs document: a
      // document update cannot tell typing apart from the initial content load, since both are local Yjs changes.
      unsubscribeFromLocalChanges = editor.onChange(
        () => emit("local-instant-change"),
        false,
      );
    },
  },
};

/**
 * @returns the link target type manager, resolved each time it's needed, so that it uses the link target types
 *   registered at that time
 */
const blockNoteContainer = useTemplateRef<HTMLElement>("blocknote-container");

const mountedBlockNote = ref<{ unmount: () => void }>();

onMounted(() => {
  if (!blockNoteContainer.value) {
    throw new Error("Missing DOM container for BlockNote!");
  }

  mountedBlockNote.value = mountBlockNote(blockNoteContainer.value, {
    ...blockNoteProps,
    content,
  });
});

onBeforeUnmount(() => {
  if (!mountedBlockNote.value) {
    throw new Error("BlockNote mounted data are absent");
  }

  mountedBlockNote.value.unmount();
});

onUnmounted(() => {
  unsubscribeFromLocalChanges?.();
  unsubscribeFromLocalChanges = undefined;
});
</script>

<template>
  <div ref="blocknote-container" />
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

    /* Keep the padding from BlockNote because the language selector is displayed in the top padding. */
    & pre {
      margin: 0;
    }

    /* BlockNote uses the dark syntax highlighting theme because its code blocks have a dark background, but ours have
      a light background. */
    & .shiki {
      color: var(--shiki-light);
    }

    /* BlockNote displays the language selector in white, which is not visible on our light background. */
    & > div > select {
      color: inherit;
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
