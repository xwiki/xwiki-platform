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
import { ref, useTemplateRef } from "vue";
import type { Collaboration } from "@xwiki/platform-collaboration-api";
import type {
  BlockNoteViewWrapperProps,
  BlockType,
  ContextForMacros,
  LinkEditionData,
  LinkEditionHooks,
} from "@xwiki/platform-editors-blocknote-react";
import type { LinkContext } from "@xwiki/platform-link-type-api";
import type { MacroWithUnknownParamsType } from "@xwiki/platform-macros-api";

type ResourceReference = NonNullable<LinkEditionData["reference"]>;

const props = defineProps<{
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

  /**
   * The references of the linked resources, indexed by link URL. The editor provides them to the link modal when
   * editing the corresponding links, like the XWiki integration does from the link metadata.
   */
  linkReferences?: Record<string, ResourceReference>;

  excludedLinkTargetTypes?: string[];

  linkContext?: LinkContext;
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

// The reference of the last submitted link, rendered so that the tests can check it.
const lastSubmittedLinkReference = ref<ResourceReference>();

const linkEdition: LinkEditionHooks = {
  beforeEdit: (linkData) => {
    const reference = props.linkReferences?.[linkData.url];
    return reference ? { ...linkData, reference } : linkData;
  },
  beforeUpdate: (linkData) => {
    lastSubmittedLinkReference.value = linkData.reference;
    return linkData;
  },
};

const editorPropsWithLinkEdition = {
  ...props.editorProps,
  overrides: { ...props.editorProps.overrides, linkEdition },
};

const innerRef = useTemplateRef<InstanceType<typeof CBlockNoteView>>("inner");

defineExpose({
  getContent: (): BlockType[] => innerRef.value!.getContent(),
});
</script>

<template>
  <div>
    <CBlockNoteView
      ref="inner"
      :editor-props="editorPropsWithLinkEdition"
      :macros
      :editor-content
      :collaboration
      :deps-container="depsContainer"
      :excluded-link-target-types="excludedLinkTargetTypes"
      :link-context
      @instant-change="$emit('instant-change')"
      @debounced-change="(content) => $emit('debounced-change', content)"
    />
    <!-- Rendered only once a link with a reference is submitted, so that it doesn't change the text of the component
         checked by the other tests. -->
    <output
      v-if="lastSubmittedLinkReference"
      data-test="submittedLinkReference"
      >{{ JSON.stringify(lastSubmittedLinkReference) }}</output
    >
  </div>
</template>
