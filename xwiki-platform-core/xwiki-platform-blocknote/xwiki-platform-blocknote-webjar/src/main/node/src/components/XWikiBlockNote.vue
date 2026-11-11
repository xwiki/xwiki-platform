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
<template>
  <div class="xwiki-blocknote" ref="root" v-if="!isLoading">
    <suspense>
      <BlocknoteEditor
        v-if="editorContent"
        ref="editor"
        :editor-props
        :editor-content
        :deps-container="container"
        :excluded-link-target-types="excludedLinkTargetTypes"
        :link-context
        :macros
        :collaboration
        @instant-change="dirty = true"
        @debounced-change="updateValue"
        @local-instant-change="onLocalInstantChange"
      ></BlocknoteEditor>
    </suspense>
    <input
      v-if="name"
      ref="valueInput"
      type="hidden"
      :name
      :value
      :form
      :disabled
    />
    <input
      v-if="name"
      type="hidden"
      name="RequiresConversion"
      :value="name"
      :form
      :disabled
    />
    <input
      v-if="name"
      type="hidden"
      :name="name + '_inputSyntax'"
      :value="inputSyntax"
      :form
      :disabled
    />
    <input
      v-if="name"
      type="hidden"
      :name="name + '_outputSyntax'"
      :value="outputSyntax"
      :form
      :disabled
    />
    <input
      v-if="collaborationKey"
      type="hidden"
      name="collaboration"
      :value="collaborationKey"
      :form
      :disabled
    />
    <!-- The files uploaded to the temporary store, which the save request attaches to the edited document. -->
    <input
      v-for="fileName in temporaryAttachments"
      :key="fileName"
      type="hidden"
      name="uploadedFiles"
      :value="fileName"
      :form
      :disabled
    />
  </div>
</template>

<script setup lang="ts">
import { BlockNoteDocument } from "../services/blocknote/BlockNoteProcessor";
import { toRelativeAttachmentResourceReference } from "../services/blocknote/attachmentResourceReference";
import { createFileUploader } from "../services/blocknote/createFileUploader";
import { createLinkEditionHooks } from "../services/blocknote/linkEditionHooks";
import {
  ConnectionStatus,
  collaborationManagerProviderName,
} from "@xwiki/platform-collaboration-api";
import { joinFormCollaboration } from "@xwiki/platform-collaboration-xwiki";
import { XWikiDocument } from "@xwiki/platform-document-xwiki";
import { BlocknoteEditor } from "@xwiki/platform-editors-blocknote-headless";
import { MINIMAL_SYNTAX_NAME } from "@xwiki/platform-minimal-syntax-config";
import { toCristalEntityReference } from "@xwiki/platform-model-xwiki";
import { SYNTAX_CONFIG_COMPONENT_GROUP_NAME } from "@xwiki/platform-syntaxes-config";
import { EntityType, Model } from "@xwiki/platform-xwiki-model-api";
import { Container } from "inversify";
import {
  inject,
  onBeforeMount,
  onUnmounted,
  ref,
  shallowRef,
  useTemplateRef,
  watch,
} from "vue";
import type { Logic } from "../services/Logic";
import type { BlockNoteProcessor } from "../services/blocknote/BlockNoteProcessor";
import type { FileUploader } from "../services/blocknote/createFileUploader";
import type { ImageWizard } from "../services/image/ImageWizard";
import type { BlockNoteMacroWizard } from "../services/macros/MacroWizard";
import type { UserSuggest } from "../services/mentions/UserSuggest";
import type { CristalApp } from "@xwiki/platform-api";
import type {
  Collaboration,
  CollaborationManagerProvider,
} from "@xwiki/platform-collaboration-api";
import type { FormCollaboration } from "@xwiki/platform-collaboration-xwiki";
import type {
  BlockNoteViewWrapperProps,
  BlockType,
  EditorLanguage,
  ImageEditionHooks,
  ImageEditionOverrideFn,
  ImageInsertionOverrideFn,
  UserSuggestFn,
} from "@xwiki/platform-editors-blocknote-react";
import type { LinkContext } from "@xwiki/platform-link-type-api";
import type { DocumentReference } from "@xwiki/platform-model-api";
import type { ResourceReference } from "@xwiki/platform-rendering-api";
import type { SyntaxConfig } from "@xwiki/platform-syntaxes-config";
import type { XWikiMeta } from "@xwiki/platform-xwiki-utils";
import type { Ref } from "vue";

