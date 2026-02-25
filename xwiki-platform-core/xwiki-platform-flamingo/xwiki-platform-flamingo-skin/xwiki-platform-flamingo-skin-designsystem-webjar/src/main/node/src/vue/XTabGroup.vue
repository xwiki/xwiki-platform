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
import { onMounted, useTemplateRef } from "vue";
// Preemptively import XTab and XTabPanel to make sure they are available during onMounted. Otherwise, they can be
// rendered with a delay since DS components are loaded lazily, and that breaks the modal initialization.
import "./XTab.vue";
import "./XTabPanel.vue";

const tabs = useTemplateRef("tabs");

// eslint-disable-next-line no-undef
const jQuery: Promise<JQuery> = new Promise((resolve) => {
  // eslint-disable-next-line @typescript-eslint/no-require-imports,no-undef
  require(["jquery"], ($: JQuery) => resolve($));
});

onMounted(async () => {
  const $ = await jQuery;
  const tabsElement = $(tabs.value);
  if (tabsElement.find('[role="presentation"].active').length == 0) {
    // If not tab is active, show the first one
    const find = tabsElement.find("a:first");
    console.log("find", find);
    find.tab("show");
  }
});
</script>

<template>
  <div>
    <!-- Nav tabs -->
    <ul class="nav nav-tabs" role="tablist" ref="tabs">
      <slot name="tabs"></slot>
    </ul>

    <!-- Tab panes -->
    <div class="tab-content">
      <slot name="panels"></slot>
    </div>
  </div>
</template>

<style scoped></style>
