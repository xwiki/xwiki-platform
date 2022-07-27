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
  The LivedataAdvancedPanels component contains all the Livedata
  advanced panels, so that they can be included at once.
  It provides advance panels for configuration (sort, filter, ...)
  whatever layout is being used.
  It is included inside the livedata root component.

  Advanced panels can be opened from the DropdownMenu component.
-->
<template>
  <div class="livedata-advanced-panels">

    <component v-for="panel in logic.panels" :is="panel.component" :key="panel.id" :panel="panel"/>

  </div>
</template>


<script>
import LivedataAdvancedPanelProperties from "./LivedataAdvancedPanelProperties.vue";
import LivedataAdvancedPanelSort from "./LivedataAdvancedPanelSort.vue";
import LivedataAdvancedPanelFilter from "./LivedataAdvancedPanelFilter.vue";
import LiveDataAdvancedPanelExtension from "./LiveDataAdvancedPanelExtension";

export default {

  name: "LivedataAdvancedPanels",

  components: {
    LivedataAdvancedPanelProperties,
    LivedataAdvancedPanelSort,
    LivedataAdvancedPanelFilter,
    LiveDataAdvancedPanelExtension,
  },

  inject: ["logic"],

};
</script>


<style>
.livedata-advanced-panels {
  display: flex;
  flex-direction: column;
}
</style>
