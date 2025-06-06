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
        :skin-manager
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

<script>
import { BlocknoteEditor } from "@xwiki/cristal-editors-blocknote-headless";
import { MarkdownToUniAstConverter, UniAstToMarkdownConverter, createConverterContext } from "@xwiki/cristal-uniast";

export default {
  name: "XWikiBlockNote",

  inject: ["logic", "container"],

  components: {
    BlocknoteEditor,
  },

  props: {
    // The key used to submit the edited content.
    name: {
      type: String,
      default: null,
    },

    // The initial content when the editor is created.
    initialValue: {
      type: String,
      default: "",
    },

    // The ID of the form this editor is associated with.
    form: {
      type: String,
      default: null,
    },

    // Prevent the edited content and the conversion metadata from being submitted.
    disabled: Boolean,

    // The syntax of the edited content, as expected by the editor.
    inputSyntax: {
      type: String,
      default: "markdown/1.2",
    },

    // The syntax of the edited content, as expected by the back-end storage.
    outputSyntax: {
      type: String,
      default: "xwiki/2.1",
    },
  },

  data() {
    const converterContext = createConverterContext(this.container);
    const markdownToUniAst = new MarkdownToUniAstConverter(converterContext);
    const uniAstToMarkdown = new UniAstToMarkdownConverter(converterContext);
    const editorContent = markdownToUniAst.parseMarkdown(this.initialValue);

    return {
      dirty: false,
      value: this.initialValue,
      editorProps: {
        theme: "light",
      },
      editorContent,
      uniAstToMarkdown,
    };
  },

  computed: {
    editor() {
      return this.$refs.editor;
    },
    skinManager() {
      return this.container.get("SkinManager");
    },
    realtimeServerURL() {
      return this.logic.realtimeServerURL;
    },
  },

  methods: {
    updateValue(editorContent) {
      if (!this.dirty) {
        // The value is already up-to-date.
        return this.value;
      }

      const instantUpdate = !editorContent;
      editorContent = editorContent || this.editor.getContent();

      const value = this.uniAstToMarkdown.toMarkdown(editorContent);
      if (value instanceof Error) {
        throw error;
      }

      this.value = value;
      this.dirty = false;

      if (instantUpdate) {
        // Update the value input immediately. This is important for instance when the form containing the BlockNote
        // editor is submitted. Alternatively, we have to delay the form submission until the next tick when Vue will
        // have updated the value input, but that is more complex.
        this.$refs.valueInput.value = value;
      }

      return this.value;
    },
  },
};
</script>

<style>
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
