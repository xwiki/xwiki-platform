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

const cristal = inject<CristalApp>("cristal");
let link = "/" + cristal?.getCurrentPage() + "/view";
const pageStatus = ref({
  currentContent: "Initial content",
  currentSource: "Initial source",
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
    <v-textarea
      v-model="pageStatus.currentSource"
      auto-grow
      rows="25"
      variant="outlined"
      label="Editor"
      width="100%"
    />
  </div>
</template>
<style>
textarea {
  border: 1px solid black;
  width: 100%;
}
</style>
