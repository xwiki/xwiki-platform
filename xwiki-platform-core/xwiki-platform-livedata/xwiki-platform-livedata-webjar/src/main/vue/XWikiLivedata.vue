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

      <!-- The design mode info bar -->
      <LivedataDesignModeBar/>

      <!-- Import the Livedata advanced configuration panels -->
      <LivedataAdvancedPanels/>

      <!-- Where the layouts are going to be displayed -->
      <LivedataLayout :layout-id="layoutId"/>

      <!-- Persitent configuration module (if supported by the config) -->
      <LivedataPersistentConfiguration v-if="logic.config.id"/>

  </div>
</template>


<script>
import LivedataDesignModeBar from "./LivedataDesignModeBar.vue";
import LivedataAdvancedPanels from "./panels/LivedataAdvancedPanels.vue";
import LivedataLayout from "./layouts/LivedataLayout.vue";
import LivedataPersistentConfiguration from "./LivedataPersistentConfiguration.vue";


const XWikiLivedata = {

  name: "XWikiLivedata",

  components: {
    LivedataDesignModeBar,
    LivedataAdvancedPanels,
    LivedataLayout,
    LivedataPersistentConfiguration,
  },

  props: {
    logic: Object,
  },

  computed: {
    // The id of the layout to be displayed
    layoutId () { return this.logic.layout.currentId; },
  },

  // The provide function allows to vue to provide the Logic object
  // to every children component of this one
  // This is handy to avoid passing the logic as a prop everywhere
  // For the children components to access it, they only need to use:
  // `inject: ["logic"]` in their component definition
  provide () {
    return {
      logic: this.logic,
    };
  },

};

export default XWikiLivedata;
export { XWikiLivedata };
</script>


<style>

</style>
