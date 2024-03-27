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
import CTemplate from "./c-template.vue";
import { inject, onMounted, onUpdated, ref } from "vue";
import type { CristalApp, Logger } from "@cristal/api";
import { ContentTools } from "./contentTools";

const root = ref(null);
const cristal = inject<CristalApp>("cristal");
const pageStatus = ref({
  currentContent: cristal?.getCurrentContent(),
  css: [],
  js: [],
  html: "",
  document: null,
  withSheet: false,
  sheet: "",
});
let logger: Logger | undefined = undefined;

if (cristal != null) {
  ContentTools.init(cristal);
  logger = cristal.getLogger("skin.vue.content");
  logger?.debug("cristal object content ref set");
  cristal.setContentRef(pageStatus);
  logger?.debug("Sheet is ", pageStatus.value.sheet);
  logger?.debug("With Sheet is ", pageStatus.value.withSheet);
} else {
  console.error("cristal object not injected properly in c-content.vue");
}

let link = "/" + cristal?.getCurrentPage() + "/edit";
let linkXWiki = "/" + cristal?.getCurrentPage() + "/editxwiki";
let linkText = "/" + cristal?.getCurrentPage() + "/edittext";
let linkMilkdown = "/" + cristal?.getCurrentPage() + "/editmilkdown";
let linkProsemirror = "/" + cristal?.getCurrentPage() + "/editprosemirror";
let serverSideRendering = cristal?.getWikiConfig().serverRendering;

onMounted(() => {
  const cristal = inject<CristalApp>("cristal");
  logger?.debug("in mounted");

  ContentTools.transformImages(cristal, "xwikicontent");
  // ContentTools.transformScripts(cristal);
});

onUpdated(() => {
  const cristal = inject<CristalApp>("cristal");
  logger?.debug("in updated");

  logger?.debug("Sheet is ", pageStatus.value.sheet);
  logger?.debug("With Sheet is ", pageStatus.value.withSheet);

  if (cristal && root.value != null) {
    ContentTools.listenToClicks(root.value, cristal);
    ContentTools.transformMacros(root.value, cristal);
  }
  ContentTools.loadCSS(pageStatus.value.css);
  ContentTools.loadJS(pageStatus.value.js);
});
</script>
<template>
  <article id="content" ref="root">
    <UIX uixname="content.before" />
    <div class="pagemenu">
      <x-menu title="Edit">
        <template #activator="{ props }">
          <span v-bind="props"> Edit </span>
        </template>
        <template #default>
          <x-menu-item :link="link" title="Default Editor">
            <router-link :to="link">Default Editor</router-link>
          </x-menu-item>
          <x-menu-item :link="linkText" title="Text Editor">
            <router-link :to="linkText">Text Editor</router-link>
          </x-menu-item>
          <x-menu-item :link="linkXWiki" title="XWiki Editor">
            <router-link :to="linkXWiki">XWiki Editor</router-link>
          </x-menu-item>
          <x-menu-item :link="linkMilkdown" title="Milkdown Editor">
            <router-link :to="linkMilkdown">Milkdown Editor</router-link>
          </x-menu-item>
          <x-menu-item :link="linkProsemirror" title="Prosemirror Editor">
            <router-link :to="linkProsemirror">Prosemirror Editor</router-link>
          </x-menu-item>
        </template>
      </x-menu>
    </div>
    <template v-if="pageStatus.withSheet && !serverSideRendering">
      <CTemplate
        :name="pageStatus.sheet"
        :document="pageStatus.document"
        mode="view"
      />
    </template>
    <template v-else>
      <!-- eslint-disable vue/no-v-html -->
      <div id="xwikicontent" v-html="pageStatus.currentContent" />
    </template>
    <UIX uixname="content.after" />
  </article>
</template>
<style>
.pagemenu {
  float: right;
}
</style>
