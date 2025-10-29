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
import CSidebarPanel from "./c-sidebar-panel.vue";
import messages from "../translations";
import { name as documentServiceName } from "@xwiki/cristal-document-api";
import { CIcon, Size } from "@xwiki/cristal-icons";
import { inject, ref } from "vue";
import { useI18n } from "vue-i18n";
import type { CristalApp } from "@xwiki/cristal-api";
import type { DocumentService } from "@xwiki/cristal-document-api";
import type { TreeNode } from "@xwiki/cristal-fn-utils";
import type { UIExtensionsManager } from "@xwiki/cristal-uiextension-api";
import type { Ref } from "vue";

const cristal: CristalApp = inject<CristalApp>("cristal")!;
const uixManager: UIExtensionsManager = cristal
  .getContainer()
  .get<UIExtensionsManager>("UIExtensionsManager")!;
const documentService = cristal
  .getContainer()
  .get<DocumentService>(documentServiceName);
const currentPageReference: Ref<string | undefined> =
  documentService.getCurrentDocumentReferenceString();

const { t } = useI18n({
  messages,
});

const openedSettings: Ref<string[]> = ref([]);

const instanceSettingsUix = await uixManager.list("settings.categories");

// Right now, we only have settings related to the Cristal instance.
// Future backend-related settings should be dynamically added to the root node.
const treeNodeRoot: Ref<TreeNode<{ id: string; label: string; url?: string }>> =
  ref({
    id: "root",
    label: "Root",
    children: [
      {
        id: "settings.categories",
        label: t("admin.instance.title"),
        children: instanceSettingsUix.map((uix) => ({
          id: uix.id,
          label: uix.parameters["title"] as string,
          url: cristal
            .getRouter()
            .resolve({ name: "admin", params: { page: uix.id } }).href,
        })),
      },
    ],
  });

instanceSettingsUix.forEach((uix) => {
  if (uix.id == currentPageReference.value) {
    openedSettings.value.push("settings.categories");
  }
});
</script>
<template>
  <c-sidebar-panel name="">
    <span>
      <x-btn @click="cristal.setCurrentPage(cristal.getWikiConfig().homePage)">
        <c-icon :size="Size.Small" name="arrow-left"></c-icon>
        {{ t("admin.back.button") }}
      </x-btn>
    </span>
    <XTree
      :rootNode="treeNodeRoot"
      v-model:activated="currentPageReference"
      v-model:opened="openedSettings"
    ></XTree>
  </c-sidebar-panel>
</template>
