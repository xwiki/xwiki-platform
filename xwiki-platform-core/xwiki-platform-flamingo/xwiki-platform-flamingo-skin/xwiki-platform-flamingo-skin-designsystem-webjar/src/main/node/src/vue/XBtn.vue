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
import { computed } from "vue";
import type { BtnProps } from "@xwiki/platform-dsapi";
const { size, variant, pill } = defineProps<BtnProps>();
// TODO: implement emit
defineEmits(["click"]);

if (pill) {
  console.warn("Pill parameter is unsupported");
}

const computedSize = computed(() => {
  if (size === "small") {
    return ["btn-sm"];
  } else {
    return [];
  }
});

const computedVariant = computed(() => {
  switch (variant) {
    case "default":
      return "btn-default";
    case "primary":
      return "btn-primary";
    case "success":
      return "btn-success";
    case "neutral":
      return "btn-default";
    case "warning":
      return "btn-warning";
    case "danger":
      return "btn-danger";
    case "text":
      return "btn-link";
  }
  return "btn-default";
});

const classes = computed(() => {
  return ["btn", ...computedSize.value, computedVariant.value];
});
</script>

<template>
  <button :class="classes" @click="$emit('click')">
    <slot />
  </button>
</template>

<style scoped></style>
