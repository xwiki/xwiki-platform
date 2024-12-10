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
import { inject, onMounted, ref, shallowRef } from "vue";
import type { CristalApp, PageData } from "@xwiki/cristal-api";
import type {
  PageAction,
  PageActionCategory,
  PageActionService,
} from "@xwiki/cristal-page-actions-api";
import type { Component, Ref, ShallowRef } from "vue";

const props = defineProps<{
  category: PageActionCategory;
  currentPage: PageData | undefined;
  currentPageName: string;
  isLast: boolean;
}>();

const cristal: CristalApp = inject<CristalApp>("cristal")!;
const actionService: PageActionService = cristal
  .getContainer()
  .get<PageActionService>("PageActionService")!;

const actions: Ref<{ action: PageAction; component: ShallowRef<Component> }[]> =
  ref([]);

onMounted(async () => {
  for (const currAction of actionService.list(props.category.id)) {
    actions.value.push({
      action: currAction,
      component: shallowRef(await currAction.component()),
    });
  }
});
</script>
<template>
  <x-menu-label>{{ category.title }}</x-menu-label>
  <x-menu-item
    v-for="action of actions"
    :key="action.action.id"
    :value="action.action.id"
  >
    <component
      :is="action.component"
      :current-page="currentPage"
      :current-page-name="currentPageName"
    ></component>
  </x-menu-item>
  <x-divider v-if="!isLast" />
</template>
<style scoped></style>
