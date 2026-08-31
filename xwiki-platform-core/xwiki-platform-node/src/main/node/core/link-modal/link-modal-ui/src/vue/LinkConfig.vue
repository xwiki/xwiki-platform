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
import { inject, ref, watch } from "vue";
import { useI18n } from "vue-i18n";
import type {
  LinkData,
  LinkTargetTypeExtension,
} from "@xwiki/platform-link-type-api";

const props = defineProps<{ linkData: LinkData }>();

defineSlots<{
  config(): void;
  options(): void;
}>();

// TODO: use modern localization system (server-side)
const { t, locale } = useI18n({ messages: translations });

// Provided by `LinkModal.vue`: the list of registered, enabled link target types, already resolved from the
// shared component manager — see `@xwiki/platform-link-type-api`'s `LinkTargetTypeExtension`.
const extensions = inject<LinkTargetTypeExtension[]>(
  "linkTargetTypeExtensions",
)!;

const linkData = ref(props.linkData);

const linkTargetTypeSelect = ref(
  extensions
    .find((extension) => extension.type === linkData.value.target.type)
    ?.getLabel(locale.value) ?? "",
);

watch(linkTargetTypeSelect, (label) => {
  const extension = extensions.find(
    (extension) => extension.getLabel(locale.value) === label,
  );

  if (extension) {
    linkData.value.target = {
      type: extension.type,
      config: extension.createDefaultConfig(),
    };
  }
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
    :items="extensions.map((extension) => extension.getLabel(locale))"
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
