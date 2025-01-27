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
import { defineProps, inject, ref } from "vue";
import { useI18n } from "vue-i18n";
import type { CristalApp, PageData } from "@xwiki/cristal-api";
import type { SpaceReference } from "@xwiki/cristal-model-api";
import type {
  ModelReferenceHandler,
  ModelReferenceHandlerProvider,
  ModelReferenceSerializer,
  ModelReferenceSerializerProvider,
} from "@xwiki/cristal-model-reference-api";
import type { NavigationTreeNode } from "@xwiki/cristal-navigation-tree-api";
import type { Ref } from "vue";

const cristal: CristalApp = inject<CristalApp>("cristal")!;

const referenceHandler: ModelReferenceHandler = cristal
  .getContainer()
  .get<ModelReferenceHandlerProvider>("ModelReferenceHandlerProvider")
  .get()!;
const referenceSerializer: ModelReferenceSerializer = cristal
  .getContainer()
  .get<ModelReferenceSerializerProvider>("ModelReferenceSerializerProvider")
  .get()!;

const dialogOpen: Ref<boolean> = ref(false);
const name: Ref<string> = ref("");
const namePlaceholder: Ref<string> = ref("");
const location: Ref<string> = ref("");
const existingPage: Ref<PageData | undefined> = ref(undefined);
var locationReference: SpaceReference | undefined = undefined;
let newDocumentReference: string = "";

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

async function createPage() {
  const newDocumentName = name.value ? name.value : namePlaceholder.value;
  newDocumentReference = referenceSerializer.serialize(
    referenceHandler.createDocumentReference(
      newDocumentName,
      locationReference!,
    ),
  )!;

  existingPage.value = await cristal.getPage(newDocumentReference);

  if (!existingPage.value) {
    cristal.setCurrentPage(newDocumentReference, "edit");

    dialogOpen.value = false;
  }
}

function editExistingPage() {
  dialogOpen.value = false;
  cristal.setCurrentPage(newDocumentReference, "edit");
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
        color="secondary"
        @click="updateCurrentPage"
      >
        <c-icon name="plus" v-bind="props"></c-icon>
        {{ t("page.creation.menu.button") }}
      </x-btn>
    </template>
    <template #default>
      <div id="new-page-content" class="grid">
        <!-- We need 2 different divs to implement the following behavior:
               - Use max available width from the parent
               - Do not resize the parent if the content is larger -->
        <div class="alerts-wrapper">
          <div class="alerts">
            <!-- Indicate that the selected page already exists. -->
            <x-alert v-if="existingPage !== undefined" type="error">
              <i18n-t
                v-if="!existingPage!.canEdit"
                keypath="page.creation.menu.alert.content"
                tag="span"
              >
                <template #pageName>
                  <a
                    :href="
                      cristal.getRouter().resolve({
                        name: 'view',
                        params: { page: newDocumentReference },
                      }).href
                    "
                    >{{ newDocumentReference }}</a
                  >
                </template>
              </i18n-t>
              <i18n-t
                v-else
                keypath="page.creation.menu.alert.content.edit"
                tag="span"
              >
                <template #pageName>
                  <a
                    :href="
                      cristal.getRouter().resolve({
                        name: 'view',
                        params: { page: newDocumentReference },
                      }).href
                    "
                    >{{ newDocumentReference }}</a
                  >
                </template>
                <template #link>
                  <a
                    :href="
                      cristal.getRouter().resolve({
                        name: 'edit',
                        params: { page: newDocumentReference },
                      }).href
                    "
                    @click.prevent="editExistingPage"
                    >{{ t("page.creation.menu.alert.content.edit.link") }}</a
                  >
                </template>
              </i18n-t>
            </x-alert>
          </div>
        </div>
        <x-form class="subgrid" @form-submit="createPage">
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
            </div>
          </div>
          <!-- This is a hidden button to enable submit events for Vuetify.
               We do not want to put the other button inside the form, we want
               to keep it in the footer instead. -->
          <input type="submit" />
        </x-form>
      </div>
    </template>
    <template #footer>
      <x-btn @click="createPage">{{ t("page.creation.menu.submit") }}</x-btn>
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

.alerts-wrapper {
  display: flex;
}
.alerts {
  flex-grow: 1;
  width: 0;
}
input[type="submit"] {
  display: none;
}
</style>
