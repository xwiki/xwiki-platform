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
import { Attachment, AttachmentPreview } from "@xwiki/cristal-attachments-api";
import { Date } from "@xwiki/cristal-date-ui";
import { FilePreview, FileSize } from "@xwiki/cristal-file-preview-ui";
import { User } from "@xwiki/cristal-user-ui";
import { Ref, inject, ref, watch } from "vue";
import { useI18n } from "vue-i18n";
import type { CristalApp } from "@xwiki/cristal-api";

const { t } = useI18n({
  messages,
});

const cristal: CristalApp = inject<CristalApp>("cristal")!;

const attachmentPreview = cristal
  .getContainer()
  .get<AttachmentPreview>("AttachmentPreview")!;

const openedDialog: Ref<boolean> = ref(false);

watch(
  () => {
    // Wait for the preview to be loading or with a value before opening the
    // modal.
    return (
      attachmentPreview.loading().value || attachmentPreview.attachment().value
    );
  },
  (open) => {
    if (open) {
      openedDialog.value = true;
    }
  },
);

function close() {
  openedDialog.value = false;
}

function download() {
  // TODO: make sure this is also working with electron somehow
  const value = attachment.value;
  if (value) {
    window.location.href = value.href;
  }
}

const attachment: Ref<Attachment | undefined> = attachmentPreview.attachment();

const loading = attachmentPreview.loading();
const error = attachmentPreview.error();
</script>

<template>
  <x-dialog v-model="openedDialog" :title="t('attachment.preview.modal.title')">
    <template #activator>
      <!-- No activator, the modal is opened when the user clicks on an attachment link. -->
      <span class="x-dialog-attachment-preview-no-activator"></span>
    </template>
    <template #default>
      <div v-if="loading">{{ t("attachment.preview.loading") }}</div>
      <div v-else-if="attachment" class="dialog_content">
        <!-- TODO: abstract to allow preview based on the type -->
        <div class="attachment_view">
          <div class="attachment_view_inner">
            <file-preview :attachment="attachment" />
          </div>
        </div>
        <div class="metadata">
          <!-- TODO make a component with key:value -->
          <div class="label_description">
            <div>
              <span class="label">
                {{ t("attachment.preview.name.label") }}
              </span>
              <span class="description">{{ attachment.name }}</span>
            </div>
          </div>
          <div v-if="attachment.author" class="label_description">
            <div>
              <span class="label">
                {{ t("attachment.preview.postedBy.label") }}
              </span>
              <span class="description">
                <user :user="attachment.author" />
              </span>
            </div>
          </div>
          <div class="label_description">
            <div>
              <span class="label">
                {{ t("attachment.preview.date.label") }}
              </span>
              <span class="description">
                <date :date="attachment.date" />
              </span>
            </div>
          </div>
          <div class="label_description">
            <div class="label_description">
              <div>
                <span class="label">
                  {{ t("attachment.preview.size.label") }}
                </span>
                <span class="description">
                  <file-size :size="attachment.size" />
                </span>
              </div>
            </div>
          </div>
          <div class="label_description">
            <div>
              <span class="label">
                {{ t("attachment.preview.type.label") }}
              </span>
              <span class="description">{{ attachment.mimetype }}</span>
            </div>
            <div class="label_description"></div>
          </div>
        </div>
        <div class="actions">
          <!-- TODO: top-right close button. -->
          <div class="main_action">
            <x-btn @click="download()">
              {{ t("attachment.preview.download.button") }}
            </x-btn>
          </div>
          <div class="close_container">
            <x-btn class="close" variant="primary" @click="close()">
              {{ t("attachment.preview.close.button") }}
            </x-btn>
          </div>
        </div>
      </div>
      <div v-else>
        <div v-if="error">{{ error }}</div>
        <div v-else>
          {{ t("attachment.preview.error.unknown") }}
        </div>
      </div>
    </template>
  </x-dialog>
</template>

<style scoped>
/* Prevent the dialog section of the modal to be displayed, to prevent click on it, which would show the popup without
an attachments on empty pages. */
:global(*:has(> .x-dialog-attachment-preview-no-activator)) {
  display: none;
}
.dialog_content {
  display: grid;
  height: 80vh;
  grid-auto-flow: column;
  grid-template-columns: 6fr 1fr;
  grid-template-rows: 1fr auto;
  gap: 16px 16px;
  grid-template-areas:
    "attachment_view metadata"
    "actions actions";
}

.metadata {
  grid-area: metadata;
  display: flex;
  flex-flow: column;
  gap: 8px;
}

.actions {
  grid-area: actions;
  display: grid;
  grid-template-columns: 1fr auto;
  gap: var(--cr-spacing-medium);

  .main_action {
    display: flex;
    gap: var(--cr-spacing-medium);
  }

  .close_container {
    justify-self: end;
  }
}

.attachment_view {
  background-color: var(--cr-color-neutral-100);
  border-radius: var(--cr-border-radius-large);
  grid-area: attachment_view;
  overflow: auto;
  display: flex;
  align-items: center;
  justify-content: center;
}

.attachment_view img {
  max-height: 70vh;
  display: block;
}

.label_description {
  display: flex;
  flex-flow: column;
  gap: 4px;

  .label {
    display: block;
    font-size: var(--cr-font-size-x-small);
  }

  .description {
    font-weight: var(--cr-font-weight-bold);
    font-size: var(--cr-font-size-small);
  }
}

@container xwCristal (max-width: 600px) {
  .dialog_content {
    display: grid;
    grid-template-columns: 1fr;
    grid-template-rows: 1fr;
    grid-template-areas:
      "attachment_view"
      "metadata"
      "actions";

    .attachment_view img {
      width: 100%;
    }
  }
}

/*TODO: Try to remove these Vuetify only styles*/

:global(.v-overlay) {
  container: vuetifyCristal;
  container-type: size;
}

@container vuetifyCristal (max-width: 600px) {
  .dialog_content {
    grid-template-columns: 1fr;
    grid-template-rows: 1fr;
    grid-template-areas:
      "attachment_view"
      "metadata"
      "actions";

    .attachment_view img {
      width: 100%;
    }
  }
}
</style>
