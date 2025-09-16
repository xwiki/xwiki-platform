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
import { Date } from "@xwiki/cristal-date-ui";
import { FileSize } from "@xwiki/cristal-file-preview-ui";
import { AttachmentReference, EntityType } from "@xwiki/cristal-model-api";
import { User } from "@xwiki/cristal-user-ui";
import { computed, inject } from "vue";
import { useI18n } from "vue-i18n";
import type { CristalApp } from "@xwiki/cristal-api";
import type { Attachment } from "@xwiki/cristal-attachments-api";
import type { ClickListener } from "@xwiki/cristal-model-click-listener";
import type { ModelReferenceParserProvider } from "@xwiki/cristal-model-reference-api";

const { t } = useI18n({
  messages,
});

const props = defineProps<{
  attachments: Attachment[];
  errorMessage?: string;
  isLoading: boolean;
}>();

const cristal = inject<CristalApp>("cristal")!;
const listener = cristal.getContainer().get<ClickListener>("ClickListener");
const modelReferenceParser = cristal
  .getContainer()
  .get<ModelReferenceParserProvider>("ModelReferenceParserProvider")
  .get()!;

function attachmentPreview(url: string, event: Event) {
  event.preventDefault();
  listener.handleURL(url);
}

// Check if at least one attachment has an author, though this is likely true that if one attachment does not have an
// author, none of them do.
const hasAuthor = computed(() => {
  return (
    props.attachments.filter((attachment) => attachment.author !== undefined)
      .length > 0
  );
});

function attachmentName(name: string) {
  try {
    return (
      modelReferenceParser.parse(name, {
        type: EntityType.ATTACHMENT,
      }) as AttachmentReference
    ).name;
  } catch {
    return "";
  }
}
</script>
<template>
  <span v-if="isLoading" class="str-loading">
    {{ t("attachments.tab.loading") }}
  </span>
  <span v-else-if="errorMessage" class="str-error">{{ errorMessage }}</span>
  <span v-else-if="attachments.length == 0" class="str-no-attachment">
    {{ t("attachments.tab.noAttachments") }}
  </span>
  <table v-else class="mobile-transform">
    <thead>
      <tr>
        <th>{{ t("attachments.tab.table.header.name") }}</th>
        <th>{{ t("attachments.tab.table.header.mimetype") }}</th>
        <th>{{ t("attachments.tab.table.header.size") }}</th>
        <th>{{ t("attachments.tab.table.header.date") }}</th>
        <th v-if="hasAuthor">{{ t("attachments.tab.table.header.author") }}</th>
      </tr>
    </thead>
    <tbody>
      <tr v-for="attachment in attachments" :key="attachment.id">
        <td>
          <span class="mobile-column-name">
            {{ t("attachments.tab.table.header.name") }}
          </span>
          <a
            :href="attachment.href"
            @click="attachmentPreview(attachment.href, $event)"
          >
            {{ attachmentName(attachment.name) }}
          </a>
        </td>
        <td>
          <span class="mobile-column-name">
            {{ t("attachments.tab.table.header.mimetype") }}
          </span>
          {{ attachment.mimetype }}
        </td>
        <td>
          <span class="mobile-column-name">
            {{ t("attachments.tab.table.header.size") }}
          </span>
          <span class="mobile-column-name">
            {{ t("attachments.tab.table.header.size") }}
          </span>
          <span class="mobile-column-name">
            {{ t("attachments.tab.table.header.size") }}
          </span>
          <file-size :size="attachment.size"></file-size>
        </td>
        <td>
          <span class="mobile-column-name">
            {{ t("attachments.tab.table.header.date") }}
          </span>
          <span class="mobile-column-name">
            {{ t("attachments.tab.table.header.date") }}
          </span>
          <span class="mobile-column-name">
            {{ t("attachments.tab.table.header.date") }}
          </span>
          <date :date="attachment.date"></date>
        </td>
        <td v-if="hasAuthor">
          <span class="mobile-column-name">
            {{ t("attachments.tab.table.header.author") }}
          </span>
          <user v-if="attachment.author" :user="attachment.author"></user>
        </td>
      </tr>
    </tbody>
  </table>
</template>
<style scoped>
.v-card-text .str-no-attachment {
  padding: 0 var(--cr-spacing-medium);
}
table {
  margin: 0 var(--cr-spacing-medium);
}
</style>
