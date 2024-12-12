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
<script lang="ts">
import { CristalApp, Logger } from "@xwiki/cristal-api";
import { inject, markRaw } from "vue";
import type { Component } from "vue";

let comps: Array<Component>;
let logger: Logger;

export default {
  setup() {
    let cristal = inject<CristalApp>("cristal");
    if (cristal) {
      comps = cristal.getUIXTemplates("editor");
      logger = cristal.getLogger("skin.vue.editor");
    }
  },
  data() {
    logger?.debug("Editor UIX components are ", comps);
    if (!comps || comps.length == 0) {
      return {};
    } else {
      let editComponent = null;
      logger?.debug("Using first editor UIX component ", comps);
      if (comps != null) {
        for (const item of comps) {
          // TODO: fix unsafe access to editorname
          // TODO: the editor should be drawn from the configuration
          // TODO: also, we shouldn't load all the editors when initializing the
          // components manager, but instead load them lazily, or only load the
          // ones allowed by the configuration (e.g., one for wysiwyg, and one
          // for plain syntax edit).
          if ((item as { editorname: string }).editorname === "editortiptap") {
            editComponent = item;
            break;
          }
        }
      }
      if (editComponent == null) {
        editComponent = comps[0];
      }
      logger?.debug("Final component ", editComponent);
      return {
        component: markRaw(editComponent),
      };
    }
  },
};
</script>
<template>
  <component :is="component" />
</template>
<style></style>
