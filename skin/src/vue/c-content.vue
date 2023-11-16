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
<template>
  <article id="content">
    <UIX uixname="content.before" />
    <div class="pagemenu">
      <router-link :to="link">
        <x-btn class="pagemenu"> Edit </x-btn>
      </router-link>
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
<script lang="ts">
import { inject, ref } from "vue";
import { CristalApp, Logger } from "@cristal/api";
import { ContentTools } from "./contentTools";
import CTemplate from "./c-template.vue";

const pageStatus = ref({
  currentContent: "Initial content",
  css: [],
  js: [],
  html: "",
  document: null,
  withSheet: false,
  sheet: "",
});
let logger: Logger;

export default {
  components: [CTemplate],
  setup() {
    const cristal = inject<CristalApp>("cristal");
    if (cristal != null) {
      ContentTools.init(cristal);
      logger = cristal.getLogger("skin.vue.content");
      logger?.debug("cristal object content ref set");
      cristal.setContentRef(pageStatus);
    } else {
      console.error("cristal object not injected properly in c-content.vue");
    }
    logger?.debug("Sheet is ", pageStatus.value.sheet);
    logger?.debug("With Sheet is ", pageStatus.value.withSheet);
    return {
      pageStatus: pageStatus,
      link: "/" + cristal?.getCurrentPage() + "/edit",
      serverSideRendering: cristal?.getWikiConfig().serverRendering,
    };
  },
  mounted() {
    const cristal = inject<CristalApp>("cristal");
    logger?.debug("in mounted");

    ContentTools.transformImages(cristal, "xwikicontent");
    // ContentTools.transformScripts(cristal);
  },
  updated() {
    const cristal = inject<CristalApp>("cristal");
    logger?.debug("in updated");

    logger?.debug("Sheet is ", pageStatus.value.sheet);
    logger?.debug("With Sheet is ", pageStatus.value.withSheet);

    if (cristal) {
      ContentTools.listenToClicks(this.$el, cristal);
      ContentTools.transformMacros(this.$el, cristal);
    }
    ContentTools.loadCSS(pageStatus.value.css);
    ContentTools.loadJS(pageStatus.value.js);
  },
};
</script>
<style type="text/css">
.pagemenu {
  float: right;
}
</style>
