<script setup lang="ts">
import messages from "../translations";
import { Attachment } from "@xwiki/cristal-attachments-api";
import { Date } from "@xwiki/cristal-date-ui";
import { FileSize } from "@xwiki/cristal-file-preview-ui";
import { User } from "@xwiki/cristal-user-ui";
import { computed, inject } from "vue";
import { useI18n } from "vue-i18n";
import type { CristalApp } from "@xwiki/cristal-api";
import type { ClickListener } from "@xwiki/cristal-model-click-listener";

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
</script>
<template>
  <span v-if="isLoading">{{ t("attachments.tab.loading") }}</span>
  <span v-else-if="errorMessage">{{ errorMessage }}</span>
  <span v-else-if="attachments.length == 0">
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
            >{{ attachment.name }}</a
          >
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
          <span class="mobile-column-name">{{ t("attachments.tab.table.header.size") }}</span><span class="mobile-column-name">{{ t("attachments.tab.table.header.size") }}</span><file-size :size="attachment.size"></file-size>
        </td>
        <td>
          <span class="mobile-column-name">
            {{ t("attachments.tab.table.header.date") }}
          </span>
          <span class="mobile-column-name">{{ t("attachments.tab.table.header.date") }}</span><span class="mobile-column-name">{{ t("attachments.tab.table.header.date") }}</span><date :date="attachment.date"></date>
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
