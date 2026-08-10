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
import { computed, markRaw, provide } from "vue";
import { useI18n } from "vue-i18n";
import type { LinkData } from "@xwiki/platform-link-modal-api";
import type { Container } from "inversify";

const props = defineProps<{
  current: LinkData;
  depsContainer: Container;
}>();

const { t } = useI18n({ messages: translations });

provide("linkEditionCtx", createLinkEditionContext(props.depsContainer));

const linkData = typedRef(props.current);

// The list of registered, enabled link target types (built-in and 3rd-party), resolved synchronously from the
// same `depsContainer` used for every other domain service (see `createLinkEditionContext` above), and shared
// with `LinkConfig.vue` (rendered nested inside whichever configuration component is active below) so it can
// build the link type selector from it.
const extensions = computed(() =>
  listEnabledLinkTargetTypeExtensions(props.depsContainer),
);

provide("linkTargetTypeExtensions", extensions);

// The configuration component for the currently selected link target type. `markRaw()` excludes it from Vue's
// reactivity tracking (mirrors `LivedataDisplayer.vue`'s own use of `markRaw()` for the same reason, for a
// dynamically registered component).
const activeConfigComponent = computed(() => {
  const extension = extensions.value.find(
    (e) => e.type === linkData.value.target.type,
  );

  // The extension providing this type is not (or no longer) registered/enabled: nothing to render.
  return extension ? markRaw(extension.component()) : null;
});

defineEmits<{ submit: [LinkData]; cancel: [] }>();
</script>

<template>
  <div :class="$style.container">
    <component
      v-if="activeConfigComponent"
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

.actions {
  padding: var(--cr-spacing-large);
  display: flex;
  justify-content: end;
  gap: var(--cr-spacing-medium);
}
</style>