//
// Injected
//
const container = inject<Container>("container")!;
const logic = inject<Logic>("logic")!;
const blockNoteProcessor: BlockNoteProcessor = container.get(
  "BlockNoteProcessor",
  {
    name: "XWiki",
  },
);
const xwikiMeta: XWikiMeta = container.get("XWikiMeta");

//
// Props
//
const {
  name = undefined,
  initialValue = "",
  form = undefined,
  disabled = false,
  uploadDisabled = false,
  inputSyntax = "blocknote/1.0",
  outputSyntax = "xwiki/2.1",
  collaborationURL = undefined,
  userScope = undefined,
  documentReference = Model.serialize(XWiki.currentDocument.documentReference),
  locale,
  startupFocus = undefined,
} = defineProps<{
  // The key used to submit the edited content.
  name?: string;

  // The initial content when the editor is created.
  initialValue?: string;

  // The ID of the form this editor is associated with.
  form?: string;

  // Prevent the edited content and the conversion metadata from being submitted.
  disabled?: boolean;

  // Prevent the user from attaching files to the edited document, set when the edited content is restricted and the
  // user doesn't have edit right on the source document (e.g. when commenting a page they can't edit).
  uploadDisabled?: boolean;

  // The syntax of the edited content, as expected by the editor.
  inputSyntax?: string;

  // The syntax of the edited content, as expected by the back-end storage.
  outputSyntax?: string;

  // The URL of the collaboration server used for real-time editing. If not specified, real-time editing is disabled.
  collaborationURL?: string;

  // The user scope of the wiki, which says whether its local users, the global ones, or both can be mentioned.
  userScope?: string;

  // The reference of the XWiki document whose field is being edited using BlockNote. This is required for real-time
  // collaboration in order to join the corresponding collaboration session. It is also used to contextualize some of
  // the editor features.
  documentReference?: string;

  /**
   * The locale to use for the editor UI, and also the locale of the edited content, in case the provided document
   * reference doesn't specify a locale.
   */
  locale?: string;

  // Whether to focus the editor when it's created. It's a string when it comes from an HTML attribute.
  startupFocus?: boolean | string;
}>();

const actualLocale = locale || xwikiMeta.locale;
const editedDocumentReference =
  Model.resolve(documentReference, EntityType.DOCUMENT) ?? undefined;

//
// Data
//
const value = ref(initialValue);
const dirty = ref(false);
const isLoading = ref(true);

let blockNoteDocument: BlockNoteDocument | undefined;

// This is passed to the BlockNote editor component.
const editorContent = ref();

const defaultLabel = "Editor";

const imageEdition: ImageEditionOverrideFn = (block, update) => {
  const imageWizard: ImageWizard = container.get("ImageWizard");
  // The XWiki resource reference is kept as block metadata (outside the BlockNote schema), so we pass it to the image
  // wizard and update it on submit, because the user may have selected a different image.
  const reference = blockNoteDocument?.getMetadata(block.id)?.xwikiReference as
    | ResourceReference
    | undefined;
  imageWizard.edit(
    { ...block.props, reference },
    {
      submit: ({ reference: newReference, ...updatedProps }) => {
        if (newReference) {
          blockNoteDocument!.getMetadata(block.id, true)!.xwikiReference =
            newReference;
        }
        update({ type: "update", updatedProps });
      },
      cancel: () => update({ type: "aborted" }),
    },
    { documentReference: editedDocumentReference },
  );
};

const imageInsertion: ImageInsertionOverrideFn = (insert) => {
  const imageWizard: ImageWizard = container.get("ImageWizard");
  // The image wizard lets the user select the image to insert (among the existing images, or by uploading a new one)
  // and set its properties. The resource reference of the selected image is stored by the image edition hooks.
  imageWizard.insert(
    {
      submit: ({ reference, ...props }) =>
        insert({ type: "insert", props, reference }),
      cancel: () => insert({ type: "aborted" }),
    },
    { documentReference: editedDocumentReference },
  );
};

