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
import LinkEditor from "./LinkEditor.vue";
import ToolbarButtonSet from "./ToolbarButtonSet.vue";
import { BlockOfType, EditorType } from "../../blocknote";
import { LinkEditionContext } from "../../components/linkEditionContext";
import { ref } from "vue";

const { editor, linkEditionCtx } = defineProps<{
  editor: EditorType;
  currentBlock: BlockOfType<"paragraph">;
  linkEditionCtx: LinkEditionContext;
}>();

const showLinkEditor = ref(false);

const selected = editor.getSelectedText();

function insertLink(url: string) {
  editor.createLink(url);
}
</script>

<template>
  <ToolbarButtonSet
    :buttons="[
      {
        icon: 'link',
        title: 'Create link',
        onClick() {
          showLinkEditor = !showLinkEditor;
        },
      },
    ]"
  />

  <div v-if="showLinkEditor" class="linkEditor">
    <LinkEditor
      :link-edition-ctx
      :current="{ title: selected, reference: null, url: '' }"
      hide-title
      @update="({ url }) => insertLink(url)"
    />
  </div>
</template>

<style scoped>
/*
  NOTE: Popover is implemented manually here due to an unresolved bug in the library we'd like to use
  Once that issue is resolved, this code block will be removed and the library will be used instead
  Consider this a temporary "dirty" hack
*/
.linkEditor {
  position: absolute;
  left: 0;
  /* Yes, this is dirty */
  top: 2.5rem;
  background: white;
  width: 100%;
  box-shadow: 0px 4px 12px #cfcfcf;
  border-radius: 6px;
}
</style>
