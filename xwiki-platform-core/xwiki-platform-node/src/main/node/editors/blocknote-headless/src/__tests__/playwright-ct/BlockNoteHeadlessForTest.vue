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
import { serviceContainerMock } from "../serviceContainer.mock";
import { ref, useTemplateRef } from "vue";
import type { HeadlessEditorOptions } from "../../editorOptions";
import type {
  BlockType,
  LinkEditionData,
  LinkEditionHooks,
} from "@xwiki/platform-editors-blocknote-react";
import type { SyntaxConfig } from "@xwiki/platform-syntaxes-config";

type ResourceReference = NonNullable<LinkEditionData["reference"]>;

const props = defineProps<{
  // The options that the tests need to set, the others being the same for all the tests. The configuration of the
  // syntax of the edited content is registered in the container, the editor taking only its id.
  options: Omit<
    HeadlessEditorOptions,
    "serviceContainer" | "lang" | "label" | "syntax"
  > & { syntax: SyntaxConfig };

  content: BlockType[];

  /**
   * The references of the linked resources, indexed by link URL. The editor provides them to the link modal when
   * editing the corresponding links, like the XWiki integration does from the link metadata.
   */
  linkReferences?: Record<string, ResourceReference>;

  /**
   * The URLs of the links whose label is generated from their target. The editor tells the link modal when editing the
   * corresponding links, like the XWiki integration does from the link metadata.
   */
  linksWithGeneratedLabel?: string[];
}>();

defineEmits<{
  "instant-change": [];
  "debounced-change": [content: BlockType[]];
}>();

// The mocked container must be created here, in the browser, rather than in
// the Playwright test/Node process: Playwright CT mount() props are
// serialized across the Node/browser boundary, so a mock full of closures
// passed as a prop would lose all its behavior once it reaches the browser.
const serviceContainer = serviceContainerMock([props.options.syntax]);

// The reference of the last submitted link, and whether its label is generated, rendered so that the tests can check
// them.
const lastSubmittedLinkReference = ref<ResourceReference>();
const lastSubmittedLinkGeneratedTitle = ref<boolean>();

const linkEdition: LinkEditionHooks = {
  beforeEdit: (linkData) => {
    const reference = props.linkReferences?.[linkData.url];
    return {
      ...linkData,
      ...(reference ? { reference } : {}),
      generatedTitle: props.linksWithGeneratedLabel?.includes(linkData.url),
    };
  },
  beforeUpdate: (linkData) => {
    lastSubmittedLinkReference.value = linkData.reference;
    lastSubmittedLinkGeneratedTitle.value = !!linkData.generatedTitle;
    return linkData;
  },
};

const options: HeadlessEditorOptions = {
  ...props.options,
  syntax: props.options.syntax.id,
  serviceContainer,
  lang: "en",
  label: "Editor",
  features: {
    ...props.options.features,
    links: { ...props.options.features?.links, hooks: linkEdition },
  },
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
      :options
      :content
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
    <output
      v-if="lastSubmittedLinkGeneratedTitle !== undefined"
      data-test="submittedLinkGeneratedTitle"
      >{{ lastSubmittedLinkGeneratedTitle }}</output
    >
  </div>
</template>
