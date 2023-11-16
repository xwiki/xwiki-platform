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
    <x-btn class="pagemenu" onclick="window.location = '/PerfDSFR/#/X'"
      >DSFR</x-btn
    >
    <x-btn class="pagemenu" onclick="window.location = '/Perf/#/X'"
      >Vuetify</x-btn
    >
    <x-btn class="pagemenu" onclick="window.location = '/PerfSL/#/X'"
      >Shoelace</x-btn
    >
    &nbsp;
    <router-link to="/X">
      <x-btn class="pagemenu">X</x-btn>
    </router-link>
    <router-link to="/empty">
      <x-btn class="pagemenu">Empty</x-btn>
    </router-link>
    <router-link v-if="ds == 'dsfr'" to="/dsfr">
      <x-btn class="pagemenu">Direct DSFR</x-btn>
    </router-link>
    <router-link v-if="ds == 'vuetify'" to="/vuetify">
      <x-btn class="pagemenu">Direct Vuetify</x-btn>
    </router-link>
    <router-link v-if="ds == 'shoelace'" to="/sl">
      <x-btn class="pagemenu">Direct Shoelace</x-btn>
    </router-link>
  </div>
  <br />
  <br />
  Design sytem: {{ ds }}
  <div>
    <router-view> </router-view>
  </div>
</template>
<script lang="ts">
import { CristalApp } from "@cristal/api";
import { inject } from "vue";

export default {
  setup() {
    const cristal = inject<CristalApp>("cristal");
    console.log("Performance location hash is ", location.hash);
    let hash = location.hash.substring(2);
    if (hash == "") hash = "X";
    let name = "perf" + hash;
    console.log("Template name is ", name);
    return { name: name, ds: cristal?.getSkinManager().getDesignSystem() };
  },
  updated() {
    const cristal = inject<CristalApp>("cristal");
    console.log("Performance location hash is ", location.hash);
    let hash = location.hash.substring(2);
    if (hash == "") hash = "X";
    let name = "perf" + hash;
    console.log("Template name is ", name);
    return { name: name, ds: cristal?.getSkinManager().getDesignSystem() };
  },
};
</script>
