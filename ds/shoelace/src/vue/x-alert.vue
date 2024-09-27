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
import "@shoelace-style/shoelace/dist/components/alert/alert";
import { computed, type PropType } from "vue";

type Types = "" | "success" | "warning" | "error" | "info";

const { type } = defineProps({
  title: { type: String, required: true },
  type: {
    type: String as PropType<Types>,
    required: true,
  },
  description: { type: String, required: true },
});

const variant = computed(() => {
  let variant: string;
  switch (type) {
    case "":
      variant = "primary";
      break;
    case "success":
      variant = "success";
      break;
    case "warning":
      variant = "warning";
      break;
    case "error":
      variant = "danger";
      break;
    case "info":
      variant = "primary";
      break;
    default:
      variant = "primary";
      break;
  }
  return variant;
});
</script>

<template>
  <sl-alert open :title="title" :variant="variant">
    {{ description }}
  </sl-alert>
</template>
