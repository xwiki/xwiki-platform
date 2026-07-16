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
import { computed, ref } from "vue";
import type { AlertProps } from "@xwiki/platform-dsapi";

const isClosed = ref(false);

const { type, closable, title, description, flatCorners } =
  defineProps<AlertProps>();

const boxclass = computed(() => {
  switch (type) {
    case "success":
      return ["successmessage"];
    case "error":
      return ["errormessage"];
    case "warning":
      return ["warningmessage"];
    case "info":
      return ["infomessage"];
    default:
      return [];
  }
});

function close() {
  isClosed.value = true;
}
</script>

<template>
  <div
    v-if="!isClosed"
    :class="[
      'box',
      ...boxclass,
      flatCorners ? $style.flatCorners : '',
      ...(closable ? ['alert-dismissible', 'fade', 'in'] : []),
    ]"
    role="alert"
  >
    <!-- TODO: localize the aria-label once a translation API is available, see XWIKI-24046 -->
    <button
      v-if="closable"
      type="button"
      class="close"
      data-dismiss="alert"
      aria-label="Close"
      @click="close"
    >
      <span aria-hidden="true">&times;</span>
    </button>
    <strong v-if="title">{{ title }}</strong>
    <br v-if="title && description" />
    {{ description }}
    <x-btn
      v-for="action of actions"
      :key="action.name"
      size="small"
      variant="default"
      @click="action.callback"
    >
      {{ action.name }}
    </x-btn>
    <br v-if="details && actions" />
    <small v-if="details">{{ details }}</small>
    <br v-if="details" />
    <slot />
  </div>
</template>

<style module>
.flatCorners {
  border-radius: initial;
}
</style>
<style scoped>
.close {
  margin-right: 0.25em;
}
</style>
