<!--
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
-->
<template>
  <div class="xwiki-blocknote">
    <suspense>
      <BlocknoteEditor
        ref="editor"
        :editor-props
        :editor-content
        :container
        :realtime-server-u-r-l
        @instant-change="dirty = true"
        @debounced-change="updateValue"
      ></BlocknoteEditor>
    </suspense>
    <input v-if="name" ref="valueInput" type="hidden" :name :value :form :disabled />
    <input v-if="name" type="hidden" name="RequiresConversion" :value="name" :form :disabled />
    <input v-if="name" type="hidden" :name="name + '_inputSyntax'" :value="inputSyntax" :form :disabled />
    <input v-if="name" type="hidden" :name="name + '_outputSyntax'" :value="outputSyntax" :form :disabled />
  </div>
</template>

<script setup lang="ts">
import { BlocknoteEditor } from "@xwiki/cristal-editors-blocknote-headless";
import { EditorLanguage } from "@xwiki/cristal-editors-blocknote-react";
import {
  MarkdownToUniAstConverter,
  UniAst,
  UniAstToMarkdownConverter,
  createConverterContext,
} from "@xwiki/cristal-uniast";
import { Container } from "inversify";
import { computed, inject, ref, shallowRef, useTemplateRef } from "vue";
import { Logic } from "../services/Logic";

//
// Injected
//
const logic = inject<Logic>("logic")!;
const container = inject<Container>("container")!;

//
// Props
//
const {
  name = undefined,
  initialValue = "",
  form = undefined,
  disabled = false,
  inputSyntax = "markdown/1.2",
  outputSyntax = "xwiki/2.1",
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
}>();

//
// Data
//
const value = ref(initialValue);
const dirty = ref(false);

const converterContext = createConverterContext(container);
const markdownToUniAst = new MarkdownToUniAstConverter(converterContext);
const uniAstToMarkdown = new UniAstToMarkdownConverter(converterContext);

const editorContent = shallowRef<UniAst | Error>(markdownToUniAst.parseMarkdown(initialValue));
const editorProps = shallowRef<InstanceType<typeof BlocknoteEditor>["$props"]["editorProps"]>({
  blockNoteOptions: {
    defaultStyles: true,
  },
  theme: "light",
  lang: getLanguage(),
});

//
// Computed
//
const valueInput = useTemplateRef<HTMLInputElement>("valueInput");
const editorInstance = useTemplateRef<InstanceType<typeof BlocknoteEditor>>("editor");
const realtimeServerURL = computed(() => {
  return logic.realtimeServerURL;
});

//
// Methods
//
function updateValue(editorContent?: UniAst | Error): string {
  if (!dirty.value) {
    // The value is already up-to-date.
    return value.value;
  }

  const instantUpdate = !editorContent;
  editorContent = editorContent || editorInstance.value?.getContent();
  if (!editorContent || editorContent instanceof Error) {
    throw editorContent || new Error("Could not get the editor content.");
  }

  const newValue = uniAstToMarkdown.toMarkdown(editorContent as UniAst);
  if (newValue instanceof Error) {
    throw newValue;
  }

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

function getLanguage(): EditorLanguage {
  return (document.documentElement.lang || "en") as EditorLanguage;
}

defineExpose({
  updateValue,
});
</script>

<style>
.xwiki-blocknote {
  --cr-base-font-size: unset;
  --cr-base-text-color: unset;
  --cr-font-sans: unset;
  --cr-font-size-x-large: 24px; /* --font-size-h2 */
  --cr-font-size-large: 20px; /* --font-size-h3 */
  --cr-font-size-medium: 17px; /* --font-size-h4 */
  --cr-font-weight-normal: var(--font-weight-regular);
  --cr-line-height-normal: unset;

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
