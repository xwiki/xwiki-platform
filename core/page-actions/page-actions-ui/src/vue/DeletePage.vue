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
import { type Ref, inject, ref } from "vue";
import { useI18n } from "vue-i18n";
import type { CristalApp, PageData } from "@xwiki/cristal-api";
import type { AlertsService } from "@xwiki/cristal-alerts-api";
import type {
  PageHierarchyItem,
  PageHierarchyResolverProvider,
} from "@xwiki/cristal-hierarchy-api";
import { CIcon, Size } from "@xwiki/cristal-icons";
import messages from "../translations";

const { t } = useI18n({
  messages,
});
const props = defineProps<{
  currentPage: PageData | undefined;
  currentPageName: string;
}>();

const cristal: CristalApp = inject<CristalApp>("cristal")!;
const alertsService: AlertsService = cristal
  .getContainer()
  .get<AlertsService>("AlertsService")!;
const deleteDialogOpen: Ref<boolean> = ref(false);

async function deletePage() {
  const hierarchy: Array<PageHierarchyItem> = await cristal
    .getContainer()
    .get<PageHierarchyResolverProvider>("PageHierarchyResolverProvider")
    .get()
    .getPageHierarchy(props.currentPage!);

  const result = await cristal
    .getWikiConfig()
    .storage.delete(props.currentPageName);
  deleteDialogOpen.value = false;

  if (result.success) {
    if (hierarchy.length > 1) {
      cristal.setCurrentPage(hierarchy[hierarchy.length - 2].pageId, "view");
    } else {
      cristal.setCurrentPage(cristal.getWikiConfig().homePage, "view");
    }
    alertsService.success(
      t("page.action.action.delete.page.success", {
        page: props.currentPageName,
      }),
    );
  } else {
    alertsService.error(
      t("page.action.action.delete.page.error", {
        page: props.currentPageName,
        reason: result.error!,
      }),
    );
  }
}
</script>

<template>
  <x-dialog
    v-model="deleteDialogOpen"
    width="auto"
    :title="t('page.action.action.delete.page.dialog.title')"
  >
    <template #activator>
      <c-icon name="trash" :size="Size.Small"></c-icon>
      {{ t("page.action.action.delete.page.title") }}
    </template>
    <template #default>
      <p>
        {{
          t("page.action.action.delete.page.confirm", { page: currentPageName })
        }}
      </p>
      <x-btn @click.stop="deletePage">
        {{ t("page.action.action.delete.page.title") }}
      </x-btn>
    </template>
  </x-dialog>
</template>

<style scoped></style>
