<!--
 * See the LICENSE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 *
 * This file is part of the Cristal Wiki software prototype
 * @copyright  Copyright (c) 2023 XWiki SAS
 * @license    http://opensource.org/licenses/AGPL-3.0 AGPL-3.0
 *
-->
<script lang="ts" setup>
import {
  computed,
  type ComputedRef,
  inject,
  onUpdated,
  ref,
  type Ref,
  watch,
} from "vue";
import { useRoute } from "vue-router";
import { type CristalApp, PageData } from "@xwiki/cristal-api";
import { marked } from "marked";
import { ContentTools } from "./contentTools";
import { CIcon, Size } from "@xwiki/cristal-icons";
import xavatarImg from "../images/no-one.svg";

const route = useRoute();

const avImg = xavatarImg;

const loading = ref(false);
const error: Ref<Error | undefined> = ref(undefined);
const currentPage: Ref<PageData | undefined> = ref(undefined);
const currentPageName: ComputedRef<string> = computed(() => {
  // TODO: define a proper abstraction.
  return (
    (route.params.page as string) || cristal.getCurrentPage() || "Main.WebHome"
  );
});

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

const cristal: CristalApp = inject<CristalApp>("cristal")!;

async function fetchPage(page: string) {
  loading.value = true;
  try {
    // Provides a reactive variable to be updated if the page content is updated
    // in the background.
    cristal.setContentRef(currentPage);
    currentPage.value = await cristal.getPage(page || currentPageName.value);
  } catch (e) {
    console.error(e);
    error.value = e;
  } finally {
    loading.value = false;
  }
}

watch(() => route.params.page, fetchPage, { immediate: true });

onUpdated(() => {
  ContentTools.transformImages(cristal, "xwikicontent");

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
    <div class="inner-content">
      <div class="content-header">
        <XBreadcrumb
          class="breadcrumb"
          :items="['Home', 'First', 'Second', 'Third']"
        ></XBreadcrumb>
        <x-btn circle size="small" variant="primary" color="primary">
          <c-icon
            class="new-page"
            name="plus"
            label="Create a new document"
          ></c-icon>
        </x-btn>
      </div>
      <div class="content-scroll">
        <div class="doc-header">
          <div class="center-content">
            <h1 class="document-title">{{ title }}</h1>
            <div class="doc-info-header">
              <x-avatar class="avatar" :image="avImg" size="2rem"></x-avatar>
              <span class="doc-info-user-info">
                User Name edited on 12/12/2024 at 12:00
              </span>
              <div class="doc-info-actions">
                <div class="info-action like">
                  <c-icon name="heart" :size="Size.Small"></c-icon>
                  <span class="counter">99</span>
                </div>
                <div class="info-action comments">
                  <c-icon name="chat" :size="Size.Small"></c-icon>
                  <span class="counter">99</span>
                </div>
                <div class="info-action attachments">
                  <c-icon name="paperclip" :size="Size.Small"></c-icon>
                  <span class="counter">99</span>
                </div>
              </div>
              <div class="doc-page-actions">
                <router-link
                  :to="{
                    name: 'edit',
                    params: { page: currentPageName },
                  }"
                >
                  <x-btn size="small">
                    <c-icon name="pencil" :size="Size.Small"></c-icon>
                    Edit
                  </x-btn>
                </router-link>
                <x-btn size="small">
                  <c-icon
                    name="three-dots-vertical"
                    :size="Size.Small"
                  ></c-icon>
                </x-btn>
              </div>
            </div>
          </div>
        </div>
        <div class="whole-content">
          <div class="center-content">
            <!-- eslint-disable vue/no-v-html -->
            <div
              v-if="content"
              id="xwikicontent"
              ref="contentRoot"
              class="document-content"
              v-html="content"
            ></div>
            <div v-else class="unknown-page">
              The requested page could not be found. You can edit the page to
              create it.
            </div>
            <div class="doc-info-footer">
              <x-avatar class="avatar"></x-avatar>
              <span class="doc-info-user-info">User name created...</span>
            </div>
          </div>
        </div>
      </div>
    </div>
    <UIX uixname="content.after" />
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

/*TABLE STYLES*/
/*TODO: Check a better way to write these styles without the global tag. Currently impossible to use :deep because the html inside the document content is not assigned an ID */

:global(.document-content table) {
  max-width: 100%;
  width: 100%;
  border: none;
  border-collapse: collapse;
  color: inherit;
  margin-bottom: var(--cr-spacing-small);
}

:global(.document-content table tr) {
  border-bottom: solid 1px var(--cr-input-border-color);
}

:global(.document-content table th) {
  font-weight: var(--cr-font-weight-bold);
  text-align: start;
}

:global(.document-content table td),
:global(.document-content table th) {
  line-height: var(--cr-line-height-normal);
  padding: 1rem 1rem;
}

:global(.document-content table th p:first-child),
:global(.document-content table td p:first-child) {
  margin-top: 0;
}

:global(.document-content table th p:last-child),
:global(.document-content table td p:last-child) {
  margin-bottom: 0;
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

.doc-header {
  display: flex;
  width: 100%;
  flex-flow: column;
  gap: var(--cr-spacing-x-small);
  position: sticky;
  top: calc(var(--cr-spacing-small) * -1);
  background: white;
  align-items: center;
  z-index: 1;
}

.document-title {
  margin: 0;
  font-size: var(--cr-font-size-2x-large);
  line-height: var(--cr-font-size-2x-large);
}

.counter {
  background-color: var(--cr-color-primary-600);
  font-weight: var(--cr-font-weight-semibold);
  font-size: var(--cr-font-size-x-small);
  line-height: var(--cr-font-size-2x-small);
  border-radius: 99px;
  color: #fff;
  flex-shrink: 1;
  flex-grow: 0;
  display: block;
  padding: var(--cr-spacing-2x-small) var(--cr-spacing-x-small);
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
  margin-right: var(--cr-spacing-medium);
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

.doc-info-header,
.doc-info-footer {
  display: flex;
  flex-flow: row;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--cr-spacing-small);
}

.doc-info-user-info {
  color: var(--cr-color-neutral-500);
  font-size: var(--cr-font-size-small);
  margin-right: auto;
}

.avatar {
  --size: 24px;
}

.content-header {
  padding: var(--cr-spacing-x-small) var(--cr-spacing-2x-large);
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
</style>