const imageEditionHooks: ImageEditionHooks = {
  // Runs right after an image is inserted or its source is changed (e.g. from the image suggestions or by uploading
  // it). The XWiki resource reference is kept as block metadata (outside the BlockNote schema), so that the image is
  // saved with the reference provided by the server rather than with its URL, relative to the edited document. An image
  // known only by its URL is saved with that URL, so we drop the reference of the previous image source.
  afterUpdate: (block, reference) => {
    const metadata = blockNoteDocument?.getMetadata(block.id, !!reference);
    if (reference?.type === "attach") {
      metadata!.xwikiReference = {
        ...(editedDocumentReference
          ? toRelativeAttachmentResourceReference(
              reference,
              editedDocumentReference,
            )
          : reference),
        // Untyped, like the image references written by the WYSIWYG editors (an image points to an attachment by
        // default).
        typed: false,
      };
    } else if (reference) {
      metadata!.xwikiReference = reference;
    } else if (metadata) {
      delete metadata.xwikiReference;
    }
  },
};

// The link modal resolves the references of the linked pages and attachments against the edited document, and saves them
// relative to it, like the image references and like the WYSIWYG editors do, so that the links keep working when the
// edited document is renamed or copied.
const linkContext: LinkContext = {
  sourceDocument: toCristalEntityReference(editedDocumentReference) as
    | DocumentReference
    | undefined,
};

// Path and UNC path links are rarely created from the editor, so the link modal doesn't offer them for new links, to
// keep the link type selector short. Existing path and UNC path links are still edited with their own link type.
const excludedLinkTargetTypes = ["path", "unc"];

const linkEdition = createLinkEditionHooks(
  () => blockNoteDocument,
  editedDocumentReference,
);

// The files uploaded to the temporary store, submitted with the edit form so that the save attaches them.
const temporaryAttachments: Ref<Set<string>> = ref(new Set<string>());

// Created only once the editor is in the DOM, because that is what the uploader listens on. See the watch below.
let fileUploader: FileUploader | undefined;

// BlockNote asks for an upload only from the editor, which exists just as long as the uploader does.
const uploadFile = uploadDisabled
  ? undefined
  : (file: File, blockId?: string) => fileUploader!.upload(file, blockId);

/**
 * @param eventTarget - where the uploader listens for the paste and drop events that tell it how a file reached the
 *   editor
 * @returns the uploader attaching the pasted and dropped files to the edited document
 */
function newFileUploader(eventTarget: EventTarget): FileUploader {
  return createFileUploader(container, {
    eventTarget,
    getDocumentReference: () => documentReference,
    getBlockNoteDocument: () => blockNoteDocument,
    // A file in the temporary store is visible only to the session that uploaded it, and only a save made from that
    // session attaches it to the document. In a realtime session any of the collaborators can be the one that saves,
    // so we attach the file right away instead, like the CKEditor integration does. The same goes when the editor
    // has no field name, because then nothing it holds is submitted and the file would never be attached.
    isTemporaryUpload: () =>
      !!name &&
      collaboration.value?.connectionStatus.value !==
        ConnectionStatus.Connected,
    registerTemporaryAttachment: (fileName) => {
      temporaryAttachments.value.add(fileName);
    },
    notifyUploadFailed: async (fileName, error) => {
      console.error(`Failed to upload [${fileName}].`, error);
      try {
        const message = await translate("upload.failed", undefined, fileName);
        XWiki.widgets.Notification.show(message, "error");
      } catch (notificationError) {
        // Nobody awaits this callback, so an error here would go unhandled.
        console.error("Failed to notify the user.", notificationError);
      }
    },
  });
}

const userSuggest: UserSuggestFn = (query) => {
  const userSuggestService: UserSuggest = container.get("UserSuggest");
  return userSuggestService.getUsers(query, userScope);
};

const syntaxes = container.getAll<SyntaxConfig>(
  SYNTAX_CONFIG_COMPONENT_GROUP_NAME,
);

let syntax = syntaxes.find((conf) => conf.id === outputSyntax);

