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
import { inject, watch } from "vue";
import { CristalApp } from "@xwiki/cristal-api";
import { AttachmentsService } from "@xwiki/cristal-attachments-api";
import { useI18n } from "vue-i18n";
import messages from "../translations";
import { useRoute } from "vue-router";

const { t } = useI18n({
  messages,
});

const cristal = inject<CristalApp>("cristal")!;
const attachmentsService = cristal
  .getContainer()
  .get<AttachmentsService>("AttachmentsService")!;

const attachments = attachmentsService.list();
const isLoading = attachmentsService.isLoading();
const errorMessage = attachmentsService.getError();

// Watch for route change to refresh the tab when a user visits a new page.
const route = useRoute();
watch(
  () => route.params.page,
  () => attachmentsService.refresh(route.params.page as string),
  { immediate: true },
);
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

<style scoped></style>
