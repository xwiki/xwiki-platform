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
  <div class="xwiki-blocknote" v-if="!isLoading">
    <suspense>
      <BlocknoteEditor
        v-if="editorContent"
        ref="editor"
        :editor-props
        :editor-content
        :deps-container="container"
        :macros
        :collaboration
        @instant-change="dirty = true"
        @debounced-change="updateValue"
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
  </div>
</template>

<script setup lang="ts">
import { BlockNoteDocument } from "../services/blocknote/BlockNoteProcessor";
import {
  extractLinkId,
  injectLinkId,
  stripLinkId,
} from "../services/blocknote/linkId";
import { collaborationManagerProviderName } from "@xwiki/platform-collaboration-api";
import { BlocknoteEditor } from "@xwiki/platform-editors-blocknote-headless";
import { MINIMAL_SYNTAX_NAME } from "@xwiki/platform-minimal-syntax-config";
import { SYNTAX_CONFIG_COMPONENT_GROUP_NAME } from "@xwiki/platform-syntaxes-config";
import { Container } from "inversify";
import { uuidv4 } from "lib0/random";
import {
  inject,
  onBeforeMount,
  onUnmounted,
  ref,
  shallowRef,
  useTemplateRef,
} from "vue";
import { resolver } from "xwiki-platform-localization-webjar";
import type { BlockNoteProcessor } from "../services/blocknote/BlockNoteProcessor";
import type { ImageWizard } from "../services/image/ImageWizard";
import type { BlockNoteMacroWizard } from "../services/macros/MacroWizard";
import type { XWikiMeta } from "../services/meta/XWikiMeta";
import type { CristalApp } from "@xwiki/platform-api";
import type {
  Collaboration,
  CollaborationManager,
  CollaborationManagerProvider,
} from "@xwiki/platform-collaboration-api";
import type {
  BlockNoteViewWrapperProps,
  BlockType,
  EditorLanguage,
  ImageEditionOverrideFn,
} from "@xwiki/platform-editors-blocknote-react";
import type { DocumentReference } from "@xwiki/platform-model-api";
import type { ModelReferenceParserProvider } from "@xwiki/platform-model-reference-api";
import type { ResourceReference } from "@xwiki/platform-rendering-api";
import type { SyntaxConfig } from "@xwiki/platform-syntaxes-config";
import type { Ref } from "vue";

//
// Injected
//
const container = inject<Container>("container")!;
const blockNoteProcessor: BlockNoteProcessor = container.get(
  "BlockNoteProcessor",
  {
    name: "XWiki",
  },
);
const xwikiMeta: XWikiMeta = container.get("XWikiMeta");
const modelReferenceParser = container
  .get<ModelReferenceParserProvider>("ModelReferenceParserProvider")
  .get();

//
// Props
//
const {
  name = undefined,
  initialValue = "",
  form = undefined,
  disabled = false,
  inputSyntax = "blocknote/1.0",
  outputSyntax = "xwiki/2.1",
  collaborationURL = undefined,
  documentReference = XWiki.Model.serialize(
    XWiki.currentDocument.documentReference,
  ),
  locale,
} = defineProps<{
  // The key used to submit the edited content.
  name?: string;

  // The initial content when the editor is created.
  initialValue?: string;

  // The ID of the form this editor is associated with.
  form?: string;

  // Prevent the edited content and the conversion metadata from being submitted.
  disabled?: boolean;

  // The syntax of the edited content, as expected by the editor.
  inputSyntax?: string;

  // The syntax of the edited content, as expected by the back-end storage.
  outputSyntax?: string;

  // The URL of the collaboration server used for real-time editing. If not specified, real-time editing is disabled.
  collaborationURL?: string;

  // The reference of the XWiki document whose field is being edited using BlockNote. This is required for real-time
  // collaboration in order to join the corresponding collaboration session. It is also used to contextualize some of
  // the editor features.
  documentReference?: string;

  /**
   * The locale to use for the editor UI, and also the locale of the edited content, in case the provided document
   * reference doesn't specify a locale.
   */
  locale?: string;
}>();

