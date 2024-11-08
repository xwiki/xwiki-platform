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
import { ExtraTab, ExtraTabsService } from "@xwiki/cristal-extra-tabs-api";
import { Ref, ShallowRef, inject, onBeforeMount, ref, shallowRef } from "vue";
import type { CristalApp } from "@xwiki/cristal-api";
import type { Component } from "vue";

const cristal = inject<CristalApp>("cristal")!;

const extraTabsService: ExtraTabsService = cristal
  .getContainer()
  .get<ExtraTabsService>("ExtraTabsService")!;

const list: ExtraTab[] = await extraTabsService.list();

// The record needs to be reactive for the template to detect changes when a
// new panel is loaded.
// But, the Components used as values must be wrapped in shallow refs for
// performance reason (it's too costly to monitor them in depth).
const loadedTabs: Ref<Record<string, ShallowRef<Component>>> = ref({});

async function load(extraTab: ExtraTab) {
  if (extraTab && !loadedTabs.value[extraTab.id]) {
    loadedTabs.value[extraTab.id] = shallowRef(await extraTab.panel());
  }
}

function change(tabId: string) {
  load(list.find((tab) => tab.id == tabId)!);
}

onBeforeMount(async () => {
  if (list.length > 0) {
    await load(list[0]);
  }
});
</script>

<template>
  <x-tab-group class="tab-group" @change="change">
    <template #tabs>
      <x-tab v-for="tab in list" :key="tab.id" :tab-id="tab.id">
        {{ tab.title }}
      </x-tab>
    </template>
    <template #panels>
      <x-tab-panel v-for="tab in list" :key="tab.id" :tab-id="tab.id">
        <component
          :is="loadedTabs[tab.id]"
          v-if="loadedTabs[tab.id]"
        ></component>
        <span v-else>Loading...</span>
      </x-tab-panel>
    </template>
  </x-tab-group>
</template>

<style scoped>
.tab-group {
  width: 100%;
}
</style>
