<script setup lang="ts">
import { NodeViewWrapper } from "@tiptap/vue-3";
import { CristalApp } from "@xwiki/cristal-api";
import { AttachmentsService } from "@xwiki/cristal-attachments-api";
import { DocumentService } from "@xwiki/cristal-document-api";
import {
  Link,
  LinkSuggestServiceProvider,
  LinkType,
} from "@xwiki/cristal-link-suggest-api";
import {
  AttachmentReference,
  DocumentReference,
} from "@xwiki/cristal-model-api";
import { ModelReferenceParserProvider } from "@xwiki/cristal-model-reference-api";
import { RemoteURLSerializerProvider } from "@xwiki/cristal-model-remote-url-api";
import { LinkSuggestItem } from "@xwiki/cristal-tiptap-link-suggest-ui";
import { debounce } from "lodash";
import { Ref, inject, ref, useTemplateRef, watch } from "vue";
import { Tippy } from "vue-tippy";
import type { NodeViewProps } from "@tiptap/vue-3";
import "@tiptap/extension-image";

const cristal = inject<CristalApp>("cristal")!;
const attachmentsService = cristal
  .getContainer()
  .get<AttachmentsService>("AttachmentsService");
const modelReferenceParser = cristal
  .getContainer()
  .get<ModelReferenceParserProvider>("ModelReferenceParserProvider")
  .get();
const remoteURLSerializer = cristal
  .getContainer()
  .get<RemoteURLSerializerProvider>("RemoteURLSerializerProvider")
  .get();
const documentService = cristal
  .getContainer()
  .get<DocumentService>("DocumentService")!;

const loading = attachmentsService.isLoading();

const imageNameQueryInput = useTemplateRef<HTMLInputElement>(
  "imageNameQueryInput",
);

const fileUpload = useTemplateRef<HTMLInputElement>("fileUpload");
const newImage = useTemplateRef<HTMLElement>("newImage");

const { editor, getPos } = defineProps<NodeViewProps>();

function insertImage(src: string) {
  // Replace the current placeholder with the selected image
  editor
    .chain()
    .setNodeSelection(getPos())
    .command(({ commands }) => {
      const nodes = editor.state.schema.nodes;
      const type = nodes.paragraph;
      commands.insertContent({
        type: type.name,
        content: [
          {
            type: nodes.image.name,
            attrs: { src },
          },
        ],
      });
      return true;
    })
    .run();
}

const imageNameQuery = defineModel<string>("imageNameQuery");

const links: Ref<Link[]> = ref([]);
const linksSearchError: Ref<string | undefined> = ref(undefined);
const linksSearchLoading: Ref<boolean> = ref(false);

const linkSuggestServiceProvider = cristal
  .getContainer()
  .get<LinkSuggestServiceProvider>("LinkSuggestServiceProvider");
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
  }, 500),
);
// Start a first empty search on the first load, to not let the content empty.
searchAttachments("");

function insertTextAsLink() {
  if (imageNameQuery.value) {
    insertImage(imageNameQuery.value);
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
      insertImage(src);
    }
  }
}
</script>

<template>
  <node-view-wrapper>
    <tippy
      :interactive="true"
      :show-on-create="true"
      :allow-h-t-m-l="true"
      :hide-on-click="false"
      :delay="500"
    >
      <template #default>
        <div ref="newImage" class="image-insert-view">
          Upload or select and attachment.
        </div>
      </template>

      <template #content>
        <div class="image-insert-view-content no-drag-handle">
          <div v-if="loading">Loading...</div>
          <ul v-else>
            <li class="item">
              <x-btn @click="triggerUpload">Upload</x-btn>
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
                placeholder="Image name or image URL"
                @keydown.enter="insertTextAsLink"
              />
            </li>
            <li v-if="linksSearchLoading" class="item">Loading...</li>
            <li v-else-if="linksSearchError" class="item">
              {{ linksSearchError }}
            </li>
            <li v-else-if="links.length == 0 && imageNameQuery" class="item">
              {{ links }}
            </li>
            <template v-else>
              <!-- factorize with c-tiptap-link-suggest -->
              <li
                v-for="link in links"
                :key="link.id"
                :class="['item', 'selectable-item']"
                @keydown.enter="insertImage(link.url)"
                @click="insertImage(link.url)"
              >
                <link-suggest-item
                  :link="convertLink(link)"
                ></link-suggest-item>
              </li>
            </template>
          </ul>
        </div>
      </template>
    </tippy>
  </node-view-wrapper>
</template>

<style scoped>
.image-insert-view {
  background-color: var(--cr-color-neutral-100);
  border-radius: var(--cr-border-radius-large);
  border: solid var(--sl-input-border-width) var(--sl-input-border-color);
  padding: var(--cr-spacing-x-small) var(--cr-spacing-x-small);
}

.image-insert-view-content {
  padding: 0;
  position: relative;
  border-radius: var(--cr-tooltip-border-radius);
  background: white;
  overflow: hidden auto;
  box-shadow:
    0 0 0 1px rgba(0, 0, 0, 0.1),
    0 10px 20px rgba(0, 0, 0, 0.1);
  max-height: 300px;
  width: 300px;
}

.image-insert-view-content input {
  width: 100%;
}

.image-insert-view-content ul {
  list-style: none;
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