if (!syntax) {
  // Falling back to the minimal syntax disables most editor features (headings, tables, images,
  // lists, macros, ...). Warn so that a missing / mismatched syntax config doesn't silently cripple
  // the editor.
  console.warn(
    `No syntax configuration registered for '${outputSyntax}', falling back to the minimal ` +
      "syntax: most editor features will be disabled.",
  );
  syntax = syntaxes.find((conf) => conf.id === MINIMAL_SYNTAX_NAME);
}

if (!syntax) {
  throw new Error(
    "Document syntax is not supported, and minimal syntax is not available",
  );
}

// This is passed to the BlockNote editor component.
const editorProps = shallowRef<
  InstanceType<typeof BlocknoteEditor>["$props"]["editorProps"]
>({
  blockNoteOptions: {
    // We want the edited content to be styled using the XWiki skin / color theme as musch as possible, in order to have
    // consistency between edit and view modes.
    defaultStyles: false,
    uploadFile,
    // Put the caret at the start of the content when the editor is focused on creation.
    autofocus:
      startupFocus === true || startupFocus === "true" ? "start" : false,
  },
  theme: "light",
  lang: (actualLocale || "en") as EditorLanguage,
  // The images uploaded from the editor are attached to the edited document.
  documentReference,
  // Style the code blocks like the code macro output in view mode.
  codeBlockClassName: "box code",
  label: defaultLabel,
  overrides: {
    imageEdition,
    imageEditionHooks,
    imageInsertion,
    linkEdition,
  },
  userSuggest,
  syntax,
});

// This is passed to the BlockNote editor component.
// Shallow on purpose: a normal ref would hide the refs inside, leaving connectionStatus.value undefined.
const collaboration = shallowRef<Collaboration | undefined>(undefined);
// Set as soon as we start joining, so that we can leave even if the editor is unmounted before the join completes.
let formCollaboration: Promise<FormCollaboration> | undefined;
// Set once the join completes, together with the collaboration passed to the BlockNote editor component.
let joinedCollaboration: FormCollaboration | undefined;
const collaborationKey: Ref<string | undefined> = ref();

onBeforeMount(async () => {
  blockNoteDocument = blockNoteProcessor.load(initialValue);
  editorContent.value = blockNoteDocument.content;

  editorProps.value.label = await translate("editor.label", defaultLabel);

  if (collaborationURL && documentReference) {
    try {
      await joinCollaborationSession();
    } catch (error) {
      // Joining doesn't wait for the connection to the collaboration server (we can join disconnected) so this happens
      // only if the collaboration session can't be created at all. Let the user edit (without realtime) nevertheless.
      console.error(
        "Failed to join the realtime collaboration session. Editing without realtime collaboration.",
        error,
      );
    }
  }

  isLoading.value = false;
});

/**
 * Join the realtime collaboration session for the edited XWiki document, using the collaboration server URL.
 */
async function joinCollaborationSession(): Promise<void> {
  const cristalApp = container.get<CristalApp>("CristalApp");
  cristalApp.getWikiConfig().realtimeURL = collaborationURL;

  // Resolved before joining the collaboration session, so that starting the auto-save doesn't have to wait for it.
  const autoSaveVersionSummary = await translate("autoSaveSummary");
  formCollaboration = joinFormCollaboration({
    collaborationManager: container
      .get<CollaborationManagerProvider>(collaborationManagerProviderName)
      .get(),
    // The collaboration session is identified by the reference and the locale of the edited document.
    document: getEditedDocument(),
    formId: form,
    autoSaveVersionSummary,
  });
  joinedCollaboration = await formCollaboration;
  collaboration.value = joinedCollaboration.collaboration;
  collaborationKey.value = `${encodeURIComponent(documentReference)}/${encodeURIComponent(actualLocale)}`;
}

onUnmounted(async () => {
  fileUploader?.dispose();
  // The realtime toolbar is removed and the auto-saver is stopped when the last editor leaves the session.
  try {
    (await formCollaboration)?.leave();
  } catch (error) {
    console.debug("Failed to join the collaboration session.", error);
  }
});

