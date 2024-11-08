<!--
See the LICENSE file distributed with this work for additional
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
<script lang="ts" setup>
import { computed } from "vue";
import type { BtnProps } from "@xwiki/cristal-dsapi";

const { variant } = defineProps<BtnProps>();

// Map the variants to corresponding vuetify colors.
const color = computed(() => {
  let color = undefined;
  switch (variant) {
    case "default":
      break;
    case "primary":
      color = "primary";
      break;
    case "success":
      color = "success";
      break;
    case "neutral":
      color = "secondary";
      break;
    case "warning":
      color = "warning";
      break;
    case "danger":
      color = "error";
      break;
  }
  return color;
});

const vVariant = computed(() => (variant == "text" ? "text" : "tonal"));
</script>
<template>
  <v-btn :color="color" :variant="vVariant">
    <slot />
  </v-btn>
</template>
