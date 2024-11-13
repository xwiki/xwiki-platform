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
import PageActionsCategory from "./PageActionsCategory.vue";
import { inject } from "vue";
import { CIcon, Size } from "@xwiki/cristal-icons";
import type { CristalApp, PageData } from "@xwiki/cristal-api";
import type {
  PageActionCategory,
  PageActionCategoryService,
} from "@xwiki/cristal-page-actions-api";

defineProps<{
  currentPage: PageData | undefined;
  currentPageName: string;
  disabled: boolean;
}>();

const cristal: CristalApp = inject<CristalApp>("cristal")!;
const actionCategoryService: PageActionCategoryService = cristal
  .getContainer()
  .get<PageActionCategoryService>("PageActionCategoryService")!;
const actionCategories: PageActionCategory[] = actionCategoryService.list();
</script>
<template>
  <x-menu :disabled="disabled">
    <template #activator="{}">
      <x-btn size="small" :disabled="disabled">
        <c-icon name="three-dots-vertical" :size="Size.Small"></c-icon>
      </x-btn>
    </template>
    <page-actions-category
      v-for="(category, i) in actionCategories"
      :key="category.id"
      :category="category"
      :current-page="currentPage"
      :current-page-name="currentPageName"
      :is-last="i == actionCategories.length - 1"
    ></page-actions-category>
  </x-menu>
</template>
<style scoped></style>
