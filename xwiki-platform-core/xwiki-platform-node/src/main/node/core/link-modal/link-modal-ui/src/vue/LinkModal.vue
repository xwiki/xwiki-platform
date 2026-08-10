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
import { createLinkEditionContext } from "../linkSuggest.js";
import { translations } from "../translations";
import { typedRef } from "../utils";
import { listEnabledLinkTargetTypeExtensions } from "@xwiki/platform-link-modal-api";
import { computed, markRaw, provide, ref, shallowRef, watchEffect } from "vue";
import { useI18n } from "vue-i18n";
import type {
  LinkData,
  LinkTargetTypeExtension,
} from "@xwiki/platform-link-modal-api";
import type { Container } from "inversify";
import type { Component } from "vue";

const props = defineProps<{
  current: LinkData;
  depsContainer: Container;
}>();

const { t } = useI18n({ messages: translations });

provide("linkEditionCtx", createLinkEditionContext(props.depsContainer));

const linkData = typedRef(props.current);

// The list of registered, enabled link target types (built-in and 3rd-party), resolved from the same
// `depsContainer` used for every other domain service (see `createLinkEditionContext` above), and shared with
// `LinkConfig.vue` (rendered nested inside whichever configuration component is active below) so it can build
// the link type selector from it.
const extensions = ref<LinkTargetTypeExtension[]>([]);
const extensionsLoading = ref(true);

provide("linkTargetTypeExtensions", extensions);

(async () => {
  try {
    extensions.value = await listEnabledLinkTargetTypeExtensions(
      props.depsContainer,
    );
  } catch (e) {
    console.error("Failed to resolve the registered link target types", e);
  } finally {
    extensionsLoading.value = false;
  }
})();

// The configuration component for the currently selected link target type, loaded lazily (mirrors
// `LivedataDisplayer.vue`'s own resolve-then-`markRaw()` pattern for the same reason: dynamically registered
// components must be excluded from Vue's reactivity tracking).
const activeConfigComponent = shallowRef<Component | null>(null);
const activeConfigLoading = ref(false);

// eslint-disable-next-line max-statements
watchEffect(async () => {
  if (extensionsLoading.value) {
    return;
  }

  const type = linkData.value.target.type;
  const extension = extensions.value.find((e) => e.type === type);

  if (!extension) {
    // The extension providing this type is not (or no longer) registered/enabled: nothing to render.
    activeConfigComponent.value = null;
    return;
  }

  activeConfigLoading.value = true;
  const component = await extension.component();

  // Guard against a race with a later invocation of this same watchEffect: only apply the result if the
  // selected type hasn't changed again while `component()` was loading.
  if (linkData.value.target.type === type) {
    activeConfigComponent.value = markRaw(component);
    activeConfigLoading.value = false;
  }
});

const ready = computed(
  () =>
    !extensionsLoading.value &&
    !activeConfigLoading.value &&
    activeConfigComponent.value !== null,
);

defineEmits<{ submit: [LinkData]; cancel: [] }>();
</script>

<template>
  <div :class="$style.container">
    <p v-if="!ready" :class="$style.loading">
      {{ t("link-modal.loading") }}
    </p>

    <component
      v-else
      :is="activeConfigComponent"
      v-model="linkData.target.config"
      :link-data="linkData"
    />

    <div :class="$style.actions">
      <!-- NOTE: 'v-bind' is used here as it is more flexible
                 'data-*' attributes would not be allowed due to not being present in `BtnProps` -->
      <x-btn
        v-bind="{ 'data-test': 'linkSubmit' }"
        variant="success"
        @click="$emit('submit', linkData)"
      >
        {{ t("link-modal.buttons.submit") }}
      </x-btn>

      <x-btn
        v-bind="{ 'data-test': 'linkCancel' }"
        variant="neutral"
        @click="$emit('cancel')"
      >
        {{ t("link-modal.buttons.cancel") }}
      </x-btn>
    </div>
  </div>
</template>

<style module>
.container {
  display: flex;
  flex-direction: column;
  gap: var(--cr-spacing-large);
  position: absolute;
  border: var(--cr-input-border-width) solid var(--cr-input-border-color);
  border-radius: var(--cr-border-radius-large);
  background: var(--cr-color-neutral-50);
  padding: var(--cr-spacing-medium);
  z-index: 99;
}

.loading {
  font-style: italic;
  color: var(--cr-color-neutral-500);
  margin: 0;
}

.actions {
  padding: var(--cr-spacing-large);
  display: flex;
  justify-content: end;
  gap: var(--cr-spacing-medium);
}
</style>
