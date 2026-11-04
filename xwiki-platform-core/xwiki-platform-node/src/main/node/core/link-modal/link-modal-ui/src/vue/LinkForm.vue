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
import { copyUnsupportedLinkTarget } from "../unsupportedLinkTarget";
import { unsupportedLinkTargetType } from "@xwiki/platform-link-type-api";
import {
  computed,
  inject,
  nextTick,
  onMounted,
  ref,
  useTemplateRef,
  watch,
} from "vue";
import { useI18n } from "vue-i18n";
import type {
  LinkData,
  LinkTarget,
  LinkTargetTypeExtension,
  UnsupportedLinkTargetConfig,
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

// Provided by `LinkModal.vue` when the edited link points to a resource that no link target type supports.
const unsupportedLinkTarget = inject<LinkTarget | null>(
  "unsupportedLinkTarget",
  null,
);

const linkData = ref(props.linkData);

// The link type selector identifies the link target types by their type, the labels are only displayed. Selecting
// another type resets the target to that type's default configuration, or to the original target of the edited link
// when it points to a resource that no link target type supports.
const linkTargetType = computed({
  get: () => linkData.value.target.type,
  set: (type: string) => {
    if (type === unsupportedLinkTargetType && unsupportedLinkTarget) {
      linkData.value.target = copyUnsupportedLinkTarget(unsupportedLinkTarget);
      return;
    }

    const extension = extensions.find((extension) => extension.type === type);

    if (extension) {
      linkData.value.target = {
        type: extension.type,
        config: extension.createDefaultConfig(),
      };
    }
  },
});

// The labels are resolved asynchronously (an extension may fetch its label from the server), so the type is displayed
// until its label is resolved, or when it fails to resolve.
const labels = ref<Record<string, string>>({});

watch(
  locale,
  async (locale, _, onCleanup) => {
    let superseded = false;
    onCleanup(() => (superseded = true));

    const resolvedLabels = await Promise.all(
      extensions.map(async (extension) => [
        extension.type,
        await extension.getLabel(locale).catch(() => extension.type),
      ]),
    );

    // Ignore the labels resolved for a locale that changed in the meantime.
    if (!superseded) {
      labels.value = Object.fromEntries(resolvedLabels);
    }
  },
  { immediate: true },
);

const linkTargetTypeItems = computed(() => {
  const items = extensions.map((extension) => ({
    value: extension.type,
    label: labels.value[extension.type] ?? extension.type,
  }));

  // The type of a resource that no link target type supports has no translation, so it is displayed as is (e.g.
  // "interwiki").
  if (unsupportedLinkTarget) {
    const { reference } =
      unsupportedLinkTarget.config as UnsupportedLinkTargetConfig;
    items.unshift({ value: unsupportedLinkTargetType, label: reference.type });
  }

  return items;
});

const displayTextField = useTemplateRef("displayTextField");

onMounted(() => {
  // The modal can open while a rich text editor still holds focus (e.g. after picking this action from a slash
  // menu), and that editor may reclaim focus synchronously right after opening it. Deferring past that with a
  // double rAF (matching the settle delay used in this component's tests) ensures our focus request runs last.
  nextTick(() => {
    requestAnimationFrame(() => {
      requestAnimationFrame(() => {
        const el = displayTextField.value?.$el as HTMLElement | undefined;
        const focusable =
          el?.querySelector<HTMLElement>("input, textarea, select") ?? el;
        focusable?.focus();
      });
    });
  });
});
</script>

<template>
  <!-- NOTE: 'v-bind' is used here as it is more flexible
             'data-*' attributes would not be allowed due to not being present in `BtnProps` -->
  <x-text-field
    ref="displayTextField"
    v-bind="{ 'data-test': 'linkDisplayText' }"
    :label="t('link-modal.config.display-text')"
    v-model="linkData.displayText"
    required
  />

  <x-select
    v-bind="{ 'data-test': 'linkTargetType' }"
    :label="t('link-modal.config.target-type')"
    v-model="linkTargetType"
    :items="linkTargetTypeItems"
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
