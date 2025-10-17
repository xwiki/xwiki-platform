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
import { name as documentServiceName } from "@xwiki/cristal-document-api";
import { CIcon } from "@xwiki/cristal-icons";
import { inject, ref } from "vue";
import { useI18n } from "vue-i18n";
import type { CristalApp, PageData } from "@xwiki/cristal-api";
import type { StorageProvider } from "@xwiki/cristal-backend-api";
import type { DocumentService } from "@xwiki/cristal-document-api";
import type {
  DocumentReference,
  SpaceReference,
} from "@xwiki/cristal-model-api";
import type {
  ModelReferenceHandler,
  ModelReferenceHandlerProvider,
  ModelReferenceSerializer,
  ModelReferenceSerializerProvider,
} from "@xwiki/cristal-model-reference-api";
import type { Ref } from "vue";

const cristal = inject<CristalApp>("cristal")!;
const container = cristal.getContainer();

const referenceHandler: ModelReferenceHandler = container
  .get<ModelReferenceHandlerProvider>("ModelReferenceHandlerProvider")
  .get()!;
const referenceSerializer: ModelReferenceSerializer = container
  .get<ModelReferenceSerializerProvider>("ModelReferenceSerializerProvider")
  .get()!;
const documentService = container.get<DocumentService>(documentServiceName);
const storage = container.get<StorageProvider>("StorageProvider").get();

const dialogOpen: Ref<boolean> = ref(false);
const name: Ref<string> = ref("");
const namePlaceholder: Ref<string> = ref("");
const location: Ref<SpaceReference | undefined> = ref(undefined);
const existingPage: Ref<PageData | undefined> = ref(undefined);
let newDocumentReferenceString: string = "";

defineProps<{
  currentPageReference?: DocumentReference;
}>();

const { t } = useI18n({
  messages,
});

function updateCurrentPage() {
  namePlaceholder.value = cristal.getWikiConfig().getNewPageDefaultName();
  name.value = "";
  existingPage.value = undefined;
}

// eslint-disable-next-line max-statements
async function createPage() {
  const newDocumentName = name.value ? name.value : namePlaceholder.value;
  const newDocumentReference = referenceHandler.createDocumentReference(
    newDocumentName,
    location.value!,
  );
  newDocumentReferenceString =
    referenceSerializer.serialize(newDocumentReference)!;

  existingPage.value = await cristal.getPage(newDocumentReferenceString);

  const titlePlaceholder = referenceHandler.getTitle(newDocumentReference);

  if (!existingPage.value) {
    await storage.save(
      newDocumentReferenceString,
      titlePlaceholder,
      "",
      "html",
    );

    await documentService.setCurrentDocument(
      newDocumentReferenceString,
      "edit",
    );
    await documentService.notifyDocumentChange("update", newDocumentReference);

    await cristal.setCurrentPage(newDocumentReferenceString, "edit");

    dialogOpen.value = false;
  }
}

async function editExistingPage() {
  dialogOpen.value = false;
  await cristal.setCurrentPage(newDocumentReferenceString, "edit");
}
</script>

<template>
  <x-dialog
    v-model="dialogOpen"
    width="auto"
    :title="t('page.creation.menu.title')"
  >
    <template #activator="{ activatorProps }">
      <x-btn
        id="new-page-button"
        size="small"
        color="secondary"
        @click="updateCurrentPage"
      >
        <c-icon name="plus" v-bind="activatorProps"></c-icon>
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
                        params: { page: newDocumentReferenceString },
                      }).href
                    "
                    >{{ newDocumentReferenceString }}</a
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
                        params: { page: newDocumentReferenceString },
                      }).href
                    "
                    >{{ newDocumentReferenceString }}</a
                  >
                </template>
                <template #link>
                  <a
                    :href="
                      cristal.getRouter().resolve({
                        name: 'edit',
                        params: { page: newDocumentReferenceString },
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
        <x-form id="page-creation-form" @form-submit="createPage">
          <x-text-field
            v-model="name"
            :placeholder="namePlaceholder"
            :label="t('page.creation.menu.field.name')"
            name="name"
            :help="t('page.creation.menu.field.name.help')"
            autofocus
          ></x-text-field>
          <XNavigationTreeSelect
            v-model="location"
            :label="t('page.creation.menu.field.location')"
            :help="t('page.creation.menu.field.location.help')"
            :current-page-reference="currentPageReference"
          ></XNavigationTreeSelect>
        </x-form>
      </div>
    </template>
    <template #footer>
      <x-btn @click.stop="dialogOpen = false">
        {{ t("page.creation.menu.cancel") }}
      </x-btn>
      <x-btn variant="primary" type="submit" form="page-creation-form">
        {{ t("page.creation.menu.submit") }}
      </x-btn>
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

.grid {
  display: grid;
  gap: 0.5rem;
  grid-auto-columns: 1fr;
  grid-template-columns: 1fr;
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
