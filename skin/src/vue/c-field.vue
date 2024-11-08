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
<template>
  <template v-if="type == 'html'">
    <!-- eslint-disable vue/no-v-html -->
    <div :id="'field-' + name" class="renderedContent" v-html="html" />
  </template>
  <template v-else>
    <template v-if="mode == 'edit'">
      <!-- eslint-disable vue/no-v-html -->
      <span v-html="editField" />
    </template>
    <template v-else>
      {{ value }}
    </template>
  </template>
</template>
<script lang="ts">
import { ContentTools } from "./contentTools";
import { defineComponent, inject, ref } from "vue";
import type { CristalApp, Document, Logger, Storage } from "@xwiki/cristal-api";
import type { PropType, Ref } from "vue";

let logger: Logger | null = null;
let editFieldMap: Map<string, Ref> = new Map<string, Ref>();
let addedHTMLField: Array<string> = [];

export default defineComponent({
  props: {
    name: { type: String, required: true },
    document: { type: Object as PropType<Document>, required: true },
    mode: { type: String, required: true },
    type: { type: String, required: false, default: () => undefined },
  },
  setup(props) {
    const cristal = inject<CristalApp>("cristal");
    if (cristal != null && logger == null) {
      logger = cristal.getLogger("skin.vue.field");
      logger?.debug("In field template");
    } else {
      console.log("Cannot initialize logger in skin.vue.field");
    }
    const document: Document = props.document;
    const value = document ? document.get(props.name) : "";
    let htmlFieldValue: string | undefined = "";
    let editFieldValue = ref("");
    editFieldMap.set(props.name, editFieldValue);
    if (props.mode == "edit") {
      let storage: Storage | undefined = cristal?.getWikiConfig().storage;
      logger?.debug("Ready to get edit field", props.name);
      editFieldValue.value = "";
      storage?.getEditField(document, props.name).then(function (
        editField: string,
      ) {
        editFieldValue.value = editField;
      });
    } else {
      if (props.type == "html") {
        addedHTMLField.push("field-" + props.name);
        if (props.name == "html") {
          htmlFieldValue = value;
        } else {
          htmlFieldValue = "";
        } // cristal?.renderContent(value, "xwiki", "html", cristal?.getWikiConfig());
      }
    }
    return { value: value, html: htmlFieldValue, editField: editFieldValue };
  },
  mounted() {
    logger?.debug("In field mounted");
    const cristal = inject<CristalApp>("cristal");
    addedHTMLField.forEach((fieldName) => {
      logger?.debug("Transform image", fieldName);
      ContentTools.transformImages(cristal, fieldName);
    });
  },
  updated() {
    logger?.debug("In field updated");
    const cristal = inject<CristalApp>("cristal");
    addedHTMLField.forEach((fieldName) => {
      logger?.debug("Transform image", fieldName);
      ContentTools.transformImages(cristal, fieldName);
    });
  },
});
</script>
