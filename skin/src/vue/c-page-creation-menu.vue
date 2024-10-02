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
import { type Ref, defineProps, inject, ref } from "vue";
import type { CristalApp, PageData } from "@xwiki/cristal-api";
import type {
  NavigationTreeNode,
  NavigationTreeSourceProvider,
} from "@xwiki/cristal-navigation-tree-api";
import { CIcon } from "@xwiki/cristal-icons";

const cristal: CristalApp = inject<CristalApp>("cristal")!;

const dialogOpen: Ref<boolean> = ref(false);
const name: Ref<string> = ref("");
const location: Ref<string> = ref("");

defineProps<{
  currentPage: PageData;
}>();

function treeNodeClickAction(node: NavigationTreeNode) {
  location.value = node.location;
}

function updateCurrentPage() {
  name.value = cristal.getWikiConfig().getNewPageDefaultName();
}

function createPage() {
  var newPage = "";

  // TODO: Use a page resolver instead when CRISTAL-234 is fixed.
  const pageResourceSeparator =
    cristal.getWikiConfig().getType() == "XWiki" ? "." : "/";

  if (location.value) {
    newPage += location.value + pageResourceSeparator;
  }
  newPage += name.value;

  cristal.setCurrentPage(newPage, "edit");

  dialogOpen.value = false;
}
</script>

<template>
  <x-dialog v-model="dialogOpen" width="auto" title="New Page">
    <template #activator="{ props }">
      <span id="new-page-button" @click="updateCurrentPage">
        <c-icon name="plus" v-bind="props"></c-icon>
        <strong>New Page</strong>
      </span>
    </template>
    <template #default>
      <div id="new-page-content">
        <x-form>
          <x-text-field
            v-model="name"
            label="Name"
            name="name"
            required
          ></x-text-field>
          <x-text-field
            v-model="location"
            label="Location"
            name="location"
            required
          ></x-text-field>
        </x-form>
        <div id="new-page-navigation-tree">
          <XNavigationTree
            :tree-source="
              cristal
                .getContainer()
                .get<NavigationTreeSourceProvider>(
                  'NavigationTreeSourceProvider',
                )
                .get()
            "
            :click-action="treeNodeClickAction"
            :current-page="currentPage"
          ></XNavigationTree>
        </div>
        <x-btn @click="createPage">Create</x-btn>
      </div>
    </template>
  </x-dialog>
</template>

<style scoped>
#new-page-button {
  cursor: pointer;
}
#new-page-content {
  min-width: 600px;
}
#new-page-navigation-tree {
  height: 400px;
  border: thin solid var(--cr-color-neutral-200);
  margin-bottom: 1rem;
  overflow: auto;
}
</style>
