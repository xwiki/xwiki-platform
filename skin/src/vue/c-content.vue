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
import { ContentTools } from "./contentTools";
import xavatarImg from "../images/no-one.svg";
import messages from "../translations";
import { AlertsToasts } from "@xwiki/cristal-alerts-ui";
import { PageData } from "@xwiki/cristal-api";
import { name as documentServiceName } from "@xwiki/cristal-document-api";
import { ExtraTabs } from "@xwiki/cristal-extra-tabs-ui";
import { CIcon, Size } from "@xwiki/cristal-icons";
import { InfoActions } from "@xwiki/cristal-info-actions-ui";
import { PageActions } from "@xwiki/cristal-page-actions-ui";
import { UIExtensions } from "@xwiki/cristal-uiextension-ui";
import { marked } from "marked";
import { computed, inject, onUpdated, ref, watch } from "vue";
import { useI18n } from "vue-i18n";
import { useRoute } from "vue-router";
import type { CristalApp } from "@xwiki/cristal-api";
import type { DocumentService } from "@xwiki/cristal-document-api";
import type {
  PageHierarchyItem,
  PageHierarchyResolverProvider,
} from "@xwiki/cristal-hierarchy-api";
import type { ComputedRef, Ref } from "vue";

const { t } = useI18n({
  messages,
});

const route = useRoute();

const avImg = xavatarImg;

const cristal: CristalApp = inject<CristalApp>("cristal")!;
const documentService = cristal
  .getContainer()
  .get<DocumentService>(documentServiceName);

const loading = documentService.isLoading();
const error: Ref<Error | undefined> = documentService.getError();
const currentPage: Ref<PageData | undefined> =
  documentService.getCurrentDocument();
const currentPageRevision: Ref<string | undefined> =
  documentService.getCurrentDocumentRevision();
const currentPageName: ComputedRef<string> = computed(() => {
  // TODO: define a proper abstraction.
  return (
    (route.params.page as string) || cristal.getCurrentPage() || "Main.WebHome"
  );
});

const breadcrumbRoot = ref(undefined);
const contentRoot = ref(undefined);

