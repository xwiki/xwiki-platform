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
<script setup lang="ts">
import "@shoelace-style/shoelace/dist/components/alert/alert";
import "@shoelace-style/shoelace/dist/components/icon/icon";
import { CIcon } from "@xwiki/cristal-icons";
import { computed } from "vue";
import type { AlertProps } from "@xwiki/cristal-dsapi";

const { type } = defineProps<AlertProps>();

const variant = computed(() => {
  let variant: string;
  let icon: string;
  switch (type) {
    case "success":
      variant = "success";
      icon = "check2-circle";
      break;
    case "warning":
      variant = "warning";
      icon = "exclamation-triangle";
      break;
    case "error":
      variant = "danger";
      icon = "exclamation-octagon";
      break;
    case "info":
      variant = "primary";
      icon = "info-circle";
      break;
  }
  return { variant, icon };
});

const open = defineModel<boolean>();
open.value = true;
</script>

<template>
  <sl-alert
    :closable="closable"
    :variant="variant.variant"
    :open="open"
    @sl-show="open = true"
    @sl-hide="open = false"
  >
    <c-icon slot="icon" :name="variant.icon"></c-icon>
    <strong v-if="title">{{ title }}</strong>
    <br v-if="title" />
    {{ description }}
    <x-btn
      v-for="action of actions"
      :key="action.name"
      size="small"
      variant="text"
      @click="action.callback"
      >{{ action.name }}</x-btn
    >
    <slot />
  </sl-alert>
</template>
