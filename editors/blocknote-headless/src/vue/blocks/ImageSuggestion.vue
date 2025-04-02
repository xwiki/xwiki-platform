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
import LinkSuggestItem from "./LinkSuggestItem.vue";
import messages from "../../translations";
import { LinkType } from "@xwiki/cristal-link-suggest-api";
import { AttachmentReference } from "@xwiki/cristal-model-api";
import { Container } from "inversify";
import { debounce } from "lodash-es";
import { inject, ref, useTemplateRef, watch } from "vue";
import { useI18n } from "vue-i18n";
import type { AttachmentsService } from "@xwiki/cristal-attachments-api";
import type { DocumentService } from "@xwiki/cristal-document-api";
import type {
  Link,
  LinkSuggestServiceProvider,
} from "@xwiki/cristal-link-suggest-api";
import type { DocumentReference } from "@xwiki/cristal-model-api";
import type { ModelReferenceParserProvider } from "@xwiki/cristal-model-reference-api";
import type { RemoteURLSerializerProvider } from "@xwiki/cristal-model-remote-url-api";

const emit = defineEmits<{
  selected: [{ url: string }];
}>();

const container = inject<Container>("container")!;

const attachmentsService =
  container.get<AttachmentsService>("AttachmentsService");

const modelReferenceParser = container
  .get<ModelReferenceParserProvider>("ModelReferenceParserProvider")
  .get();

const remoteURLSerializer = container
  .get<RemoteURLSerializerProvider>("RemoteURLSerializerProvider")
  .get();

const documentService = container.get<DocumentService>("DocumentService")!;

const { t } = useI18n({
  messages,
});

const loading = attachmentsService.isLoading();

const imageNameQueryInput = useTemplateRef<HTMLInputElement>(
  "imageNameQueryInput",
);

const fileUpload = useTemplateRef<HTMLInputElement>("fileUpload");

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

function insertTextAsLink() {
  if (imageNameQuery.value) {
    emit("selected", { url: imageNameQuery.value });
  }
}

function convertLink(link: Link) {
  const attachmentReference = modelReferenceParser?.parse(
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
    segments,
    imageURL: remoteURLSerializer?.serialize(attachmentReference),
  };
}

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

    const parser = modelReferenceParser?.parse(currentPageName);

    const src = remoteURLSerializer?.serialize(
      new AttachmentReference(fileItem.name, parser as DocumentReference),
    );
    if (src) {
      emit("selected", { url: src });
    }
  }
}
</script>

<template>
  <div class="image-insert-view-content no-drag-handle">
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
          @keydown.enter="insertTextAsLink"
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
        <!-- factorize with c-blocknote-link-suggest -->
        <li
          v-for="link in links"
          :key="link.id"
          :class="['item', 'selectable-item']"
          @keydown.enter="$emit('selected', { url: link.url })"
          @click="$emit('selected', { url: link.url })"
        >
          <link-suggest-item :link="convertLink(link)"></link-suggest-item>
        </li>
      </template>
    </ul>
  </div>
</template>

<style scoped>
.image-insert-view {
  background-color: var(--cr-color-neutral-100);
  border-radius: var(--cr-border-radius-large);
  border: solid var(--sl-input-border-width) var(--sl-input-border-color);
  padding: var(--cr-spacing-x-small) var(--cr-spacing-x-small);
}

.image-insert-view-content {
  padding: var(--cr-spacing-x-small) var(--cr-spacing-x-small);
  position: relative;
  border-radius: var(--cr-tooltip-border-radius);
  background: white;
  overflow: hidden auto;
  box-shadow:
    0 0 0 1px rgba(0, 0, 0, 0.1),
    0 10px 20px rgba(0, 0, 0, 0.1);
  max-height: 300px;
  width: auto;
}

.image-insert-view-content input {
  width: 100%;
}

.image-insert-view-content ul {
  list-style: none;
}

.image-insert-view-content .item-group {
  overflow: auto;
  padding: 0;
}

.image-insert-view-content .item {
  display: block;
  background: transparent;
  border: none;
  padding: var(--cr-spacing-x-small);
  width: 100%;
  text-align: start;
}

.image-insert-view-content .selectable-item:hover {
  background-color: white;
}

.image-insert-view-content .selectable-item:hover {
  background-color: var(--cr-color-neutral-200);
  cursor: pointer;
}
</style>