const content: ComputedRef<string | undefined> = computed(() => {
  if (currentPage.value) {
    const cpn: PageData = currentPage.value;
    if (cpn.html && cpn.html.trim() !== "") {
      return cpn.html as string;
    } else if (cpn.source) {
      // TODO: currently blindly convert the content to markdown.
      console.log("marked", marked, cpn.source);
      const parse = marked.parse(cpn.source);
      console.log("parse", parse);
      return parse as string;
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

const breadcrumbItems: Ref<Array<PageHierarchyItem>> = ref([]);
watch(
  currentPage,
  async (p) => {
    if (p) {
      try {
        breadcrumbItems.value = await cristal
          .getContainer()
          .get<PageHierarchyResolverProvider>("PageHierarchyResolverProvider")
          .get()
          .getPageHierarchy(p);
      } catch (e) {
        console.error(e);
        breadcrumbItems.value = [];
      }
    }
  },
  { immediate: true },
);

onUpdated(() => {
  ContentTools.transformImages(cristal, "xwikicontent");

  if (cristal && breadcrumbRoot.value) {
    ContentTools.listenToClicks(breadcrumbRoot.value, cristal);
  }

  if (cristal && contentRoot.value) {
    ContentTools.listenToClicks(contentRoot.value, cristal);
    ContentTools.transformMacros(contentRoot.value, cristal);
  }
});
</script>
<template>
  <div v-if="loading" class="content-loading">
    <!-- TODO: improve loading UI. -->
    <span class="load-spinner"></span>
    <h3>Loading</h3>
  </div>
  <div v-else-if="error" class="content-error">
    <!-- TODO: improve error reporting. -->
    {{ error }}
  </div>
  <article v-else id="content" ref="root" class="content">
    <UIX uixname="content.before" />

    <alerts-toasts></alerts-toasts>

    <div class="page-header">
      <!-- This div lets us reference an actual HTML element,
             to be used in `ContentTools.listenToClicks()`. -->
      <div id="breadcrumbRoot" ref="breadcrumbRoot">
        <XBreadcrumb class="breadcrumb" :items="breadcrumbItems"></XBreadcrumb>
      </div>
      <x-btn circle size="small" variant="primary" color="primary">
        <c-icon
          class="new-page"
          name="plus"
          :label="t('page.actions.create.label')"
        ></c-icon>
      </x-btn>
    </div>

    <div class="doc-header">
      <div class="doc-header-inner">
        <h1 class="doc-title">{{ title }}</h1>
        <div class="doc-info">
          <span class="doc-info-user-info">
            <x-avatar class="avatar" :image="avImg" size="2rem"></x-avatar>
            User Name edited on 12/12/2024 at 12:00
          </span>
          <!-- TODO: add a way to inject those by extension
               and provide one for the number of attachments.
              It must be reactive whenever the attachment store is updated -->
          <div class="doc-info-actions">
            <suspense>
              <info-actions></info-actions>
            </suspense>
            <div class="doc-page-actions">
              <router-link
                :to="
                  currentPageRevision
                    ? ''
                    : {
                        name: 'edit',
                        params: { page: currentPageName },
                      }
                "
              >
                <x-btn
                  size="small"
                  :disabled="currentPageRevision !== undefined"
                >
                  <c-icon name="pencil" :size="Size.Small"></c-icon>
                  Edit
                </x-btn>
              </router-link>
              <page-actions
                :current-page="currentPage"
                :current-page-name="currentPageName"
                :disabled="currentPageRevision !== undefined"
              ></page-actions>
            </div>
          </div>
        </div>
        <div class="doc-header-alerts">
          <!-- Indicate that the page displayed is not the current version. -->
          <x-alert v-if="currentPageRevision !== undefined" type="warning">
            {{
              t("history.alert.content", {
                revision: currentPageRevision,
                pageName: currentPageName,
              })
            }}
            <router-link
              :to="{ name: 'view', params: { page: currentPageName } }"
              >{{ t("history.alert.link.label") }}
            </router-link>
          </x-alert>
        </div>
      </div>
    </div>

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
    <!-- The footer is not displayed in case of unknown page. -->
    <div v-if="pageExist" class="doc-info-extra">
      <!-- Suspense is mandatory here as extra-tabs is asynchronous -->
      <suspense>
        <extra-tabs></extra-tabs>
      </suspense>
    </div>
    <suspense>
      <u-i-extensions uix-name="content.after"></u-i-extensions>
    </suspense>
  </article>
</template>
<style scoped>
.content {
  padding: 0;
}

.content-loading {
  display: flex;
  flex-flow: column;
  height: 100vh;
  align-items: center;
  justify-content: center;
}

/*TABLE STYLES*/
/*TODO: Check a better way to write these styles without the global tag. Currently impossible to use :deep because the html inside the document content is not assigned an ID */
:global(.content),
:global(.content > .edit-wrapper) {
  display: grid;
  grid-template-rows: auto auto auto 1fr;
  gap: var(--cr-spacing-small);
  overflow: auto;
  scrollbar-gutter: stable;
  height: 100%;
}

:global(.content:has(.edit-wrapper)) {
  grid-template-rows: 1fr;
}

:global(.content > .edit-wrapper) {
  grid-template-rows: auto 1fr;
}

:global(.doc-content),
:global(.doc-header-inner) {
  padding: 0;
}

/*---*/

.content-loading svg {
  width: 64px;
  height: 64px;
}

.content-loading h3 {
  padding: 0;
  margin: 0;
  color: var(--cr-color-neutral-500);
}

.edit-icon {
  font-size: 14px;
}

.new-page {
  display: block;
}

:global(.doc-header) {
  display: grid;
  grid-auto-flow: column;
  grid-template-columns: 1fr;
  grid-template-rows: auto auto;
  gap: 0px 0px;
  grid-template-areas:
    "title"
    "info-user";
  gap: var(--cr-spacing-x-small);
  top: 0;
  background: white;
  z-index: 1;
}

.doc-header-alerts:not(:empty) {
  margin-top: var(--cr-spacing-x-small);
}

.doc-title {
  grid-area: title;
  margin: 0;
  font-size: var(--cr-font-size-2x-large);
  line-height: var(--cr-font-size-2x-large);
}

.doc-info {
  grid-area: info-user;
  display: flex;
  flex-flow: row;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--cr-spacing-small);
  color: var(--cr-color-neutral-500);
  font-size: var(--cr-font-size-small);
  justify-content: space-between;
}

.avatar {
  --size: 24px;
}

.doc-info-actions,
.doc-page-actions {
  display: flex;
  flex-wrap: wrap;
  flex-flow: row;
  align-items: center;
  gap: var(--cr-spacing-2x-small);
}

.doc-info-actions {
  justify-self: end;
}

.info-action {
  display: flex;
  background-color: var(--cr-color-neutral-100);
  border-radius: 99px;
  padding: var(--cr-spacing-2x-small) var(--cr-spacing-2x-small);
  font-size: var(--cr-font-size-medium);
  flex-flow: row;
  gap: var(--cr-spacing-2x-small);
  align-items: center;
}

.info-action .cr-icon {
  line-height: 1.3rem;
}

.page-header {
  padding: var(--cr-spacing-small) 0;
  display: flex;
  flex-wrap: wrap;
  gap: var(--cr-spacing-medium);
  align-items: center;
  flex-flow: row;
}

.inner-content {
  display: flex;
  flex-flow: column;
  flex-basis: auto;
  overflow: auto;
}

.doc-info-extra {
  &.floating {
    position: sticky;
    bottom: 0;
    background: white;
    box-shadow: var(--cr-shadow-small);
  }
}
</style>
