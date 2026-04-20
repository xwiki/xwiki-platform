<!--
  See the NOTICE file distributed with this work for additional
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
import AttachmentConfig from "./linkTypes/AttachmentConfig.vue";
import EmailConfig from "./linkTypes/EmailConfig.vue";
import PageConfig from "./linkTypes/PageConfig.vue";
import UrlConfig from "./linkTypes/UrlConfig.vue";
import { createLinkEditionContext } from "../linkSuggest";
import { translations } from "../translations";
import { typedRef } from "../utils";
import { inject, provide } from "vue";
import { useI18n } from "vue-i18n";
import type { LinkData } from "../data/linkType";
import type { CristalApp } from "@xwiki/platform-api";

const props = defineProps<{
  current: LinkData;
}>();

const { t } = useI18n({ messages: translations });

const cristal = inject<CristalApp>("cristal")!;
const container = cristal.getContainer();
provide("linkEditionCtx", createLinkEditionContext(container));

const linkData = typedRef(props.current);

defineEmits<{ submit: [LinkData]; cancel: [] }>();
</script>

<template>
  <div class="container">
    <url-config
      v-if="linkData.target.type === 'url'"
      v-model="linkData.target.config"
      :link-data="linkData"
    />

    <page-config
      v-if="linkData.target.type === 'page'"
      v-model="linkData.target.config"
      :link-data="linkData"
    />

    <attachment-config
      v-if="linkData.target.type === 'attachment'"
      v-model="linkData.target.config"
      :link-data="linkData"
    />

    <email-config
      v-if="linkData.target.type === 'email'"
      v-model="linkData.target.config"
      :link-data="linkData"
    />

    <div class="actions">
      <x-btn variant="success" @click="$emit('submit', linkData)">
        {{ t("link-modal.buttons.submit") }}
      </x-btn>

      <x-btn variant="neutral" @click="$emit('cancel')">
        {{ t("link-modal.buttons.cancel") }}
      </x-btn>
    </div>
  </div>
</template>

<style scoped>
.container {
  position: absolute;
  border: 1px solid black;
  border-radius: 0.5rem;
  background: white /*rgba(255, 255, 255, 0.8)*/;
  padding: 5px;
  z-index: 99;
}

.actions {
  padding: 1rem;
  display: flex;
  justify-content: end;
  gap: 0.5rem;
}
</style>
