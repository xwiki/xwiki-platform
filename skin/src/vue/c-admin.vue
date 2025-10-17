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
import messages from "../translations";
import { name as documentServiceName } from "@xwiki/cristal-document-api";
import { inject, ref, watch } from "vue";
import { useI18n } from "vue-i18n";
import type { CristalApp } from "@xwiki/cristal-api";
import type { DocumentService } from "@xwiki/cristal-document-api";
import type {
  UIExtension,
  UIExtensionsManager,
} from "@xwiki/cristal-uiextension-api";
import type { Component, Ref } from "vue";

const cristal: CristalApp = inject<CristalApp>("cristal")!;
const container = cristal.getContainer();

const documentService = container.get<DocumentService>(documentServiceName);
const uixManager: UIExtensionsManager = container.get<UIExtensionsManager>(
  "UIExtensionsManager",
)!;

const currentPageName = documentService.getCurrentDocumentReferenceString();
const currentUIExtension: Ref<UIExtension | undefined> = ref(undefined);
const currentComponent: Ref<Component | undefined> = ref(undefined);

const { t } = useI18n({
  messages,
});

watch(
  currentPageName,
  async () => {
    currentUIExtension.value = (
      await uixManager.list("settings.categories")
    ).find((uix) => uix.id == currentPageName.value);
    currentComponent.value = await currentUIExtension.value?.component();
  },
  { immediate: true },
);
</script>
<template>
  <article class="content">
    <div class="doc-content">
      <div v-if="currentComponent !== undefined">
        <h1 class="doc-title">{{ currentUIExtension?.parameters["title"] }}</h1>
        <component :is="currentComponent"></component>
      </div>
      <div v-else>
        <h1 class="doc-title">{{ t("admin.home.title") }}</h1>
      </div>
    </div>
  </article>
</template>
<style scoped>
.content {
  display: grid;
  gap: var(--cr-spacing-small);
  scrollbar-gutter: stable;
  height: 100%;
  padding: 0 var(--cr-spacing-2x-large);
  overflow: auto;
}

.doc-title {
  font-size: var(--cr-font-size-2x-large);
  font-weight: var(--cr-font-weight-bold);
}

.doc-content {
  max-width: var(--cr-sizes-max-page-width);
  width: 100%;
  justify-self: center;
}

@container xwCristal (max-width: 600px) {
  .content {
    padding-left: 0 var(--cr-spacing-x-small);
  }
}
</style>
