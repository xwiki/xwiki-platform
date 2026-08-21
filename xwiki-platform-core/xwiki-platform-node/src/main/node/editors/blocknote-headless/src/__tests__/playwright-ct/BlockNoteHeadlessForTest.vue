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
<script setup lang="ts">
import CBlockNoteView from "../../vue/c-blocknote-view.vue";
import { depsContainerMock } from "../depsContainer.mock";
import { useTemplateRef } from "vue";
import type { Collaboration } from "@xwiki/platform-collaboration-api";
import type {
  BlockNoteViewWrapperProps,
  BlockType,
  ContextForMacros,
} from "@xwiki/platform-editors-blocknote-react";
import type { MacroWithUnknownParamsType } from "@xwiki/platform-macros-api";

defineProps<{
  editorProps: Omit<
    BlockNoteViewWrapperProps,
    | "depsContainer"
    | "content"
    | "linkEditionHandler"
    | "macroAstToReactJsxConverter"
    | "macros"
  >;

  macros:
    | {
        list: MacroWithUnknownParamsType[];
        ctx: ContextForMacros;
      }
    | false;

  editorContent: BlockType[];

  collaboration?: Collaboration;
}>();

defineEmits<{
  "instant-change": [];
  "debounced-change": [content: BlockType[]];
}>();

// The mocked container must be created here, in the browser, rather than in
// the Playwright test/Node process: Playwright CT mount() props are
// serialized across the Node/browser boundary, so a mock full of closures
// passed as a prop would lose all its behavior once it reaches the browser.
const depsContainer = depsContainerMock();

const innerRef = useTemplateRef<InstanceType<typeof CBlockNoteView>>("inner");

defineExpose({
  getContent: (): BlockType[] => innerRef.value!.getContent(),
});
</script>

<template>
  <CBlockNoteView
    ref="inner"
    v-bind="$props"
    :deps-container="depsContainer"
    @instant-change="$emit('instant-change')"
    @debounced-change="(content) => $emit('debounced-change', content)"
  />
</template>
