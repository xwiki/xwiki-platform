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
<!--
  The XWikiLivedata is the root component of the Livedata.
  It only needs the `logic` object as prop, and then is
  totally autonomous.
-->

<script setup lang="ts">
// @ts-expect-error not a typescript component
import LivedataPersistentConfiguration from "./LivedataPersistentConfiguration.vue";
// @ts-expect-error not a typescript component
import LivedataFootnotes from "./footnotes/LivedataFootnotes.vue";
// @ts-expect-error not a typescript component
import LivedataLayout from "./layouts/LivedataLayout.vue";
// @ts-expect-error not a typescript component
import LivedataAdvancedPanels from "./panels/LivedataAdvancedPanels.vue";
import { LiveDataLogic } from "./services/LiveDataLogic";
import {
  computed,
  inject,
  nextTick,
  onMounted,
  provide,
  ref,
  useTemplateRef,
} from "vue";
import type {
  LiveDataSource,
  Logic,
  TranslationQuery,
  Translations,
} from "@xwiki/platform-livedata-api";
import type { I18n } from "vue-i18n";

const element = useTemplateRef("rootElement");

const { liveDataSource, data, contentTrusted, resolveTranslations } =
  defineProps<{
    liveDataSource: LiveDataSource;
    data: string;
    contentTrusted: boolean;
    locale: string;
    i18n: I18n;
    resolveTranslations: (query: TranslationQuery) => Promise<Translations>;
  }>();

const logic = new LiveDataLogic(
  liveDataSource,
  data,
  contentTrusted,
  resolveTranslations,
);

// eslint is unable to resolved jquery's type, but TypeScript does.
// eslint-disable-next-line no-undef
const jQuery: JQueryStatic = inject("jQuery")!;

provide<Logic>("logic", logic);

const layoutLoaded = ref(false);
const translationsLoaded = ref(false);

const dataId = computed(() => logic.data?.id);
const layoutId = computed(() => logic.currentLayoutId?.value);

// eslint-disable-next-line max-statements
onMounted(async () => {
  const elementValue = element.value!;
  logic.setElement(elementValue);
  // Register the logic on the parent of the root element to make its API accessible publicly.
  jQuery(elementValue).parent().data("liveData", logic);
  // Waits for the layout to be (lazily) loaded before hiding the loader.
  logic.onEvent("layoutLoaded", () => {
    layoutLoaded.value = true;
  });

  try {
    await logic.translationsLoaded();
  } finally {
    translationsLoaded.value = true;
  }

  logic.registerPanel({
    id: "propertiesPanel",
    title: logic.t("livedata.panel.properties.title"),
    name: logic.t("livedata.dropdownMenu.panels.properties"),
    icon: "list-bullets",
    component: "LivedataAdvancedPanelProperties",
    order: 1000,
  });
  logic.registerPanel({
    id: "sortPanel",
    title: logic.t("livedata.panel.sort.title"),
    name: logic.t("livedata.dropdownMenu.panels.sort"),
    icon: "table_sort",
    component: "LivedataAdvancedPanelSort",
    order: 2000,
  });
  logic.registerPanel({
    id: "filterPanel",
    title: logic.t("livedata.panel.filter.title"),
    name: logic.t("livedata.dropdownMenu.panels.filter"),
    icon: "filter",
    component: "LivedataAdvancedPanelFilter",
    order: 3000,
  });

  // Fetch the data if we don't have any. This call must be made just after the main Vue
  // component is initialized as  LivedataPersistentConfiguration must be mounted for the
  // persisted filters to be loaded and applied when fetching  the entries. We use a dedicated
  // field (firstEntriesLoading) for the first load as the fetch start/end events can be
  // triggered  before the loader components is loaded (and in this case the loader is never
  // hidden even once the entries are displayed).
  if (!logic.data.data.entries.length) {
    try {
      await logic.updateEntries();
    } finally {
      // Mark the loader as finished, even if it fails as the loader should stop and a message be
      // displayed to the user in this case.
      logic.firstEntriesLoading.value = false;
    }
  } else {
    logic.firstEntriesLoading.value = false;
  }

  // Trigger the "instanceCreated" event on the next tick to ensure that the constructor has
  // returned, and thus all references to the logic instance have been initialized.
  nextTick(() => {
    logic.triggerEvent("instanceCreated", {});
  });
});
</script>

<template>
  <div class="xwiki-livedata" ref="rootElement">
    <!-- Import the Livedata advanced configuration panels -->
    <LivedataAdvancedPanels />

    <!-- Where the layouts are going to be displayed -->
    <LivedataLayout :layout-id="layoutId" v-if="translationsLoaded" />

    <!-- Displays the footnotes once the layout is loaded. -->
    <LivedataFootnotes v-if="layoutLoaded" />

    <!-- Persistent configuration module (if supported by the config) -->
    <LivedataPersistentConfiguration v-if="dataId" />

    <!-- Displays a loader until the component is fully mounted. -->
    <div v-if="!layoutLoaded" class="loading"></div>
  </div>
</template>
