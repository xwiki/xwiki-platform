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
import UIX from "./c-uix.vue";
import messages from "../translations";
import { AlertsToasts } from "@xwiki/cristal-alerts-ui";
import { Date } from "@xwiki/cristal-date-ui";
import { ExtraTabs } from "@xwiki/cristal-extra-tabs-ui";
import { InfoActions } from "@xwiki/cristal-info-actions-ui";
import { UIExtensions } from "@xwiki/cristal-uiextension-ui";
import { User } from "@xwiki/cristal-user-ui";
import { inject, ref, watch } from "vue";
import { useI18n } from "vue-i18n";
import type { CristalApp, PageData } from "@xwiki/cristal-api";
import type {
  PageHierarchyItem,
  PageHierarchyResolverProvider,
} from "@xwiki/cristal-hierarchy-api";
import type { DocumentReference } from "@xwiki/cristal-model-api";
import type { Ref } from "vue";

// FIXME: since vue-tsc 2.2.6, this line fix with a compilation error for no obvious reasons as other components of the
// same package are also relying on use18n + object destructuring. Therefore, we need to define a fake type for t until
// this is fixed.
const { t }: { t: (key: string) => string } = useI18n({
  messages,
});

const cristal: CristalApp = inject<CristalApp>("cristal")!;
const { currentPage, currentPageReference } = defineProps<{
  loading: boolean;
  error: Error | undefined;
  currentPage: PageData | undefined;
  currentPageReference: DocumentReference | undefined;
  pageExist: boolean;
  beforeUIXPId: string;
  afterUIXPId: string;
}>();

