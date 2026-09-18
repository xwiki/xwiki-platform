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
import { mountBlockNote } from "@xwiki/platform-editors-blocknote-react";
import {
  LinkModal,
  linkTargetToResourceReference,
  parseLinkTarget,
  resourceReferenceToLinkTarget,
} from "@xwiki/platform-link-modal-ui";
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
import type { Collaboration } from "@xwiki/platform-collaboration-api";
import type {
  BlockNoteViewWrapperProps,
  BlockType,
  ContextForMacros,
  EditorType,
  LinkEditionData,
  LinkEditionHandlerProps,
} from "@xwiki/platform-editors-blocknote-react";
import type { LinkData, LinkTarget } from "@xwiki/platform-link-modal-ui";
import type { MacroWithUnknownParamsType } from "@xwiki/platform-macros-api";
import type {
  ModelReferenceParserProvider,
  ModelReferenceSerializerProvider,
} from "@xwiki/platform-model-reference-api";
import type {
  RemoteURLParserProvider,
  RemoteURLSerializerProvider,
} from "@xwiki/platform-model-remote-url-api";

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
};

const {
  editorProps,
  editorContent,
  macros,
  collaboration = undefined,
  depsContainer,
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

const remoteURLParser = depsContainer
  .get<RemoteURLParserProvider>("RemoteURLParserProvider")
  .get()!;

const remoteURLSerializer = depsContainer
  .get<RemoteURLSerializerProvider>("RemoteURLSerializerProvider")
  .get()!;

const modelReferenceParser = depsContainer
  .get<ModelReferenceParserProvider>("ModelReferenceParserProvider")
  .get()!;

const modelReferenceSerializer = depsContainer
  .get<ModelReferenceSerializerProvider>("ModelReferenceSerializerProvider")
  .get()!;

function getContent(): BlockType[] {
  return editorRef.value!.document;
}

defineExpose({
  getContent,
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
 * The reference of the linked resource is the authoritative link target (the URL is only a rendering
 * of it), so build the target to configure in the link modal from that reference when the
 * integration provides one, and fall back on reverse-engineering the URL otherwise.
 */
function currentLinkTarget(current: LinkEditionData): LinkTarget {
  return (
    (current.reference &&
      resourceReferenceToLinkTarget(current.reference, modelReferenceParser)) ??
    parseLinkTarget(current.url, remoteURLParser)
  );
}

const submitEditedLink = ({ displayText, target }: LinkData) => {
  const { type, config } = target;

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
    reference: linkTargetToResourceReference(target, modelReferenceSerializer),
  });

  editingLink.value = null;
};

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

  <div ref="link-modal-container" v-if="editingLink">
    <LinkModal
      :current="{
        displayText: editingLink.current.title,
        target: currentLinkTarget(editingLink.current),
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
  /*
   * Headings need margin-top: 0 and line-height: var(--cr-line-height-normal) for the padding-top
   * formula below to hold. Bootstrap's global heading rule (type.less) sets both directly on bare
   * "h1"-"h6" elements; BlockNote's own reset for this, ".bn-default-styles h1, ..., h6
   * { margin: 0 }", only applies with "defaultStyles: true", which XWikiBlockNote.vue sets to
   * false so the wiki skin controls the editor's typography instead.
   */
  & h1 {
    font-size: var(--cr-font-size-x-large);
    line-height: var(--cr-line-height-normal);
    margin-top: 0;
  }

  & h2 {
    font-size: var(--cr-font-size-x-large);
    line-height: var(--cr-line-height-normal);
    margin-top: 0;
  }

  & h3 {
    font-size: var(--cr-font-size-large);
    line-height: var(--cr-line-height-normal);
    margin-top: 0;
  }

  & h4 {
    font-size: var(--cr-font-size-medium);
    line-height: var(--cr-line-height-normal);
    margin-top: 0;
  }

  & h5 {
    font-size: var(--cr-font-size-medium);
    line-height: var(--cr-line-height-normal);
    margin-top: 0;
  }

  & h6 {
    font-size: var(--cr-font-size-medium);
    line-height: var(--cr-line-height-normal);
    margin-top: 0;
  }

  /*
   * On hover, BlockNote positions the block handle (drag handle + "+" button) by anchoring it to
   * a block's top edge and nudging it down by a fixed offset, hardcoded per block type in
   * SideMenuController.getBlockOffset() (@blocknote/react): 39px for heading level 1, 27px for
   * level 2, 18.5px for level 3, 0px for every other type (heading levels 4-6, paragraphs, list
   * items, quotes). That offset bakes in BlockNote's own default font-size, line-height and
   * padding as a target distance from a block's top edge to its first line's vertical center:
   * 54px/42px/33.5px for heading level 1/2/3, 15px otherwise. The font-size and line-height set
   * above change that distance for every block type restyled here, so each one's padding-top is
   * set below to keep the distance at the value BlockNote's offset assumes:
   *   padding-top = <target distance for the type> - (line-height / 2)
   * A margin-top other than 0 cannot be compensated this way, since padding-top cannot go
   * negative to make room for it - which is why headings keep margin-top: 0 above. A block type's
   * padding-top here must be recomputed with this formula whenever its font-size, line-height or
   * margin changes.
   *
   * These target ".bn-block-content" (not the bare "[data-content-type]" attribute BlockNote's
   * own Block.css uses) for enough specificity to win over the ".bn-block-content { padding: 0 }"
   * rule in XWikiBlockNote.vue regardless of stylesheet order.
   */
  & .bn-block-content[data-content-type="heading"] {
    padding-top: calc(
      54px - (var(--cr-font-size-x-large) * var(--cr-line-height-normal)) / 2
    );
  }

  & .bn-block-content[data-content-type="heading"][data-level="2"] {
    padding-top: calc(
      42px - (var(--cr-font-size-x-large) * var(--cr-line-height-normal)) / 2
    );
  }

  & .bn-block-content[data-content-type="heading"][data-level="3"] {
    padding-top: calc(
      33.5px - (var(--cr-font-size-large) * var(--cr-line-height-normal)) / 2
    );
  }

  & .bn-block-content[data-content-type="heading"][data-level="4"] {
    padding-top: calc(
      15px - (var(--cr-font-size-medium) * var(--cr-line-height-normal)) / 2
    );
  }

  & .bn-block-content[data-content-type="paragraph"],
  & .bn-block-content[data-content-type="bulletListItem"],
  & .bn-block-content[data-content-type="numberedListItem"],
  & .bn-block-content[data-content-type="checkListItem"] {
    padding-top: calc(
      15px - (var(--cr-base-font-size) * var(--cr-line-height-normal)) / 2
    );
  }

  & .bn-block-content[data-content-type="quote"] {
    padding-top: calc(
      15px - (var(--cr-font-size-large) * var(--cr-line-height-normal)) / 2
    );
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
    /* Bootstrap's global blockquote rule (type.less) also sets a 10px padding-top directly on
       the bare "blockquote" element. The padding-top formula above only accounts for its
       ".bn-block-content" wrapper's own padding-top, so this needs the same reset as the heading
       margin-top and line-height above. */
    padding-top: 0;
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
