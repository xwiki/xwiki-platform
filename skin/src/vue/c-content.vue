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
import { type CristalApp, PageData } from "@cristal/api";
import { marked } from "marked";
import { ContentTools } from "./contentTools";

const loading = ref(false);
const error: Ref<Error | undefined> = ref(undefined);
const currentPage: Ref<PageData | undefined> = ref(undefined);
const currentPageName: ComputedRef<string> = computed(() => {
  // TODO: define a proper abstraction.
  return cristal?.getCurrentPage() || "XWiki.Main";
});

const contentRoot = ref(undefined);

const content: ComputedRef<string> = computed(() => {
  if (currentPage.value) {
    const cpn: PageData = currentPage.value;
    if (cpn.html && cpn.html.trim() !== "") {
      return cpn.html as string;
    } else {
      // TODO: currently blindly convert the content to markdown.
      console.log("marked", marked, cpn.source);
      const parse = marked.parse(cpn.source);
      console.log("parse", parse);
      return parse as string;
    }
  } else {
    return "";
  }
});

const cristal: CristalApp = inject<CristalApp>("cristal")!;

const route = useRoute();

async function fetchPage() {
  loading.value = true;
  try {
    currentPage.value = await cristal.getPage(currentPageName.value);
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

  if (cristal && content.value != null) {
    ContentTools.listenToClicks(contentRoot.value!, cristal);
    ContentTools.transformMacros(contentRoot.value!, cristal);
  }
});
</script>
<template>
  <div v-if="loading">
    <!-- TODO: improve loading UI. -->
    LOADING
  </div>
  <div v-else-if="error">
    <!-- TODO: improve error reporting. -->
    {{ error }}
  </div>
  <article v-else id="content" ref="root">
    <UIX uixname="content.before" />
    <div class="pagemenu">
      <router-link
        :to="{
          name: 'edit',
          params: { page: currentPageName },
        }"
        >Edit
      </router-link>
    </div>
    <!-- Provide a target for the links listener, otherwise the links from other 
    elements of the component can be wrongly captured. -->
    <div ref="contentRoot">
      <!-- eslint-disable vue/no-v-html -->
      <div id="xwikicontent" v-html="content" />
    </div>
    <UIX uixname="content.after" />
  </article>
</template>
<style>
.pagemenu {
  float: right;
}
</style>
