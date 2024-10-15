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
import { type Component, inject } from "vue";
import { CristalApp } from "@xwiki/cristal-api";
import { UIExtensionsManager } from "@xwiki/cristal-uiextension-api";
//
const { uixName } = defineProps<{
  uixName: string;
}>();

const cristal = inject<CristalApp>("cristal")!;

const uixManager: UIExtensionsManager = cristal
  .getContainer()
  .get<UIExtensionsManager>("UIExtensionsManager")!;

const uiExtensions: { id: string; component: Component }[] = [];
for (let uiExtension of await uixManager.list(uixName)) {
  uiExtensions.push({
    id: uiExtension.id,
    component: await uiExtension.component(),
  });
}
</script>

<template>
  <component
    :is="uix.component"
    v-for="uix in uiExtensions"
    :key="uix.id"
  ></component>
</template>
