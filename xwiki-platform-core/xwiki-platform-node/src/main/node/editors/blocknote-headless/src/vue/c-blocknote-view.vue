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
import { moveCaretTo } from "./caret";
import { mountBlockNote } from "@xwiki/platform-editors-blocknote-react";
import { LinkModal } from "@xwiki/platform-link-modal-ui";
import {
  linkTargetTypeManagerRole,
  unsupportedLinkTargetType,
} from "@xwiki/platform-link-type-api";
import { Container } from "inversify";
import { debounce } from "lodash-es";
import {
  computed,
  onBeforeUnmount,
  onMounted,
  onUnmounted,
  ref,
  shallowRef,
  toRaw,
  useTemplateRef,
} from "vue";
import type { Collaboration } from "@xwiki/platform-collaboration-api";
import type {
  BlockNoteViewWrapperProps,
  BlockType,
  ContextForMacros,
  EditorType,
  LinkEditionData,
  LinkEditionHandlerProps,
} from "@xwiki/platform-editors-blocknote-react";
import type {
  LinkData,
  LinkTarget,
  LinkTargetTypeManager,
} from "@xwiki/platform-link-type-api";
import type { MacroWithUnknownParamsType } from "@xwiki/platform-macros-api";

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
        /** Optional list of client-rendered macros; omit it when only the server-rendered macros are used. */
        list?: MacroWithUnknownParamsType[];
        ctx: ContextForMacros;
      }
    | false;

  /** Content to initialize the editor with */
  editorContent: BlockType[];

  collaboration?: Collaboration;

  /** Container to inject dependencies from */
  depsContainer: Container;

  /**
   * The link target types (by type) that the link modal doesn't offer for new links, while still using them to edit
   * the links they handle. Unknown types are ignored.
   */
  excludedLinkTargetTypes?: string[];
};

const {
  editorProps,
  editorContent,
  macros,
  collaboration = undefined,
  depsContainer,
  excludedLinkTargetTypes = undefined,
} = defineProps<Props>();

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
 * Inserts inline content where the user is, for the integrations that insert content of their own rather than
 * through BlockNote.
 *
 * @param content - what to insert
 * @param coordinates - where the pointer is, for an insertion that follows a drop, since a drop leaves the caret
 *   behind
 */
function insertInlineContent(
  content: Parameters<EditorType["insertInlineContent"]>[0],
  coordinates?: { left: number; top: number },
): void {
  const editor = editorRef.value;
  if (!editor) {
    return;
  }
  if (coordinates) {
    moveCaretTo(editor, coordinates);
  }
  // updateSelection leaves the caret after the content, so a second insertion lines up behind the first.
  editor.insertInlineContent(content, { updateSelection: true });
}

defineExpose({
  getContent,
  insertInlineContent,
});

/**
 * Notify the parent component the editor's content changed
 */
function notifyChanges(): void {
  emit("debounced-change", getContent());
}

const notifyChangesDebounced = debounce(notifyChanges, 500);

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
      unsubscribeFromLocalChanges?.();
      // Ask the editor itself which changes are the local user's, rather than watching the shared Yjs document: a
      // document update cannot tell typing apart from the initial content load, since both are local Yjs changes.
      unsubscribeFromLocalChanges = editor.onChange(
        () => emit("local-instant-change"),
        false,
      );
    },
  },
  depsContainer,
  linkEditionHandler: (props) => {
    editingLink.value = props;
  },
};

/**
 * @returns the link target type manager, resolved each time it's needed, so that it uses the link target types
 *   registered at that time
 */
function getLinkTargetTypeManager(): LinkTargetTypeManager {
  return depsContainer.get<LinkTargetTypeManager>(linkTargetTypeManagerRole);
}

const submitEditedLink = ({ displayText, target }: LinkData) => {
  const linkTargetTypeManager = getLinkTargetTypeManager();
  // The URL of a resource that no link target type supports can't be computed client-side, so keep the URL of the
  // edited link. It's only a preview anyway: the link is saved from its reference.
  const url =
    target.type === unsupportedLinkTargetType
      ? (editingLink.value?.current.url ?? "")
      : linkTargetTypeManager.toURL(target);
  const reference = linkTargetTypeManager.toReference(target);

  editingLink.value?.onSubmit({
    title: displayText,
    url,
    reference,
  });

  editingLink.value = null;
};

const blockNoteContainer = useTemplateRef<HTMLElement>("blocknote-container");
const linkModalContainer = useTemplateRef<HTMLElement>("link-modal-container");

const mountedBlockNote = ref<{ unmount: () => void }>();

const editingLink = shallowRef<LinkEditionHandlerProps | null>(null);

/**
 * The reference of the linked resource is the authoritative link target (the URL is only a rendering
 * of it), so build the target to configure in the link modal from that reference when the
 * integration provides one, even when no link target type supports it (the reference is then edited as
 * is), and fall back on reverse-engineering the URL otherwise.
 */
function currentLinkTarget(current: LinkEditionData): LinkTarget {
  const linkTargetTypeManager = getLinkTargetTypeManager();
  return current.reference
    ? linkTargetTypeManager.fromReference(current.reference)
    : linkTargetTypeManager.fromURL(current.url);
}

const currentLinkData = computed<LinkData | null>(() => {
  if (!editingLink.value) {
    return null;
  }

  return {
    displayText: editingLink.value.current.title,
    target: currentLinkTarget(editingLink.value.current),
  };
});

function handleLinkEditorOutsideClick(e: MouseEvent) {
  if (!editingLink.value || !linkModalContainer.value) {
    return;
  }

  if (!e.composedPath().includes(linkModalContainer.value)) {
    editingLink.value = null;
  }
}

onMounted(() => {
  if (!blockNoteContainer.value) {
    throw new Error("Missing DOM container for BlockNote!");
  }

  mountedBlockNote.value = mountBlockNote(blockNoteContainer.value, {
    ...initializedEditorProps,
    content: editorContent,
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
  unsubscribeFromLocalChanges?.();
  unsubscribeFromLocalChanges = undefined;
});
</script>

<template>
  <div ref="blocknote-container" />

  <div ref="link-modal-container" v-if="currentLinkData">
    <LinkModal
      :current="currentLinkData"
      :deps-container="depsContainer"
      :excluded-link-target-types="excludedLinkTargetTypes"
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
