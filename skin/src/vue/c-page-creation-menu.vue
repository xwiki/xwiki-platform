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
import { CIcon } from "@xwiki/cristal-icons";
import { DocumentReference, SpaceReference } from "@xwiki/cristal-model-api";
import { defineProps, inject, ref } from "vue";
import type { CristalApp, PageData } from "@xwiki/cristal-api";
import type {
  ModelReferenceSerializer,
  ModelReferenceSerializerProvider,
} from "@xwiki/cristal-model-reference-api";
import type { NavigationTreeNode } from "@xwiki/cristal-navigation-tree-api";
import type { Ref } from "vue";

const cristal: CristalApp = inject<CristalApp>("cristal")!;

const referenceSerializer: ModelReferenceSerializer = cristal
  .getContainer()
  .get<ModelReferenceSerializerProvider>("ModelReferenceSerializerProvider")
  .get()!;

const dialogOpen: Ref<boolean> = ref(false);
const name: Ref<string> = ref("");
const location: Ref<string> = ref("");
var locationReference: SpaceReference | undefined = undefined;

defineProps<{
  currentPage: PageData;
}>();

function treeNodeClickAction(node: NavigationTreeNode) {
  locationReference = node.location;
  location.value = referenceSerializer.serialize(locationReference)!;
}

function updateCurrentPage() {
  name.value = cristal.getWikiConfig().getNewPageDefaultName();
}

function createPage() {
  const newDocumentReference = new DocumentReference(
    name.value,
    locationReference!,
  );

  cristal.setCurrentPage(
    referenceSerializer.serialize(newDocumentReference)!,
    "edit",
  );

  dialogOpen.value = false;
}
</script>

<template>
  <x-dialog v-model="dialogOpen" width="auto" title="New Page">
    <template #activator="{ props }">
      <x-btn
        id="new-page-button"
        size="small"
        variant="secondary"
        color="secondary"
        @click="updateCurrentPage"
      >
        <c-icon name="plus" v-bind="props"></c-icon>
        New Page
      </x-btn>
    </template>
    <template #default>
      <div id="new-page-content" class="grid">
        <x-form class="subgrid">
          <x-text-field
            v-model="name"
            label="Name"
            name="name"
            required
          ></x-text-field>
          <div>
            <label>Parent Location</label>
            <div id="new-page-navigation-tree" class="location-box">
              <XNavigationTree
                :click-action="treeNodeClickAction"
                :current-page="currentPage"
              ></XNavigationTree>
              <x-text-field
                v-model="location"
                label="Location"
                name="location"
                required
              ></x-text-field>
            </div>
          </div>
        </x-form>
      </div>
      <x-btn slot="footer" @click="createPage">Create</x-btn>
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
  overflow: auto;
}
.location-box {
  border: 1px solid #ddd;
  border-radius: var(--cr-border-radius-medium);
  padding: var(--cr-spacing-small);
}

.grid {
  display: grid;
  gap: 0.5rem;
  grid-auto-columns: 1fr;
  grid-template-columns: 1fr;
}
.subgrid {
  display: grid;
  grid-template-columns: subgrid;
  grid-column: 1 / 1;
  gap: 0.5rem;
}
</style>
