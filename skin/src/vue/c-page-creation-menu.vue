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
import messages from "../translations";
import { CIcon } from "@xwiki/cristal-icons";
import { DocumentReference, SpaceReference } from "@xwiki/cristal-model-api";
import { defineProps, inject, ref } from "vue";
import { useI18n } from "vue-i18n";
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
const namePlaceholder: Ref<string> = ref("");
const location: Ref<string> = ref("");
var locationReference: SpaceReference | undefined = undefined;

defineProps<{
  currentPage: PageData;
}>();

const { t } = useI18n({
  messages,
});

function treeNodeClickAction(node: NavigationTreeNode) {
  locationReference = node.location;
  location.value = referenceSerializer.serialize(locationReference)!;
}

function updateCurrentPage() {
  namePlaceholder.value = cristal.getWikiConfig().getNewPageDefaultName();
  name.value = "";
}

function createPage() {
  const newDocumentName = name.value ? name.value : namePlaceholder.value;
  const newDocumentReference = new DocumentReference(
    newDocumentName,
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
  <x-dialog
    v-model="dialogOpen"
    width="auto"
    :title="t('page.creation.menu.title')"
  >
    <template #activator="{ props }">
      <x-btn
        id="new-page-button"
        size="small"
        variant="secondary"
        color="secondary"
        @click="updateCurrentPage"
      >
        <c-icon name="plus" v-bind="props"></c-icon>
        {{ t("page.creation.menu.button") }}
      </x-btn>
    </template>
    <template #default>
      <div id="new-page-content" class="grid">
        <x-form class="subgrid">
          <x-text-field
            v-model="name"
            :placeholder="namePlaceholder"
            :label="t('page.creation.menu.field.name')"
            name="name"
            autofocus
            required
          ></x-text-field>
          <div>
            <label>{{ t("page.creation.menu.field.location") }}</label>
            <div id="new-page-navigation-tree" class="location-box">
              <XNavigationTree
                :click-action="treeNodeClickAction"
                :current-page="currentPage"
              ></XNavigationTree>
              <x-text-field
                v-model="location"
                :label="t('page.creation.menu.field.location')"
                name="location"
                required
              ></x-text-field>
            </div>
          </div>
        </x-form>
      </div>
      <x-btn slot="footer" @click="createPage">{{
        t("page.creation.menu.submit")
      }}</x-btn>
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
