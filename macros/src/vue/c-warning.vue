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
  <x-alert type="warning" :title="title" :description="content">
    <!-- eslint-disable vue/no-v-html -->
    <div class="renderedContent" v-html="html"></div>
  </x-alert>
</template>
<script lang="ts">
import type { Logger, CristalApp } from "@xwiki/cristal-api";
import type { PropType } from "vue";
import { inject } from "vue";
import { ContentTools } from "@xwiki/cristal-skin";
import type { MacroData } from "@xwiki/cristal-skin";

let logger: Logger | null = null;
let addedHTMLField: Array<string> = [];

export default {
  props: {
    macroData: { type: Object as PropType<MacroData>, required: true },
  },
  async setup(props: { macroData: MacroData }) {
    const cristal = inject<CristalApp>("cristal");
    if (cristal != undefined && logger == null) {
      logger = cristal.getLogger("macro.warning");
      logger?.debug("In warning macro");
    } else {
      console.log("Cannot initialize logger in skin.vue.field");
    }

    let warningMacroData = props.macroData;
    let htmlFieldValue = await cristal?.renderContent(
      warningMacroData.getMacroContent().toString(),
      "xwiki",
      "html",
      cristal?.getWikiConfig(),
    );
    return {
      title: warningMacroData.getMacroParameter("title"),
      content: warningMacroData.getMacroContent(),
      html: htmlFieldValue,
    };
  },
  mounted() {
    logger?.debug("In warning mounted");
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
};
</script>
