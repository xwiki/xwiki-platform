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
<script lang="ts" setup>
import CArticle from "./c-article.vue";
import { ContentTools } from "./contentTools";
import messages from "../translations";
import { PageData } from "@xwiki/cristal-api";
import { name as documentServiceName } from "@xwiki/cristal-document-api";
import { CIcon, Size } from "@xwiki/cristal-icons";
import { PageActions } from "@xwiki/cristal-page-actions-ui";
import { computed, inject, onUpdated, ref } from "vue";
import { useI18n } from "vue-i18n";
import type { CristalApp } from "@xwiki/cristal-api";
import type { DocumentService } from "@xwiki/cristal-document-api";
import type { MarkdownRenderer } from "@xwiki/cristal-markdown-api";
import type { ComputedRef, Ref } from "vue";

const { t } = useI18n({
  messages,
});

const cristal: CristalApp = inject<CristalApp>("cristal")!;
const container = cristal.getContainer();
const documentService = container.get<DocumentService>(documentServiceName);
const markdownRenderer = container.get<MarkdownRenderer>("MarkdownRenderer");

const loading = documentService.isLoading();
const error: Ref<Error | undefined> = documentService.getError();
const currentPage: Ref<PageData | undefined> =
  documentService.getCurrentDocument();
const currentPageRevision: Ref<string | undefined> =
  documentService.getCurrentDocumentRevision();
const currentPageName = documentService.getCurrentDocumentReferenceString();

const contentRoot = ref(undefined);

const content: ComputedRef<string | undefined> = computed(() => {
  if (currentPage.value) {
    const cpn: PageData = currentPage.value;
    if (cpn.html && cpn.html.trim() !== "") {
      return cpn.html as string;
    } else if (cpn.source) {
      return markdownRenderer.render(cpn.source);
    } else {
      return "";
    }
  } else {
    return undefined;
  }
});

const title = computed(() => {
  return (
    currentPage.value?.document?.get("headline") || currentPage.value?.name
  );
});

const pageExist = computed(() => {
  return content.value !== undefined;
});

onUpdated(() => {
  ContentTools.transformImages(cristal, "xwikicontent");

  if (cristal && contentRoot.value) {
    ContentTools.listenToClicks(contentRoot.value, cristal);
    ContentTools.transformMacros(contentRoot.value, cristal);
  }
});
</script>
<template>
  <c-article
    :loading="loading"
    :error="error"
    :current-page="currentPage"
    :page-exist="pageExist"
    before-u-i-x-p-id="content.before"
    after-u-i-x-p-id="content.after"
  >
    <template #title>
      <h1 class="doc-title">{{ title }}</h1>
    </template>
    <template #doc-page-actions>
      <div class="doc-page-actions">
        <router-link
          :to="
            currentPageRevision
              ? ''
              : { name: 'edit', params: { page: currentPageName } }
          "
        >
          <x-btn size="small" :disabled="currentPageRevision !== undefined">
            <c-icon name="pencil" :size="Size.Small"></c-icon>
            Edit
          </x-btn>
        </router-link>
        <page-actions
          :current-page="currentPage"
          :current-page-name="currentPageName ?? ''"
          :disabled="currentPageRevision !== undefined"
        ></page-actions>
      </div>
    </template>
    <template #doc-header-alerts>
      <!-- Indicate that the page displayed is not the current version. -->
      <x-alert v-if="currentPageRevision !== undefined" type="warning">
        {{
          t("history.alert.content", {
            revision: currentPageRevision,
            pageName: currentPageName,
          })
        }}
        <router-link :to="{ name: 'view', params: { page: currentPageName } }"
          >{{ t("history.alert.link.label") }}
        </router-link>
      </x-alert>
    </template>
    <template #default>
      <!-- eslint-disable vue/no-v-html -->
      <div
        v-if="pageExist"
        id="xwikicontent"
        ref="contentRoot"
        class="doc-content"
        v-html="content"
      ></div>
      <div v-else class="doc-content unknown-page">
        <p>
          The requested page could not be found. You can edit the page to create
          it.
        </p>
      </div>
    </template>
  </c-article>
</template>
<style scoped>
.doc-content {
  padding: 0;
}

:global(.doc-content img) {
  max-width: 100%;
  height: auto;
}

.doc-title {
  grid-area: doc-title;
  margin: 0;
  font-size: var(--cr-font-size-2x-large);
  line-height: var(--cr-font-size-2x-large);
  padding-block-start: var(--cr-spacing-small);
}

.doc-page-actions {
  display: flex;
  flex-wrap: wrap;
  flex-flow: row;
  align-items: center;
  gap: var(--cr-spacing-2x-small);
}

/*---*/
</style>
