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
import UnsupportedLinkTargetField from "./UnsupportedLinkTargetField.vue";
import { plainTextFallbackField } from "./plainTextFallbackField";
import { UnsupportedLinkTargetType } from "../UnsupportedLinkTargetType";
import { linkTargetTypeFormRole } from "../linkTargetTypeForm";
import { translations } from "../translations";
import { typedRef } from "../utils";
import {
  linkTargetTypeManagerRole,
  unsupportedLinkTargetType,
} from "@xwiki/platform-link-type-api";
import { computed, markRaw, ref, useId, watch } from "vue";
import { useI18n } from "vue-i18n";
import type { LinkTargetTypeForm } from "../linkTargetTypeForm";
import type {
  LinkContext,
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

  /**
   * Whether the modal creates a new link or edits an existing one, which sets its title and the label of its submit
   * button.
   * Default: createNew.
   * @since 18.10.0RC1
   */
  mode?: "createNew" | "editExisting";

  /**
   * Where the link is (e.g. the document holding it), passed to the target fields so that they display and parse the
   * references relative to it.
   * @since 18.10.0RC1
   */
  linkContext?: LinkContext;
}>();

const { t } = useI18n({ messages: translations });

// The display text field is left empty for a link whose text is generated from its target, so that the text follows
// the target (the field shows the generated text as placeholder).
const linkData = typedRef(
  props.current.generatedDisplayText
    ? { ...props.current, displayText: "" }
    : props.current,
);

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

// The forms of the link target types, registered separately from the link target types themselves so that these don't
// depend on the UI framework of the link modal.
const linkTargetTypeForms: LinkTargetTypeForm[] = [
  {
    type: unsupportedLinkTargetType,
    targetField: () => UnsupportedLinkTargetField,
  },
  ...(props.depsContainer.isBound(linkTargetTypeFormRole)
    ? props.depsContainer.getAll<LinkTargetTypeForm>(linkTargetTypeFormRole)
    : []),
];

// The form of the currently selected link target type, with its components resolved, or a fallback form with a plain
// text target field for a link target type registered without a form. `markRaw()` excludes its components from Vue's
// reactivity tracking (mirrors `LivedataDisplayer.vue`'s own use of `markRaw()` for the same reason, for a dynamically
// registered component).
const activeForm = computed(() => {
  const type = linkData.value.target.type;
  const form = linkTargetTypeForms.find((form) => form.type === type);
  if (form) {
    const optionFields = form.optionFields?.();
    return {
      targetField: markRaw(form.targetField()),
      optionFields: optionFields ? markRaw(optionFields) : undefined,
    };
  }

  const targetType = linkTargetTypes.find(
    (candidate) => candidate.type === type,
  );

  // The link target type is not (or no longer) registered/enabled: nothing to render.
  return targetType
    ? { targetField: markRaw(plainTextFallbackField(targetType)) }
    : undefined;
});

// The text that the link gets when its display text is left empty. The link target types that don't compute a label
// get their URL, like XWiki Rendering does for the links without a label.
const defaultDisplayText = computed(() => {
  const { type, config } = linkData.value.target;
  const targetType = linkTargetTypes.find(
    (candidate) => candidate.type === type,
  );
  if (!targetType) {
    return "";
  }
  return targetType.getDefaultLabel
    ? targetType.getDefaultLabel(config)
    : targetType.toURL(config);
});

const emit = defineEmits<{ submit: [LinkData]; cancel: [] }>();

function submit() {
  // An empty display text means that the text of the link is generated from its target.
  const generatedDisplayText = linkData.value.displayText.trim() === "";
  emit("submit", {
    ...linkData.value,
    displayText: generatedDisplayText
      ? defaultDisplayText.value
      : linkData.value.displayText,
    generatedDisplayText,
  });
}

// The submit button is displayed in the footer of the dialog, outside of the form, which it designates by its id.
const formId = useId();

// The modal is shown as soon as it is mounted, and it's up to its parent to unmount it once submitted or cancelled.
// Closing the dialog otherwise (e.g. with the Escape key or its close button) cancels the link edition.
const open = ref(true);
watch(open, (isOpen) => {
  if (!isOpen) {
    emit("cancel");
  }
});
</script>

<template>
  <!-- The dialog is persistent so that a click outside of it, e.g. back in the edited content, doesn't discard the
       link being edited. -->
  <x-dialog
    v-model="open"
    :title="
      t(
        mode === 'editExisting'
          ? 'link-modal.title.edit'
          : 'link-modal.title.insert',
      )
    "
    persistent
  >
    <!-- The form gives the fields the form style of the design system, and is submitted only once they are valid (e.g.
         the required ones are filled), otherwise the dialog stays open, showing what's invalid. -->
    <x-form :id="formId" :class="$style.container" @form-submit="submit">
      <!-- The fields shared by all the link target types, with the target and option fields of the selected one. -->
      <LinkForm
        :link-data="linkData"
        :link-target-types="offeredLinkTargetTypes"
        :default-display-text="defaultDisplayText"
      >
        <template #target>
          <!-- Identifies the target field, whatever the selected link target type. -->
          <div class="link-target">
            <component
              v-if="activeForm"
              :is="activeForm.targetField"
              v-model="linkData.target.config"
              :deps-container
              :link-context
              autofocus
            />
          </div>
        </template>

        <template #options>
          <component
            v-if="activeForm?.optionFields"
            :is="activeForm.optionFields"
            v-model="linkData.target.config"
            :deps-container
          />
        </template>
      </LinkForm>
    </x-form>

    <template #footer>
      <div :class="$style.actions">
        <!-- NOTE: 'v-bind' is used here as it is more flexible
                   'data-*' attributes would not be allowed due to not being present in `BtnProps` -->
        <x-btn
          v-bind="{ 'data-test': 'linkCancel' }"
          variant="neutral"
          @click="emit('cancel')"
        >
          {{ t("link-modal.buttons.cancel") }}
        </x-btn>

        <x-btn
          v-bind="{ 'data-test': 'linkSubmit' }"
          variant="primary"
          type="submit"
          :form="formId"
        >
          {{
            t(
              mode === "editExisting"
                ? "link-modal.buttons.update"
                : "link-modal.buttons.insert",
            )
          }}
        </x-btn>
      </div>
    </template>
  </x-dialog>
</template>

<style module>
.container {
  display: flex;
  flex-direction: column;
  gap: var(--cr-spacing-large);
}

.actions {
  display: flex;
  justify-content: end;
  gap: var(--cr-spacing-medium);
}
</style>
