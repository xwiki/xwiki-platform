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
<script setup lang="ts">
import CDocumentRefresher from "./c-document-refresher.vue";
import { inject, markRaw } from "vue";
import type { CristalApp } from "@xwiki/cristal-api";

const cristal = inject<CristalApp>("cristal")!;

const comps = await cristal.getUIXTemplates("editor");
const logger = cristal.getLogger("skin.vue.editor");

logger?.debug("Editor UIX components are ", comps);

const editor = cristal.getWikiConfig().editor;

const editComponent =
  comps.find(
    (item) =>
      // TODO: fix unsafe access to editorname
      (item as { editorname: string }).editorname === (editor ?? "tiptap"),
  ) ?? comps[0];

logger?.debug("Final component ", editComponent);

const component = markRaw(editComponent);
</script>

<template>
  <!-- When the user is on an edit page with the editor displayed, and a new page is created, the editor is not refreshed.
       Due lots of state variables in the editor component, it is arguably preferable to refresh the entire component to make it aware of the new document.

      Ths <CDocumentRefresher> component helps with that: the c-edit component, which is in charge of displaying the selected editor, watches the current document.
      When it changes, the underlying editor component is un-mounted then re-mounted using Vue's builtin key mechanism, which forces re-mounting.
  -->
  <CDocumentRefresher>
    <component :is="component" />
  </CDocumentRefresher>
</template>
