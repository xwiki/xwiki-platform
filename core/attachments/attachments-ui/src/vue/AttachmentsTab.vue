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
import AttachmentUploadForm from "./AttachmentUploadForm.vue";
import AttachmentsTable from "./AttachmentsTable.vue";
import { AlertsService } from "@xwiki/cristal-alerts-api";
import { CristalApp } from "@xwiki/cristal-api";
import { AttachmentsService } from "@xwiki/cristal-attachments-api";
import { inject, ref, watch } from "vue";
import { useRoute } from "vue-router";

const cristal = inject<CristalApp>("cristal")!;
const attachmentsService = cristal
  .getContainer()
  .get<AttachmentsService>("AttachmentsService")!;

const attachments = attachmentsService.list();
const isLoading = attachmentsService.isLoading();
const errorMessage = attachmentsService.getError();
const isUploading = attachmentsService.isUploading();

// Watch for route change to refresh the tab when a user visits a new page.
const route = useRoute();
watch(
  () => route.params.page,
  () =>
    attachmentsService.refresh(
      (route.params.page as string) ||
        cristal.getCurrentPage() ||
        "Main.WebHome",
    ),
  { immediate: true },
);

const attachmentUpload = ref();

const alertsService = cristal
  .getContainer()
  .get<AlertsService>("AlertsService")!;

async function upload(files: File[]) {
  try {
    await attachmentsService.upload(
      (route.params.page as string) ||
        cristal.getCurrentPage() ||
        ("Main.WebHome" as string),
      files,
    );
    attachmentUpload.value?.reset();
  } catch (e) {
    if (e instanceof Error) {
      alertsService.error(e.message);
    }
  }
}
</script>

<template>
  <div class="attachments">
    <AttachmentUploadForm
      ref="attachmentUpload"
      :is-uploading="isUploading"
      @files-selected="upload"
    ></AttachmentUploadForm>
    <AttachmentsTable
      :attachments="attachments"
      :error-message="errorMessage"
      :is-loading="isLoading"
    />
  </div>
</template>
<style scoped>
.attachments {
  display:flex;
  flex-flow: column;
  gap:16px;
}
</style>
