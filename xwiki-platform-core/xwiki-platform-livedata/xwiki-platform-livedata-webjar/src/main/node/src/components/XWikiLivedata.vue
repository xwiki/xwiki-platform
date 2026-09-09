<!--
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
-->
<!--
  The XWikiLivedata is the root component of the Livedata.
  It only needs the `logic` object as prop, and then is
  totally autonomous.
-->
<template>
  <div class="xwiki-livedata">

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

<script>

import LivedataAdvancedPanels from "./panels/LivedataAdvancedPanels.vue";
import LivedataLayout from "./layouts/LivedataLayout.vue";
import LivedataPersistentConfiguration from "./LivedataPersistentConfiguration.vue";
import LivedataFootnotes from "./footnotes/LivedataFootnotes.vue";

export default {

  name: "XWikiLivedata",

  inject: ["logic"],

  components: {
    LivedataAdvancedPanels,
    LivedataLayout,
    LivedataPersistentConfiguration,
    LivedataFootnotes,
  },

  computed: {
    dataId() {
      return this.logic.data.id;
    },
    // The id of the layout to be displayed
    layoutId() {
      return this.logic.currentLayoutId.value;
    },
  },

  data() {
    return {
      layoutLoaded: false,
      translationsLoaded: false,
    };
  },

  async mounted() {
    // Waits for the layout to be (lazily) loaded before hiding the loader. The promise is rejected when no layout
    // can be loaded at all, so that we never wait for one indefinitely.
    const layoutReady = new Promise((resolve, reject) => {
      this.logic.onEvent("layoutLoaded", event => {
        const { error } = event.detail;
        if (error) {
          reject(error);
        } else {
          // Hide the loader and show the footnotes only when a layout was actually loaded. When none could be
          // loaded the loader keeps running: there is nothing to display in its place yet, and it is also what
          // tells the functional tests that the live data is not usable.
          // TODO: XWIKI-24835: The Live Data displays an endless loading animation instead of an error when no
          // layout can be loaded
          this.layoutLoaded = true;
          resolve();
        }
      });
    });
    // We await this promise only at the end, after the translations and the entries, so we register the rejection
    // handler right away to keep the failure from being reported as unhandled meanwhile.
    layoutReady.catch(() => {});

    let error;
    try {
      try {
        await this.logic.translationsLoaded();
      } finally {
        this.translationsLoaded = true;
      }

      // The first entries are fetched by the Live Data logic, right after this component is mounted.
      await this.logic.firstEntriesLoaded;

      // The layout is loaded in parallel with the entries so we have to wait for it too, and for
      // the tick that renders it, before the live data can be considered fully displayed.
      await layoutReady;
      await this.$nextTick();
    } catch (e) {
      error = e;
    } finally {
      // Notify that the live data is fully loaded and displayed, or that it failed, because there are
      // listeners that can't wait indefinitely, such as the page ready detection used by the PDF
      // export. The "error" event data tells the two cases apart: it is undefined on success, and the
      // live data instance is always available as the "livedata" event data.
      this.logic.triggerEvent("instanceReady", { error });
    }
  },

};
</script>
