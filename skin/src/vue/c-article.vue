<script setup lang="ts">
import xavatarImg from "../images/no-one.svg";
import messages from "../translations";
import { AlertsToasts } from "@xwiki/cristal-alerts-ui";
import { PageData } from "@xwiki/cristal-api";
import { ExtraTabs } from "@xwiki/cristal-extra-tabs-ui";
import { CIcon } from "@xwiki/cristal-icons";
import { InfoActions } from "@xwiki/cristal-info-actions-ui";
import { UIExtensions } from "@xwiki/cristal-uiextension-ui";
import { Ref, inject, ref, watch } from "vue";
import { useI18n } from "vue-i18n";
import type { CristalApp } from "@xwiki/cristal-api";
import type {
  PageHierarchyItem,
  PageHierarchyResolverProvider,
} from "@xwiki/cristal-hierarchy-api";

const avImg = xavatarImg;
const { t } = useI18n({
  messages,
});

const cristal: CristalApp = inject<CristalApp>("cristal")!;
const { currentPage } = defineProps<{
  loading: boolean;
  error: Error | undefined;
  currentPage: PageData | undefined;
  pageExist: boolean;
  beforeUIXPId: string;
  afterUIXPId: string;
}>();

const breadcrumbItems: Ref<Array<PageHierarchyItem>> = ref([]);
watch(
  () => currentPage,
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
        <slot name="title"></slot>
        <div class="info-wrapper">
          <span class="doc-author">
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

@container xwCristal (max-width: 600px) {
  .content {
    padding-left: 0 var(--cr-spacing-x-small);
  }
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
</style>
