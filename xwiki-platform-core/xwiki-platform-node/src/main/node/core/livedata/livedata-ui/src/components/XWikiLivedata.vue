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

  mounted() {
    // Waits for the layout to be (lazily) loaded before hiding the loader.
    this.logic.onEvent("layoutLoaded", () => {
      this.layoutLoaded = true;
    });
    this.logic.translationsLoaded().finally(() => {
      this.translationsLoaded = true;
    });
  },

};
</script>