const breadcrumbItems: Ref<Array<PageHierarchyItem>> = ref([]);
watch(
  () => currentPageReference,
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
</script>

<template>
  <div v-if="loading" class="content-loading">
    <!-- TODO: improve loading UI. -->
    <span class="load-spinner"></span>
    <h3>{{ t("article.loading") }}</h3>
  </div>
  <div v-else-if="error" class="content-error">
    <!-- TODO: provide a better error reporting. -->
    {{ error }}
  </div>
  <article v-else id="content" ref="root" class="content">
    <UIX :uixname="beforeUIXPId" />

    <alerts-toasts></alerts-toasts>

    <div class="page-header">
      <XBreadcrumb class="breadcrumb" :items="breadcrumbItems"></XBreadcrumb>
    </div>

    <div class="doc-header">
      <div class="doc-header-inner">
        <slot name="title"></slot>
        <div class="info-wrapper">
          <span class="doc-author">
            <i18n-t
              v-if="
                currentPage?.lastAuthor && currentPage?.lastModificationDate
              "
              keypath="page.edited.details.user"
              tag="span"
            >
              <template #date>
                <date :date="currentPage?.lastModificationDate!" />
              </template>
              <template #user>
                <user :user="currentPage?.lastAuthor" />
              </template>
            </i18n-t>
            <i18n-t
              v-else-if="currentPage?.lastModificationDate"
              keypath="page.edited.details"
              tag="span"
            >
              <template #date>
                <date :date="currentPage?.lastModificationDate!" />
              </template>
            </i18n-t>
          </span>
          <!-- TODO: add a way to inject those by extension
                 and provide one for the number of attachments.
                It must be reactive whenever the attachment store is updated -->
          <div class="doc-info-actions">
            <suspense>
              <info-actions></info-actions>
            </suspense>

            <slot name="doc-page-actions"></slot>
          </div>
        </div>
        <div class="doc-header-alerts">
          <slot name="doc-header-alerts"></slot>
        </div>
      </div>
    </div>

    <slot></slot>

    <!-- The footer is not displayed in case of unknown page. -->
    <div v-if="pageExist" class="doc-info-extra">
      <!-- Suspense is mandatory here as extra-tabs is asynchronous -->
      <suspense>
        <extra-tabs></extra-tabs>
      </suspense>
    </div>
    <suspense>
      <u-i-extensions :uix-name="afterUIXPId"></u-i-extensions>
    </suspense>
  </article>
</template>

<style scoped>
.content-loading {
  display: flex;
  flex-flow: column;
  height: 100vh;
  align-items: center;
  justify-content: center;
}

:deep(blockquote) {
  background-color: var(--cr-color-neutral-50);
  color: var(--cr-color-neutral-600);
  font-size: var(--cr-font-size-large);
  border-inline-start: 2px solid var(--cr-color-neutral-200);
  padding-inline-start: var(--cr-spacing-large);
  margin: 0;
}

:deep(.doc-title) {
  grid-area: doc-title;
  align-self: center;
  display: flex;
  border: none;
  font-size: var(--cr-font-size-2x-large);
  font-weight: var(--cr-font-weight-bold);
  flex-flow: column;
  justify-self: center;
  outline: none;
  margin: 0;
  max-width: var(--cr-sizes-max-page-width);
  width: 100%;
  line-height: var(--cr-font-size-2x-large);
  padding-block-start: var(--cr-spacing-small);
}

@container xwCristal (max-width: 600px) {
  .content {
    padding-left: 0 var(--cr-spacing-x-small);
  }
}

/*
 * Code block style.
 * TODO: replace with a code macro rendering as soon as we support macro.
 */
:deep(.box .code),
:deep(.doc-content.editor pre) {
  font-family: var(--cr-font-mono);
  background: var(--cr-color-neutral-100);
  border-radius: var(--cr-border-radius-medium);
  padding: var(--cr-spacing-small);
}

/*TABLE STYLES*/
/*TODO: Check a better way to write these styles without the global tag. Currently impossible to use :deep because the html inside the document content is not assigned an ID */
.content {
  padding: 0 var(--cr-spacing-2x-large);
  overflow: auto;
}

.content {
  display: grid;
  grid-template-rows: auto auto 1fr auto;
  gap: var(--cr-spacing-small);
  scrollbar-gutter: stable;
  height: 100%;
}

.content-loading svg {
  width: 64px;
  height: 64px;
}

.content-loading h3 {
  padding: 0;
  margin: 0;
  color: var(--cr-color-neutral-500);
}

.new-page {
  display: block;
}

.doc-header {
  top: 0;
  background: white;
  z-index: 1;

  & .doc-header-inner {
    display: grid;
    grid-template-columns: 1fr;
    grid-template-rows: auto auto auto;
    gap: var(--cr-spacing-x-small);
    grid-auto-flow: row;
    grid-template-areas:
      "doc-title"
      "info-wrapper"
      "alerts";
    margin: 0 auto;

    & .info-wrapper {
      grid-area: info-wrapper;
      display: flex;
      flex-flow: wrap;
      gap: var(--cr-spacing-small);

      & .doc-author {
        grid-area: doc-author;
        margin-inline-end: auto;
        font-size: var(--cr-font-size-small);
        color: var(--cr-color-neutral-600);
        align-self: center;

        & .avatar {
          --size: 24px;
        }
      }

      & .doc-info {
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

      & .doc-info-actions {
        display: flex;
        flex-wrap: wrap;
        flex-flow: row;
        align-items: center;
        gap: var(--cr-spacing-2x-small);
        justify-self: end;
      }
    }
  }

  .doc-header-alerts:not(:empty) {
    margin-top: var(--cr-spacing-x-small);
  }
}

.page-header {
  padding: var(--cr-spacing-small) 0;
  display: flex;
  flex-wrap: wrap;
  gap: var(--cr-spacing-medium);
  align-items: center;
  flex-flow: row;
}

.doc-info-extra {
  &.floating {
    position: sticky;
    bottom: 0;
    background: white;
    box-shadow: var(--cr-shadow-small);
  }
}

.doc-header-inner {
  padding: 0;
}

/* External links style. */
:deep(.wikiexternallink) {
  font-style: italic;
  position: relative;
  display: inline-flex;
}

:deep(.wikiexternallink a:after),
:deep(a.wikiexternallink:after) {
  /*
  TODO: Try to make this customisable, it is dependant on bootstrap icons for now.
  */
  content: "\F1C5";
  display: inline-block;
  font-family: bootstrap-icons;
  font-style: normal;
  font-weight: bold;
  text-transform: none;
  line-height: 1;
  -webkit-font-smoothing: antialiased;
  font-size: 0.7rem;
  text-decoration: none;
  margin-left: 4px;
}
</style>
