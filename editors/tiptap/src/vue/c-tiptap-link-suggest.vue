<!--
 * See the LICENSE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 *
 * This file is part of the Cristal Wiki software prototype
 * @copyright  Copyright (c) 2023 XWiki SAS
 * @license    http://opensource.org/licenses/AGPL-3.0 AGPL-3.0
 *
 **/
-->
<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from "vue";

import tippy, { GetReferenceClientRect, Instance, Props } from "tippy.js";
import { storeToRefs } from "pinia";
import linkSuggestStore from "../stores/link-suggest-store";
import { listNavigation } from "./list-navigation-helper";
import { SuggestionProps } from "@tiptap/suggestion";

const container = ref();

defineProps<{
  hasSuggestService: boolean;
}>();

const { links, text, props } = storeToRefs(linkSuggestStore());

const piniaProps = computed(() => props.value as SuggestionProps<unknown>);

let popup: Instance<Props>[];

onMounted(() => {
  popup = tippy("body", {
    getReferenceClientRect: piniaProps.value
      .clientRect as GetReferenceClientRect,
    appendTo: () => document.body,
    content: container.value,
    showOnCreate: true,
    interactive: true,
    trigger: "manual",
    placement: "bottom-start",
  });
});

const isLinkValid = computed(() => {
  // TODO: currently all typed text are considered as valid but we'll need
  // to add some checks.
  return true;
});

const apply = (index: number) => {
    // We only submit the text as a link if it's a valid link
    if (index == 0 && isLinkValid.value) {
      piniaProps.value.editor
        .chain()
        .focus()
        .deleteRange(piniaProps.value.range)
        .setLink({ href: text.value })
        .command(({ tr }) => {
          tr.insertText(text.value);
          return true;
        })
        .run();
    } else {
      const link = links.value[index - 1];
      piniaProps.value.editor
        .chain()
        .focus()
        .deleteRange(piniaProps.value.range)
        .setLink({ href: link.url })
        .command(({ tr }) => {
          tr.insertText(link.title);
          return true;
        })
        .run();
    }
  },
  { down, up, enter, index } = listNavigation(
    apply,
    computed(() => {
      return links.value.length;
    }),
    container,
  );

onUnmounted(() => {
  popup[0].destroy();
});
</script>

<template>
  <!--
  Defines a root element that is not part of the tippy component.
  It's is useful as a receiver for keyboard events forwarded for the editor.
  -->
  <div @keydown.down="down" @keydown.up="up" @keydown.enter="enter">
    <!--
    This container elements is moved inside tippy and is used as the content of
    the tippy popover.
    -->
    <div ref="container" class="form">
      <button
        class="item"
        :class="[
          index == 0 ? 'is-selected' : '',
          isLinkValid ? '' : 'disabled',
        ]"
        :title="
          isLinkValid
            ? 'Insert the typed link'
            : 'Invalid link, can\'t be submitted'
        "
        @click="apply(0)"
      >
        Create link from text
      </button>
      <template v-if="hasSuggestService">
        <button
          v-for="(link, linkIndex) in links"
          :key="link.reference"
          :class="['item', index - 1 == linkIndex ? 'is-selected' : '']"
          @click="apply(linkIndex + 1)"
        >
          {{ link.title }}
          <XBreadcrumb :items="link.segments"></XBreadcrumb>
        </button>
      </template>
      <span v-else class="information"> No suggestion service available </span>
    </div>
  </div>
</template>

<style scoped>
.form {
  position: relative;
  border-radius: var(--cr-tooltip-border-radius);
  background: white;
  overflow: hidden auto;
  box-shadow:
    0 0 0 1px rgba(0, 0, 0, 0.1),
    0 10px 20px rgba(0, 0, 0, 0.1);
  max-height: 300px;
}

.item,
.information {
  display: block;
  background: transparent;
  border: none;
  padding: var(--cr-spacing-x-small);
  width: 100%;
  text-align: left;
}

.information {
  font-style: italic;
  color: var(--cr-color-neutral-300);
}

.item.disabled {
  color: var(--cr-color-neutral-300);
}

.item.is-selected,
.item:hover {
  background-color: var(--cr-color-neutral-200);
  cursor: pointer;
}
</style>