/**
 * Tell the auto-saver that the local user changed the content. The editor reports only the local changes here, so
 * neither the initial content load nor the updates received from the other collaborators are counted.
 */
function onLocalInstantChange(): void {
  collaboration.value?.saver?.contentModifiedLocally();
}

/**
 * @param key - the translation key, without the prefix the localization module adds back
 * @param fallback - what to return when the key has no translation
 * @param params - the parameters of the translation message
 * @returns the translated message
 */
async function translate(
  key: string,
  fallback?: string,
  ...params: unknown[]
): Promise<string> {
  const fullKey = `blocknote.${key}`;
  const translation = await logic.translate(fullKey, ...params);
  // vue-i18n echoes the key back when it has no translation for it.
  return translation === fullKey ? (fallback ?? translation) : translation;
}

/**
 * @returns the document being edited, which the auto-saver keeps up to date with the versions the other clients
 *   create, so that the next save doesn't run into a merge conflict
 */
function getEditedDocument(): XWikiDocument {
  if (
    Model.serialize(xwikiMeta.documentReference) === documentReference &&
    actualLocale === xwikiMeta.locale
  ) {
    // The page displays the very document we're editing, so use the factory that also keeps the page meta data and
    // the hidden fields of the edit form in sync after each save.
    return XWikiDocument.currentDocument(xwikiMeta);
  }
  return new XWikiDocument({
    documentReference: editedDocumentReference,
    language: actualLocale,
  });
}

// This is passed to the BlockNote editor component. Macros are inserted / edited directly as the server-rendered
// xwikiMacroBlock / xwikiInlineMacro blocks: the wizard operates on macro invocations, and the editor stores the
// resulting invocation in the block. The list of client-rendered macros is left empty (they are not used here).
const macros: BlockNoteViewWrapperProps["macros"] = {
  ctx: {
    // The macro output is kept outside the edited content (which is shared with the other users when editing in
    // realtime) because it was rendered for the current user.
    getMacroOutput: (id, call) => blockNoteDocument?.getMacroOutput(id, call),

    openParamsEditor: async (invocation, update) => {
      try {
        const macroWizard: BlockNoteMacroWizard = container.get(
          "BlockNoteMacroWizard",
        );
        update(
          await macroWizard.insertOrUpdate(invocation, {
            syntax: outputSyntax,
            inlineParametersSyntax: inputSyntax,
          }),
        );
      } catch (error) {
        if (error) {
          console.error("Failed to edit the macro", error);
        }
      }
    },

    openInsertionEditor: async (prefill, insert) => {
      try {
        const macroWizard: BlockNoteMacroWizard = container.get(
          "BlockNoteMacroWizard",
        );
        insert(
          await macroWizard.insert(prefill.kind, prefill.params, prefill.body),
        );
      } catch (error) {
        if (error) {
          console.error("Failed to insert the macro", error);
        }
      }
    },
  },
};

//
// Computed
//
const valueInput = useTemplateRef<HTMLInputElement>("valueInput");
const editorInstance =
  useTemplateRef<InstanceType<typeof BlocknoteEditor>>("editor");
const root = useTemplateRef<HTMLElement>("root");

// The editor is rendered only once its content is loaded, so it isn't in the DOM yet when this component is set up.
// Watching its element rather than creating the uploader on mount also keeps the two in sync if it is ever re-created.
watch(root, (element) => {
  fileUploader?.dispose();
  fileUploader =
    element && !uploadDisabled ? newFileUploader(element) : undefined;
});

// Start the auto-save (and show the realtime toolbar) only once the editor is there, because the first client joining
// a session loads the initial content into the shared document, and that is a local Yjs change: counting it would
// make the session dirty and trigger an auto-save before anyone typed anything.
watch(editorInstance, (editor) => {
  if (editor) {
    // The auto-saver belongs to the collaboration session, shared by every editor taking part in it, so only the first
    // editor to get here starts it.
    joinedCollaboration?.startAutoSave();
  }
});

