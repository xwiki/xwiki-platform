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
import { CIcon, Size } from "@xwiki/cristal-icons";
import { ConfigurationsSettings } from "@xwiki/cristal-settings-configurations";
import { inject } from "vue";
import type { CristalApp, WikiConfig } from "@xwiki/cristal-api";
import type { SettingsManager } from "@xwiki/cristal-settings-api";
import type { WikiConfigProxy } from "@xwiki/cristal-wiki-config-api";
import type { Ref } from "vue";

const cristal = inject<CristalApp>("cristal")!;
const wikiConfigProxy = cristal
  .getContainer()
  .get<WikiConfigProxy>("WikiConfigProxy")!;
const configs: Ref<Map<string, WikiConfig>> =
  wikiConfigProxy.getAvailableConfigurations();
const settingsManager = cristal
  .getContainer()
  .get<SettingsManager>("SettingsManager")!;

const currentConfig = cristal.getWikiConfig().name;

function isEditable(configName: string): boolean {
  return (
    (configName != currentConfig &&
      settingsManager.get(ConfigurationsSettings)?.content?.has(configName)) ??
    false
  );
}

defineEmits(["edit", "delete"]);
</script>
<template>
  <div>
    <div class="grid-container">
      <div v-for="[key, wikiConfig] in configs" :key="key">
        <div>
          <div class="wiki-name">
            {{ wikiConfig.name }}
            <span v-if="wikiConfig.offline" class="offline">Offline</span>
          </div>
          <div v-if="wikiConfig.designSystem != ''" class="ds-name">
            Design System: <strong>{{ wikiConfig.designSystem }}</strong>
          </div>
          <div class="url">{{ wikiConfig.baseURL }}</div>
        </div>
        <x-btn
          v-if="isEditable(wikiConfig.name)"
          variant="primary"
          @click="$parent!.$emit('edit', wikiConfig.name)"
        >
          <c-icon name="pencil" :size="Size.Small"></c-icon>
        </x-btn>
        <div v-else></div>
        <x-btn
          v-if="isEditable(wikiConfig.name)"
          variant="danger"
          @click="$parent!.$emit('delete', wikiConfig.name)"
        >
          <c-icon name="trash" :size="Size.Small"></c-icon>
        </x-btn>
        <div v-else></div>
        <div v-if="wikiConfig.name == currentConfig" class="current-ds">
          Currently in Use
        </div>
        <x-btn v-else @click="cristal?.switchConfig(wikiConfig.name)">
          Open
        </x-btn>
      </div>
    </div>
  </div>
</template>
<style scoped>
.wiki-name {
  font-weight: var(--cr-font-weight-bold);
}

.url {
  font-weight: var(--cr-font-weight-semi-bold);
  color: var(--cr-color-neutral-500);
  font-size: var(--cr-font-size-small);
  overflow-wrap: break-word;
  line-height: var(--cr-font-size-small);
  width: 100%;
}

.ds-name {
  font-size: var(--cr-font-size-small);
}

.current-ds {
  font-size: var(--cr-font-size-small);
  text-align: center;
}

p {
  padding: 0;
  margin: 0;
}

.grid-container {
  display: grid;
  grid-template-columns: 1fr;
  align-items: center;
}

.grid-container > * {
  display: grid;
  gap: var(--cr-spacing-x-small);
  align-items: center;
  justify-content: center;
  grid-template-columns: 1fr max-content max-content 150px;
  border-bottom: 1px solid #ddd;
  padding: var(--cr-spacing-x-small) var(--cr-spacing-2x-small);
}

.grid-container > *:last-child {
  border-bottom: 0;
}
</style>
