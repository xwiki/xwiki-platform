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
import xBtn from "./XBtn.vue";
import { onMounted, useTemplateRef } from "vue";
import type { AlertProps } from "@xwiki/platform-dsapi";

type NotificationType = "done" | "error" | "warning" | "info";

const { type, closable, title, description, flatCorners } =
  defineProps<AlertProps>();

if (closable !== undefined) {
  console.warn("XAlert closable is not supported.");
}

function getVariant(): NotificationType {
  switch (type) {
    case "success":
      return "done";
    case "warning":
      return "warning";
    case "error":
      return "error";
    case "info":
      return "info";
  }
}

const alertContent = useTemplateRef("alertContent");

onMounted(() => {
  const value = alertContent.value!;
  const html = value.getHTML();

  // TODO: see how to make this non-intereactive to let users click on buttons.
  const notif = new XWiki.widgets.Notification(html, getVariant());

  if (flatCorners) {
    notif.element.children[0].style.setProperty("border-radius", 0);
  }
});
</script>

<template>
  <div v-show="false" ref="alertContent">
    <strong v-if="title">{{ title }}</strong>
    <br v-if="title" />
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
    <br />
    <slot />
  </div>
</template>

<style scoped></style>