const actualLocale = locale || xwikiMeta.locale;

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

const docRef = documentReference
  ? (modelReferenceParser?.parse(
      `doc:${documentReference}`,
    ) as DocumentReference)
  : undefined;
if (docRef && docRef.locale === undefined) {
  docRef.locale = actualLocale;
}

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
  );
};

const syntaxes = container.getAll<SyntaxConfig>(
  SYNTAX_CONFIG_COMPONENT_GROUP_NAME,
);

const syntax =
  syntaxes.find((conf) => conf.id === outputSyntax) ??
  syntaxes.find((conf) => conf.id === MINIMAL_SYNTAX_NAME);

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
  },
  theme: "light",
  lang: (actualLocale || "en") as EditorLanguage,
  label: defaultLabel,
  overrides: {
    imageEdition,
    linkEdition: {
      // Runs before the popover is opened to edit an existing link. The XWiki resource reference is
      // kept as link metadata (outside the BlockNote schema), mapped to a synthetic id stored in the
      // link url, so we resolve it here and inject it into the link data to pre-fill the popover. The
      // synthetic id is stripped from the url shown in the popover; beforeUpdate recovers it from the
      // previous link data.
      beforeEdit: (linkData) => {
        const id = extractLinkId(linkData.url);
        const reference = id
          ? (blockNoteDocument?.getMetadata(id)?.xwikiReference as
              | ResourceReference
              | undefined)
          : undefined;
        return { ...linkData, url: stripLinkId(linkData.url), reference };
      },
      // Runs right before the link is written into the content (i.e. before createLink / editLink).
      // We store the (possibly updated) resource reference as link metadata, mapped to a synthetic id
      // that we carry in the link url. When editing, we reuse the id of the edited link (taken from
      // the previous link data) so that the other link metadata (parameters, freestanding flag) is
      // preserved even when the target changes; when creating, we mint a new id.
      beforeUpdate: (linkData, previous) => {
        const id = extractLinkId(previous?.url ?? "") ?? uuidv4();
        if (linkData.reference) {
          blockNoteDocument!.getMetadata(id, true)!.xwikiReference =
            linkData.reference;
          return {
            ...linkData,
            url: injectLinkId(stripLinkId(linkData.url), id),
          };
        }
        return linkData;
      },
    },
  },
  syntax,
});

// This is passed to the BlockNote editor component.
const collaboration: Ref<Collaboration | undefined> = ref(undefined);
let collaborationManager: CollaborationManager | undefined = undefined;
const collaborationKey: Ref<string | undefined> = ref();

onBeforeMount(async () => {
  blockNoteDocument = blockNoteProcessor.load(initialValue);
  editorContent.value = blockNoteDocument.content;

  editorProps.value.label =
    (await resolver.resolve(["platform.blocknote.editor.label"])).translations[
      "platform.blocknote.editor.label"
    ] ?? defaultLabel;

  if (collaborationURL && docRef) {
    const cristalApp = container.get<CristalApp>("CristalApp");
    cristalApp.getWikiConfig().realtimeURL = collaborationURL;

    collaborationManager = container
      .get<CollaborationManagerProvider>(collaborationManagerProviderName)
      .get();
    // Join the realtime collaboration session for the specified XWiki document.
    collaboration.value = await collaborationManager.join(docRef);
    collaborationKey.value = `${encodeURIComponent(documentReference)}/${encodeURIComponent(actualLocale)}`;
  }

  isLoading.value = false;
});

onUnmounted(() => {
  collaborationManager?.leave();
});

// This is passed to the BlockNote editor component. Macros are inserted / edited directly as the server-rendered
// xwikiMacroBlock / xwikiInlineMacro blocks: the wizard operates on macro invocations, and the editor stores the
// resulting invocation in the block. The list of client-rendered macros is left empty (they are not used here).
const macros: BlockNoteViewWrapperProps["macros"] = {
  ctx: {
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
        console.error("Failed to edit the macro", error);
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
        console.error("Failed to insert the macro", error);
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
</style>
