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
import LinkForm from "./LinkForm.vue";
import UnsupportedLinkTargetForm from "./UnsupportedLinkTargetForm.vue";
import { UnsupportedLinkTargetType } from "../UnsupportedLinkTargetType";
import { createLinkEditionContext } from "../linkEditionContext";
import { linkTargetTypeFormRole } from "../linkTargetTypeForm";
import { plainTextFallbackForm } from "../plainTextFallbackForm";
import { translations } from "../translations";
import { typedRef } from "../utils";
import {
  linkTargetTypeManagerRole,
  unsupportedLinkTargetType,
} from "@xwiki/platform-link-type-api";
import { computed, markRaw, provide } from "vue";
import { useI18n } from "vue-i18n";
import type { LinkTargetTypeForm } from "../linkTargetTypeForm";
import type {
  LinkData,
  LinkTargetType,
  LinkTargetTypeManager,
  UnsupportedLinkTargetConfig,
} from "@xwiki/platform-link-type-api";
import type { Container } from "inversify";

const props = defineProps<{
  current: LinkData;
  depsContainer: Container;

  /**
   * The link target types (by type) that the link type selector doesn't offer, e.g. because their links are rarely
   * created from the link modal. They are still used to edit the links they handle, and offered when the edited link
   * is of an excluded type. Unknown types are ignored.
   */
  excludedLinkTargetTypes?: string[];
}>();

const { t } = useI18n({ messages: translations });

provide("linkEditionCtx", createLinkEditionContext(props.depsContainer));

const linkData = typedRef(props.current);

// When the edited link points to a resource that no registered link target type supports, its own link target type is
// offered first, so that the user can keep editing the reference as plain text, or switch back to it (restoring the
// original reference) after selecting another type.
const linkTargetTypes: LinkTargetType[] = [
  ...(props.current.target.type === unsupportedLinkTargetType
    ? [
        new UnsupportedLinkTargetType(
          props.current.target.config as UnsupportedLinkTargetConfig,
        ),
      ]
    : []),
  ...props.depsContainer
    .get<LinkTargetTypeManager>(linkTargetTypeManagerRole)
    .list(),
];

// The link target types offered by the link type selector, which always include the type of the edited link, so that
// the user can switch back to it after selecting another type: this list is computed once, from the type of the edited
// link when the modal is opened.
const initialLinkTargetType = props.current.target.type;
const offeredLinkTargetTypes = linkTargetTypes.filter(
  (targetType) =>
    targetType.type === initialLinkTargetType ||
    !props.excludedLinkTargetTypes?.includes(targetType.type),
);

provide("linkTargetTypes", offeredLinkTargetTypes);

// The forms of the link target types, registered separately from the link target types themselves so that these don't
// depend on the UI framework of the link modal.
const linkTargetTypeForms: LinkTargetTypeForm[] = [
  {
    type: unsupportedLinkTargetType,
    component: () => UnsupportedLinkTargetForm,
  },
  ...(props.depsContainer.isBound(linkTargetTypeFormRole)
    ? props.depsContainer.getAll<LinkTargetTypeForm>(linkTargetTypeFormRole)
    : []),
];

// The form of the currently selected link target type, or a plain text form for a link target type registered without
// a form. `markRaw()` excludes its components from Vue's reactivity tracking (mirrors `LivedataDisplayer.vue`'s own use
// of `markRaw()` for the same reason, for a dynamically registered component).
const activeForm = computed(() => {
  const type = linkData.value.target.type;
  const form = linkTargetTypeForms.find((form) => form.type === type);
  if (form) {
    const options = form.optionsComponent?.();
    return {
      component: markRaw(form.component()),
      optionsComponent: options ? markRaw(options) : undefined,
    };
  }

  const targetType = linkTargetTypes.find(
    (candidate) => candidate.type === type,
  );

  // The link target type is not (or no longer) registered/enabled: nothing to render.
  return targetType
    ? { component: markRaw(plainTextFallbackForm(targetType)) }
    : undefined;
});

defineEmits<{ submit: [LinkData]; cancel: [] }>();
</script>

<template>
  <div :class="$style.container">
    <!-- The fields shared by all the link target types, with the fields and options of the selected one. -->
    <LinkForm :link-data="linkData">
      <template #config>
        <component
          v-if="activeForm"
          :is="activeForm.component"
          v-model="linkData.target.config"
          :link-data="linkData"
        />
      </template>

      <template #options>
        <component
          v-if="activeForm?.optionsComponent"
          :is="activeForm.optionsComponent"
          v-model="linkData.target.config"
          :link-data="linkData"
        />
      </template>
    </LinkForm>

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
