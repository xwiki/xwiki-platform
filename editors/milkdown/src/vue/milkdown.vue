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
import { CristalApp } from "@cristal/api";
import { inject, ref } from "vue";
import { Milkdown, useEditor } from "@milkdown/vue";
import { defaultValueCtx, Editor, rootCtx } from "@milkdown/core";
import { nord } from "@milkdown/theme-nord";
import { commonmark } from "@milkdown/preset-commonmark";
import "@milkdown/theme-nord/style.css";

const cristal = inject<CristalApp>("cristal");
let link = "/" + cristal?.getCurrentPage() + "/view";

const pageStatus = ref({
  currentContent: "Initial content",
  currentSource: cristal?.getCurrentSource(),
  css: [],
  js: [],
  html: "",
  document: null,
  withSheet: false,
  sheet: "",
});
if (cristal) {
  cristal.setContentRef(pageStatus);
}

useEditor((root) => {
  return Editor.make()
    .config(nord)
    .config((ctx) => {
      ctx.set(rootCtx, root);
      ctx.set(defaultValueCtx, pageStatus.value.currentSource);
    })
    .use(commonmark);
});
</script>
<template>
  <div class="pagemenu">
    <router-link :to="link">
      <x-btn>Cancel</x-btn>
    </router-link>
    <x-btn>Save</x-btn>
  </div>
  <br />
  <br />
  <div id="xwikicontent">
    <Milkdown />
  </div>
</template>
