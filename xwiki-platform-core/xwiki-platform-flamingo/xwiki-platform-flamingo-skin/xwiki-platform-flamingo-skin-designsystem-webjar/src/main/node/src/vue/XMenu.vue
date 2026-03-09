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
import { defineSlots, onMounted, useId, useTemplateRef } from "vue";

const rootId = useId();

// eslint-disable-next-line no-undef
const jQuery: Promise<JQuery> = new Promise((resolve) => {
  // requiring bootstrap is needed to be able to access the modal method once the component is mounted.
  // eslint-disable-next-line @typescript-eslint/no-require-imports,no-undef
  require(["jquery", "bootstrap"], ($: JQuery) => resolve($));
});

const toggle = useTemplateRef("toggle");

onMounted(async () => {
  const $ = await jQuery;
  const $1 = $(toggle.value);
  $1.children[0].dropdown();
});

async function initDropdown(element) {
  const $ = await jQuery;
  $(element).dropdown();
}
</script>

<template>
  <div class="dropdown">
    <div
      :id="rootId"
      ref="toggle"
      role="button"
      data-toggle="dropdown"
      aria-haspopup="true"
      aria-expanded="false"
    >
      <slot name="activator" :init-dropdown="initDropdown"></slot>
    </div>
    <ul class="dropdown-menu" :aria-labelledby="rootId">
      <slot></slot>
    </ul>
  </div>
</template>

<style scoped></style>
