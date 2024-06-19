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
<script lang="ts">
import type { CristalApp, WikiConfig } from "@xwiki/cristal-api";
import { inject } from "vue";

let cristal: CristalApp | undefined;

export default {
  setup() {
    let configList: Array<WikiConfig> = [];
    let currentConfig = "";
    cristal = inject<CristalApp>("cristal");
    if (cristal != null) {
      let configs = cristal.getAvailableConfigurations();
      configs.forEach((wikiConfig: WikiConfig) => {
        configList.push(wikiConfig);
      });
      currentConfig = cristal.getWikiConfig().name;
    }
    return {
      configList: configList,
      cristal: cristal,
      currentConfig: currentConfig,
    };
  },
};
</script>
<template>
  <div>
    <div class="grid-container">
      <div v-for="wikiConfig in configList" :key="wikiConfig.name">
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
  gap: var(--cr-spacing-medium);
  align-items: center;
  justify-content: center;
  grid-template-columns: 1fr 150px;
  border-bottom: 1px solid #ddd;
  padding: var(--cr-spacing-x-small) var(--cr-spacing-2x-small);
}
.grid-container > *:last-child {
  border-bottom: 0;
}
</style>
