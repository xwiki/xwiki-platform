<!--
 * See the LICENSE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 *
 * This file is part of the Cristal Wiki software prototype
 * @copyright  Copyright (c) 2023 XWiki SAS
 * @license    http://opensource.org/licenses/AGPL-3.0 AGPL-3.0
 *
-->
<template>
  <div>
    <div>
      Choose Configuration:
      <ul>
        <li v-for="wikiConfig in configList" :key="wikiConfig.name">
          <span>{{ wikiConfig.name }} ({{ wikiConfig.baseURL }})</span>
          <span v-if="wikiConfig.offline"> Offline</span>
          <span v-if="wikiConfig.designSystem != ''">
            Design System: {{ wikiConfig.designSystem }}</span
          >
          -
          <span v-if="wikiConfig.name == currentConfig">
            (current configuration)
          </span>
          <span v-else @click="cristal?.switchConfig(wikiConfig.name)">
            (open)
          </span>
        </li>
      </ul>
    </div>
  </div>
</template>
<script lang="ts">
import { CristalApp, WikiConfig } from "@cristal/api";
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
