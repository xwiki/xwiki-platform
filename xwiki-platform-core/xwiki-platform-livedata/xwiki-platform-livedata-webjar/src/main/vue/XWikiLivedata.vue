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
      <LivedataLayout :layout-id="layoutId"/>

      <!-- Persitent configuration module (if supported by the config) -->
      <LivedataPersistentConfiguration v-if="data.id"/>

  </div>
</template>


<script>
// eslint-disable-next-line camelcase
__webpack_public_path__ = window.liveDataBaseURL;

import LivedataAdvancedPanels from "./panels/LivedataAdvancedPanels.vue";
import LivedataLayout from "./layouts/LivedataLayout.vue";
import LivedataPersistentConfiguration from "./LivedataPersistentConfiguration.vue";


export default {

  name: "XWikiLivedata",

  components: {
    LivedataAdvancedPanels,
    LivedataLayout,
    LivedataPersistentConfiguration,
  },

  props: {
    logic: Object,
    xClassPropertyHelper: Object,
    editBus: Object
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
      logic: this.logic,
      xClassPropertyHelper: this.xClassPropertyHelper,
      editBus: this.editBus
    };
  },

};
</script>


<style>

</style>
