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
import LinkSuggestList from "./LinkSuggestList.vue";
import { LinkEditionContext } from "../../components/linkEditionContext";
import { LinkSuggestion } from "../../components/linkSuggest";
import messages from "../../translations";
import { DocumentService } from "@xwiki/cristal-document-api";
import { LinkType } from "@xwiki/cristal-link-suggest-api";
import {
  AttachmentReference,
  DocumentReference,
} from "@xwiki/cristal-model-api";
import { Container } from "inversify";
import { debounce } from "lodash-es";
import { inject, onMounted, ref, shallowRef, useTemplateRef, watch } from "vue";
import { useI18n } from "vue-i18n";
import type { AttachmentsService } from "@xwiki/cristal-attachments-api";
import type {
  Link,
  LinkSuggestServiceProvider,
} from "@xwiki/cristal-link-suggest-api";

const { linkEditionCtx } = defineProps<{
  linkEditionCtx: LinkEditionContext;
}>();

const emit = defineEmits<{
  select: [{ url: string }];
}>();

const container = inject<Container>("container")!;

const attachmentsService =
  container.get<AttachmentsService>("AttachmentsService");

const documentService = container.get<DocumentService>("DocumentService")!;

const { t } = useI18n({
  messages,
});

const loading = attachmentsService.isLoading();

const imageNameQueryInput = useTemplateRef<HTMLInputElement>(
  "imageNameQueryInput",
);

const imageNameQuery = defineModel<string>("imageNameQuery");

const links = ref<Link[]>([]);
const linksSearchError = ref<string | undefined>(undefined);
const linksSearchLoading = ref(false);

const linkSuggestServiceProvider = container.get<LinkSuggestServiceProvider>(
  "LinkSuggestServiceProvider",
);
const linkSuggestService = linkSuggestServiceProvider.get()!;

async function searchAttachments(query: string) {
  if (linkSuggestService) {
    links.value = await linkSuggestService.getLinks(
      query,
      LinkType.ATTACHMENT,
      "image/*",
    );
  }
}

watch(
  imageNameQuery,
  debounce(async () => {
    if (imageNameQuery.value && imageNameQuery.value.length) {
      const query = imageNameQuery.value;
      await searchAttachments(query);
    }
  }),
);

// Start a first empty search on the first load, to not let the content empty.
searchAttachments("");

const fileUpload = useTemplateRef<HTMLInputElement>("fileUpload");

function triggerUpload() {
  fileUpload.value?.click();
}

function getCurrentPageName() {
  return documentService.getCurrentDocumentReferenceString().value ?? "";
}

async function fileSelected() {
  const files = fileUpload.value?.files;
  if (files && files.length > 0) {
    const fileItem = files.item(0)!;
    const currentPageName = getCurrentPageName();
    await attachmentsService.upload(currentPageName, [fileItem]);

    const parser = linkEditionCtx.modelReferenceParser?.parse(currentPageName);

    const url = linkEditionCtx.remoteURLSerializer?.serialize(
      new AttachmentReference(fileItem.name, parser as DocumentReference),
    );

    if (url) {
      emit("select", { url: url });
    }
  }
}

function insertTextAsLink() {
  if (imageNameQuery.value) {
    emit("select", { url: imageNameQuery.value });
  }
}

function convertLink(link: Link): LinkSuggestion {
  const attachmentReference = linkEditionCtx.modelReferenceParser?.parse(
    link.reference,
  ) as AttachmentReference;

  const documentReference = attachmentReference.document;
  const segments = documentReference.space?.names.slice(0) ?? [];

  // TODO: replace with an actual construction of segments from a reference
  if (documentReference.terminal) {
    segments.push(documentReference.name);
  }

  return {
    type: link.type,
    title: link.label,
    reference: link.reference,
    url: link.url,
    segments,
  };
}

const listInstance = shallowRef<InstanceType<typeof LinkSuggestList>>();

onMounted(() => {
  imageNameQueryInput.value?.focus();
});
</script>

<template>
  <div class="image-insert-view">
    <div v-if="loading">
      {{ t("blocknote.image.insertView.loading") }}
    </div>

    <ul v-else class="item-group">
      <li class="item">
        <x-btn @click="triggerUpload">
          {{ t("blocknote.image.insertView.upload") }}
        </x-btn>

        <input
          v-show="false"
          ref="fileUpload"
          type="file"
          accept="image/*"
          @change="fileSelected"
        />
      </li>

      <li class="item">
        <input
          ref="imageNameQueryInput"
          v-model="imageNameQuery"
          type="text"
          :placeholder="t('blocknote.image.insertView.search.placeholder')"
          @keydown.up.prevent="listInstance?.focusRelative(-1)"
          @keydown.down.prevent="listInstance?.focusRelative(1)"
          @keydown.enter="
            listInstance ? listInstance.select() : insertTextAsLink()
          "
        />
      </li>

      <li v-if="linksSearchLoading" class="item">
        {{ t("blocknote.image.insertView.loading") }}
      </li>

      <li v-else-if="linksSearchError" class="item">
        {{ linksSearchError }}
      </li>

      <li v-else-if="links.length == 0 && imageNameQuery" class="item">
        {{ t("blocknote.image.insertView.noResults") }}
      </li>

      <template v-else>
        <LinkSuggestList
          ref="listInstance"
          images
          :links="links.map(convertLink)"
          @select="(link) => $emit('select', { url: link.url })"
        />
      </template>
    </ul>
  </div>
</template>

<style scoped>
.image-insert-view {
  padding: var(--cr-spacing-x-small) var(--cr-spacing-x-small);
  position: relative;
  border-radius: var(--cr-tooltip-border-radius);
  overflow: hidden auto;
  box-shadow:
    0 0 0 1px rgba(0, 0, 0, 0.1),
    0 10px 20px rgba(0, 0, 0, 0.1);
  max-height: 300px;
  width: auto;
}

ul {
  list-style: none;
}

.item-group {
  overflow: auto;
  padding: 0;
}

.item {
  display: block;
  background: transparent;
  border: none;
  padding: var(--cr-spacing-x-small);
  width: 100%;
  text-align: start;
}

input {
  outline: none;
  border: 1px solid lightgray;
  width: 100%;
}
</style>
