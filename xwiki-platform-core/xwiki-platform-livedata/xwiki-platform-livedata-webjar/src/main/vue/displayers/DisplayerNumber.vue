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
  DisplayerNumber is a custom displayer that displays the entry value as numbers.
  As this is very simple, it uses the BaseDisplayer default widgets
-->
<template>
  <!--
    Uses the BaseDisplayer as root element, as it handles for us
    all the displayer default behavior
  -->
  <BaseDisplayer
    class="displayer-number"
    :property-id="propertyId"
    :entry="entry"
    :is-view.sync="isView"
    @saveEdit="genericSave"
  >

    <!-- Keep the default Viewer widget -->
    <template #viewer></template>

    <!-- Keep the default Editor widget -->
    <template #editor></template>

  </BaseDisplayer>
</template>


<script>

import displayerMixin from "./displayerMixin.js";
import BaseDisplayer from "./BaseDisplayer.vue";
import displayerStatesMixin from "./displayerStatesMixin";

export default {

  name: "displayer-number",

  components: {
    BaseDisplayer,
  },

  // Add the displayerMixin to get access to all the displayers methods and computed properties inside this component
  mixins: [displayerMixin, displayerStatesMixin],

};
</script>


<style>

.livedata-displayer.displayer-number {
  text-align: right;
}

</style>
