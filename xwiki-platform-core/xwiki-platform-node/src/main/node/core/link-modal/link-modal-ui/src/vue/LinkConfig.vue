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
import { translations } from "../translations";
import { ref, watch } from "vue";
import { useI18n } from "vue-i18n";
import type { LinkData, LinkTarget } from "../data/linkType";

const props = defineProps<{ linkData: LinkData }>();

defineSlots<{
  config(): void;
  options(): void;
}>();

// TODO: use modern localization system (server-side)
const { t } = useI18n({ messages: translations });

const targetTypes: { label: string; default: LinkTarget }[] = [
  {
    label: t("link-modal.target-types.page.label"),
    default: { type: "page", config: { ref: null } },
  },
  {
    label: t("link-modal.target-types.url.label"),
    default: { type: "url", config: { url: "" } },
  },
  {
    label: t("link-modal.target-types.attachment.label"),
    default: { type: "attachment", config: { ref: null } },
  },
  {
    label: t("link-modal.target-types.email.label"),
    default: { type: "email", config: { address: "" } },
  },
];

const linkData = ref(props.linkData);

const linkTargetTypeSelect = ref(
  targetTypes.find((c) => c.default.type === linkData.value.target.type)!.label,
);

watch(linkTargetTypeSelect, (label) => {
  const targetType = targetTypes.find((c) => c.label === label)!;
  linkData.value.target = targetType.default;
});
</script>

<template>
  <!-- NOTE: 'v-bind' is used here as it is more flexible
             'data-*' attributes would not be allowed due to not being present in `BtnProps` -->
  <x-text-field
    v-bind="{ 'data-test': 'linkDisplayText' }"
    :label="t('link-modal.config.display-text')"
    v-model="linkData.displayText"
    required
  />

  <x-select
    v-bind="{ 'data-test': 'linkTargetType' }"
    :label="t('link-modal.config.target-type')"
    v-model="linkTargetTypeSelect"
    :items="targetTypes.map((t) => t.label)"
    required
  />

  <slot name="config" />

  <details>
    <summary>{{ t("link-modal.config.options") }}</summary>

    <x-checkbox
      :label="t('link-modal.config.open-new-tab')"
      v-model="linkData.newTab"
    />

    <slot name="options" />
  </details>
</template>

<style scoped>
summary {
  cursor: pointer;
  user-select: none;
  /* Explicitly set the display to list item because it can be overridden in some context. */
  display: list-item;
}
</style>
