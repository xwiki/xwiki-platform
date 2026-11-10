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
import { useLinkTargetTypeLabels } from "./linkTargetTypeLabels";
import { translations } from "../translations";
import { computed, nextTick, onMounted, ref, useId, useTemplateRef } from "vue";
import { useI18n } from "vue-i18n";
import type { LinkData, LinkTargetType } from "@xwiki/platform-link-type-api";

const props = defineProps<{
  linkData: LinkData;

  /**
   * The text that the link gets when its display text is left empty, computed from its target.
   */
  defaultDisplayText?: string;

  /**
   * The link target types offered by the link type selector, in the order they are offered.
   */
  linkTargetTypes: LinkTargetType[];
}>();

defineSlots<{
  /**
   * The field designating the target of the link, specific to the selected link target type.
   */
  target(): void;

  /**
   * The fields setting the options of the link that are specific to the selected link target type.
   */
  options(): void;
}>();

// TODO: use modern localization system (server-side)
const { t, locale } = useI18n({ messages: translations });

const linkData = ref(props.linkData);

// The link type selector identifies the link target types by their type, the labels are only displayed. Selecting
// another type resets the target to that type's default configuration.
const linkTargetType = computed({
  get: () => linkData.value.target.type,
  set: (type: string) => {
    const targetType = props.linkTargetTypes.find(
      (candidate) => candidate.type === type,
    );

    if (targetType) {
      linkData.value.target = {
        type: targetType.type,
        config: targetType.createDefaultConfig(),
      };
    }
  },
});

const labels = useLinkTargetTypeLabels(props.linkTargetTypes, locale);

const linkTargetTypeItems = computed(() =>
  props.linkTargetTypes.map((targetType) => ({
    value: targetType.type,
    label: labels.value[targetType.type] ?? targetType.type,
  })),
);

const optionsLabelId = useId();

const targetField = useTemplateRef("targetField");

onMounted(() => {
  // The target field is the one that matters when creating a link, the others being optional or preset. The link modal
  // asks it to mark its main input with autofocus, for the dialogs that move the focus to the element asking for it
  // once their opening animation is done. The input can be missing at this point, when the design system loads its
  // components asynchronously: it's then left to these dialogs.
  // The modal can open while a rich text editor still holds focus (e.g. after picking this action from a slash
  // menu), and that editor may reclaim focus synchronously right after opening it. Deferring past that with a
  // double rAF (matching the settle delay used in this component's tests) ensures our focus request runs last.
  nextTick(() => {
    requestAnimationFrame(() => {
      requestAnimationFrame(() => {
        targetField.value?.querySelector<HTMLElement>("[autofocus]")?.focus();
      });
    });
  });
});
</script>

<template>
  <!-- NOTE: 'v-bind' is used here as it is more flexible
             'data-*' attributes would not be allowed due to not being present in `BtnProps` -->
  <!-- The link type is required, but it's not marked as such since it always has a value: the selector has no empty
       option. Assistive technologies are still told. -->
  <x-select
    v-bind="{ 'data-test': 'linkTargetType', 'aria-required': 'true' }"
    :label="t('link-modal.config.target-type')"
    v-model="linkTargetType"
    :items="linkTargetTypeItems"
  />

  <!-- Holds the target field, to focus it: it's provided by the selected link target type. -->
  <div ref="targetField" class="target-field">
    <slot name="target" />
  </div>

  <!-- The options are grouped in a box, to make it clear that the summary toggles the fields that follow it. -->
  <!-- NOTE: 'v-bind' is used because the strict template checks reject the data-* attributes of native elements. -->
  <details class="options" v-bind="{ 'data-test': 'linkOptions' }">
    <summary :id="optionsLabelId">{{ t("link-modal.config.options") }}</summary>

    <!-- The summary names the group of option fields, since a legend can't be shared with it. -->
    <fieldset class="options-fields" :aria-labelledby="optionsLabelId">
      <!-- NOTE: 'v-bind' is used here as it is more flexible
                 'data-*' attributes would not be allowed due to not being present in `BtnProps` -->
      <x-text-field
        v-bind="{ 'data-test': 'linkDisplayText' }"
        :label="t('link-modal.config.display-text')"
        :help="t('link-modal.config.display-text.hint')"
        :placeholder="defaultDisplayText"
        v-model="linkData.displayText"
      />

      <slot name="options" />
    </fieldset>
  </details>
</template>

<style scoped>
/* The options are boxed like a form field, using the variables of the skin when it defines them (e.g. XWiki), and
   the ones of the design system otherwise. */
.options {
  border: var(--cr-input-border-width) solid
    var(--input-border, var(--cr-input-border-color));
  border-radius: var(--border-radius-base, var(--cr-border-radius-medium));
  padding: var(--cr-spacing-x-large)
    var(--padding-base-horizontal, var(--cr-spacing-large));
}

/* The target field is laid out as if it was a direct child of the form. */
.target-field {
  display: contents;
}

.options-fields {
  /* Reset the default style of the fieldset: the box is drawn around the whole details element. */
  border: 0;
  padding: 0;
  min-width: 0;
  /* Separate the fields from the summary as they are separated from each other by the form style, i.e. by a line. */
  margin: 1lh 0 0;
}

summary {
  cursor: pointer;
  user-select: none;
  /* Explicitly set the display to list item because it can be overridden in some context. */
  display: list-item;
}
</style>
