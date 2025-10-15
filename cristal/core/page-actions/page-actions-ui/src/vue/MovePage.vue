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
import { CIcon, Size } from "@xwiki/cristal-icons";
import { inject, ref } from "vue";
import { useI18n } from "vue-i18n";
import type { AlertsService } from "@xwiki/cristal-alerts-api";
import type { CristalApp, PageData } from "@xwiki/cristal-api";
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
import type {
  PageRenameManager,
  PageRenameManagerProvider,
} from "@xwiki/cristal-rename-api";
import type { Ref } from "vue";

const { t } = useI18n({
  messages,
});
const props = defineProps<{
  currentPage: PageData | undefined;
  currentPageName: string;
  currentPageReference: DocumentReference;
}>();

const cristal: CristalApp = inject<CristalApp>("cristal")!;
const alertsService: AlertsService = cristal
  .getContainer()
  .get<AlertsService>("AlertsService")!;
const documentService = cristal
  .getContainer()
  .get<DocumentService>("DocumentService");
const pageRenameManager: PageRenameManager = cristal
  .getContainer()
  .get<PageRenameManagerProvider>("PageRenameManagerProvider")
  .get();
const referenceHandler: ModelReferenceHandler = cristal
  .getContainer()
  .get<ModelReferenceHandlerProvider>("ModelReferenceHandlerProvider")
  .get()!;
const referenceSerializer: ModelReferenceSerializer = cristal
  .getContainer()
  .get<ModelReferenceSerializerProvider>("ModelReferenceSerializerProvider")
  .get()!;

const dialogOpen: Ref<boolean> = ref(false);
const preserveChildren: Ref<boolean> = ref(true);
const existingPage: Ref<PageData | undefined> = ref(undefined);
const locationReference: Ref<SpaceReference | undefined> = ref(undefined);
let newDocumentReference: DocumentReference;
let newDocumentReferenceSerialized: string;

async function movePage() {
  newDocumentReference = referenceHandler.createDocumentReference(
    props.currentPageReference.name,
    locationReference.value!,
  );
  newDocumentReferenceSerialized =
    referenceSerializer.serialize(newDocumentReference)!;

  existingPage.value = await cristal.getPage(newDocumentReferenceSerialized);
  if (!existingPage.value) {
    const result = await pageRenameManager.updateReference(
      props.currentPage!,
      newDocumentReferenceSerialized,
      preserveChildren.value,
    );
    dialogOpen.value = false;
    await handleSuccess(result);
  }
}

async function handleSuccess(result: { success: boolean; error?: string }) {
  if (result.success) {
    if (preserveChildren.value) {
      documentService.notifyDocumentChange(
        "delete",
        props.currentPageReference,
      );
    }
    await cristal.setCurrentPage(newDocumentReferenceSerialized, "view");
    alertsService.success(
      t("page.action.action.move.page.success", {
        page: props.currentPageName,
        newPage: newDocumentReferenceSerialized,
      }),
    );
    documentService.notifyDocumentChange("update", newDocumentReference);
  } else {
    alertsService.error(
      t("page.action.action.move.page.error", {
        page: props.currentPageName,
        reason: result.error!,
      }),
    );
  }
}
</script>

<template>
  <x-dialog
    v-model="dialogOpen"
    width="auto"
    :title="t('page.action.action.move.page.dialog.title')"
  >
    <template #activator>
      <c-icon name="box-arrow-in-right" :size="Size.Small"></c-icon>
      {{ t("page.action.action.move.page.title") }}
    </template>
    <template #default>
      <x-alert v-if="existingPage !== undefined" type="error">
        <i18n-t keypath="page.action.action.move.page.alert.content" tag="span">
          <template #pageName>
            <a
              :href="
                cristal.getRouter().resolve({
                  name: 'view',
                  params: { page: newDocumentReferenceSerialized },
                }).href
              "
              >{{ newDocumentReference }}</a
            >
          </template>
        </i18n-t>
      </x-alert>
      <div class="dialog-content">
        <x-form id="page-move-form" @form-submit="movePage">
          <XNavigationTreeSelect
            v-model="locationReference"
            :label="t('page.action.action.move.page.location.label')"
            :help="t('page.action.action.move.page.location.help')"
            :current-page-reference="currentPageReference"
          ></XNavigationTreeSelect>
          <XCheckbox
            v-model="preserveChildren"
            :label="t('page.action.action.move.page.preserve.children.label')"
            :help="t('page.action.action.move.page.preserve.children.help')"
          ></XCheckbox>
        </x-form>
      </div>
    </template>
    <template #footer>
      <x-btn @click.stop="dialogOpen = false">
        {{ t("page.action.action.move.page.cancel") }}
      </x-btn>
      <x-btn variant="primary" type="submit" form="page-move-form">
        {{ t("page.action.action.move.page.title") }}
      </x-btn>
    </template>
  </x-dialog>
</template>

<style scoped>
.dialog-content {
  min-width: 600px;
}
</style>