//
// Methods
//
// eslint-disable-next-line max-statements
function updateValue(editorContent?: BlockType[]): string {
  if (!dirty.value) {
    // The value is already up-to-date.
    return value.value;
  }

  const instantUpdate = !editorContent;
  editorContent = editorContent || editorInstance.value?.getContent();
  if (!editorContent) {
    throw new Error("Could not get the editor content.");
  }

  blockNoteDocument!.content = editorContent;
  const newValue = blockNoteProcessor.save(blockNoteDocument!);

  value.value = newValue;
  dirty.value = false;

  if (instantUpdate) {
    // Update the value input immediately. This is important for instance when the form containing the BlockNote
    // editor is submitted. Alternatively, we have to delay the form submission until the next tick when Vue will
    // have updated the value input, but that is more complex.
    valueInput.value!.value = newValue;
  }

  return newValue;
}

defineExpose({
  updateValue,
});
</script>

<style>
.xwiki-blocknote {
  .bn-container {
    --bn-font-family: unset;
    --mantine-font-size-md: inherit;
    --mantine-font-size-xs: 80%;
  }

  .bn-editor {
    --bn-colors-editor-text: unset;
    --bn-colors-editor-background: unset;
    /* Overwrite the inline padding coming from BlockNote. Note that we don't set it to 0 because it leads to a
      horizontal scrollbar in Firefox. */
    padding-inline: 0 1px;

    h5 {
      font-size: unset; /* --font-size-h5 */
    }

    h6 {
      font-size: 13px; /* --font-size-h6 */
    }

    [data-content-type="bulletListItem"] > p.bn-inline-content,
    [data-content-type="numberedListItem"] > p.bn-inline-content {
      margin: 0;
    }

    .bn-block-content[data-content-type="codeBlock"] {
      /* The code block is styled by the XWiki skin, like the code macro output in view mode, through the CSS classes
        added to its pre element (see codeBlockClassName). */
      background: transparent;
      border-radius: 0;

      /* Use the same spacing as the code macro output in view mode. */
      > pre {
        padding: 19px; /* .well */
        margin: 0 0 20px; /* @line-height-computed */
      }

      /* The XWiki skin styles select elements as form fields, but the language selector should look like plain
        text. */
      > div > select {
        width: auto;
        height: auto;
        padding: 0;
        border: 0 none;
        border-radius: 0;
        background: transparent;
        box-shadow: none;
        font-size: 0.8em;
        /* Display the language selector in the top padding of the code block, aligned with the code. */
        line-height: 1;
        top: 4px;
        left: 20px; /* border width + padding */

        &:focus {
          box-shadow: none;
        }
      }
    }

    [data-content-type="table"] {
      table {
        margin-bottom: 0;
      }

      th,
      td {
        border: 0 none;
        border-top: 1px solid var(--table-border-color);
        padding: 8px 10px; /* @table-cell-padding */

        > p {
          margin-bottom: 0;
        }
      }
    }
  }

  /* Overwrite styles coming from XWiki */
  .bn-editor ~ * .container {
    width: auto;
    padding: 0;
    margin: 0;
  }

  .bn-toolbar {
    button,
    select {
      --ai-size: 30px !important;
    }
  }

  .bn-block-outer {
    line-height: unset;
  }

  .bn-block-content {
    padding: 0;
  }

  .link-editor label {
    line-height: 34px; /* @input-height-base */
    margin-bottom: 0;
  }
}

/**
 * Standalone edit mode
 */

/* There's no border around the content editor so we need to show the top border of the action toolbar. */
form#edit .bottom-editor > .sticky-buttons {
  border-top: var(--border-width) solid var(--input-border);
  border-top-left-radius: var(--border-radius-base);
  border-top-right-radius: var(--border-radius-base);
}

/* There's no border around the content editor so we need to leave some space before the action toolbar. */
#xwikieditcontent > .xwiki-blocknote-wrapper {
  margin-bottom: calc(var(--grid-gutter-width) / 2);
}

/**
 * In-place edit mode
 */

/* The editor is created inside #xwikicontent, which the skin makes a scroll container in order to handle wide content
  in view mode. The floating UI of the editor (e.g. the slash menu) is positioned and clipped based on its scroll
  containers, so on a page with little content it would be squeezed to the small height of #xwikicontent. */
#xwikicontent.xwiki-blocknote-wrapper {
  overflow: visible;
}
</style>
