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
    <textarea :name :value :form :disabled @input="$emit('update:modelValue', $event.target.value)"></textarea>
    <suspense>
      <BlocknoteEditor
        ref="editor"
        :editor-props
        :editor-content
        :container
        :skin-manager
        :realtime-server-u-r-l
        @blocknote-save="updateModelValue"
      ></BlocknoteEditor>
    </suspense>
    <input v-if="name" type="hidden" name="RequiresConversion" :value="name" :form :disabled />
    <input v-if="name" type="hidden" :name="name + '_inputSyntax'" :value="inputSyntax" :form :disabled />
    <input v-if="name" type="hidden" :name="name + '_outputSyntax'" :value="outputSyntax" :form :disabled />
  </div>
</template>

<script>
import { BlocknoteEditor } from "@xwiki/cristal-editors-blocknote-headless";
import { MarkdownToUniAstConverter, createConverterContext } from "@xwiki/cristal-uniast";

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

    // The edited content.
    value: {
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
    const markdownConverter = new MarkdownToUniAstConverter(createConverterContext(this.container));
    const editorContent = markdownConverter.parseMarkdown(this.value);

    return {
      editorProps: {
        theme: "light",
      },
      editorContent,
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
    updateModelValue(value) {
      this.$emit("update:modelValue", value);
    },
  },

  emits: ["update:modelValue"],
};
</script>
