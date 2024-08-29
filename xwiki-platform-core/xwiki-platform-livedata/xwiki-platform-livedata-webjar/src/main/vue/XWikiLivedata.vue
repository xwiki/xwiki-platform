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
    <LivedataAdvancedPanels/>

    <!-- Where the layouts are going to be displayed -->
    <LivedataLayout :layout-id="layoutId" v-if="translationsLoaded"/>

    <!-- Displays the footnotes once the layout is loaded. -->
    <LivedataFootnotes v-if="layoutLoaded" />

    <!-- Persistent configuration module (if supported by the config) -->
    <LivedataPersistentConfiguration v-if="data.id"/>
    
    <!-- Displays a loader until the component is fully mounted. -->
    <div v-if="!layoutLoaded" class="loading"></div>
    
  </div>
</template>


<script>
// eslint-disable-next-line camelcase
__webpack_public_path__ = window.liveDataBaseURL;

import Vue from "vue";
import VueTippy from "vue-tippy";
import Vue2TouchEvents from 'vue2-touch-events';
import 'tippy.js/themes/light-border.css';
import LivedataAdvancedPanels from "./panels/LivedataAdvancedPanels.vue";
import LivedataLayout from "./layouts/LivedataLayout.vue";
import LivedataPersistentConfiguration from "./LivedataPersistentConfiguration.vue";
import LivedataFootnotes from "./footnotes/LivedataFootnotes";

// Declare vue plugins here
// We can't declare vue plugins during initialization inside logic.js
// because we are using in logic.js external webjars dependencies from RequireJs,
// and not dependencies from nodejs package.json
Vue.use(VueTippy, {
  theme: 'light',
});

Vue.use(Vue2TouchEvents)



export default {

  name: "XWikiLivedata",

  components: {
    LivedataAdvancedPanels,
    LivedataLayout,
    LivedataPersistentConfiguration,
    LivedataFootnotes
  },

  props: {
    logic: Object
  },

  computed: {
    data () { return this.logic.data; },
    // The id of the layout to be displayed
    layoutId () { return this.logic.currentLayoutId; }
  },

  // The provide function allows to vue to provide the Logic object
  // to every children component of this one
  // This is handy to avoid passing the logic as a prop everywhere
  // For the children components to access it, they only need to use:
  // `inject: ["logic"]` in their component definition
  provide () {
    return {
      logic: this.logic
    };
  },
  
  data() {
    return {
      layoutLoaded: false,
      translationsLoaded: false
    }
  },
  
  mounted() {
    // Waits for the layout to be (lazily) loaded before hiding the loader.  
    this.logic.onEvent("layoutLoaded", () => {
      this.layoutLoaded = true;
    });
    this.logic.translationsLoaded().finally(() => {
      this.translationsLoaded = true;
    })
  }

};
</script>


<style>

</style>
