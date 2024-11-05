<script setup lang="ts">
import messages from "../translations";
import { useI18n } from "vue-i18n";
import { Attachment } from "@xwiki/cristal-attachments-api";

const { t } = useI18n({
  messages,
});

defineProps<{
  attachments: Attachment[];
  errorMessage?: string;
  isLoading: boolean;
}>();
</script>
<template>
  <span v-if="isLoading">{{ t("attachments.tab.loading") }}</span>
  <span v-else-if="errorMessage">{{ errorMessage }}</span>
  <span v-else-if="attachments.length == 0">
    {{ t("attachments.tab.noAttachments") }}
  </span>
  <table v-else>
    <thead>
      <tr>
        <th>{{ t("attachments.tab.table.header.name") }}</th>
        <th>{{ t("attachments.tab.table.header.mimetype") }}</th>
      </tr>
    </thead>
    <tbody>
      <tr v-for="attachment in attachments" :key="attachment.id">
        <td>
          <a :href="attachment.href">{{ attachment.name }}</a>
        </td>
        <td>{{ attachment.mimetype }}</td>
      </tr>
    </tbody>
  </table>
</template>
