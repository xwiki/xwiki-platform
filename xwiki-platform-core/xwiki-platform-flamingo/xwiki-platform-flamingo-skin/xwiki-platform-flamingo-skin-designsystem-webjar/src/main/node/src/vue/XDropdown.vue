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
import XBtn from "./XBtn.vue";
import { dropdownKey } from "./inject/keys";
import { onMounted, provide, useId, useTemplateRef } from "vue";
import type { DropdownProps } from "@xwiki/platform-dsapi";

const rootId = useId();

// Define a context to let children know they are in a dropdown. This is useful for the menu element that needs to
// behave differently when in a dropdown.
provide(dropdownKey, { inDropdown: true, activatorId: rootId });

const toggle = useTemplateRef("toggle");

defineProps<DropdownProps>();
defineSlots<{ default(): void; activator(): void }>();

const jQuery: Promise<JQueryStatic> = new Promise((resolve) => {
  // requiring bootstrap is needed to be able to access the modal method once the component is mounted.
  // eslint-disable-next-line @typescript-eslint/no-require-imports
  require(["jquery", "bootstrap"], ($: JQueryStatic) => resolve($));
});

onMounted(async () => {
  const $ = await jQuery;
  // @ts-expect-error - bootstrap dropdown not typed on JQuery
  $(toggle.value!).dropdown();
});
</script>

<template>
  <div class="dropdown">
    <x-btn
      :disabled="disabled"
      :id="rootId"
      ref="toggle"
      class="dropdown-toggle"
      data-toggle="dropdown"
      aria-haspopup="true"
      aria-expanded="false"
      v-bind="btnProps"
    >
      <slot name="activator"></slot>
      &nbsp;<span class="caret"></span>
    </x-btn>
    <slot v-if="!disabled"></slot>
  </div>
</template>

<style scoped></style>
