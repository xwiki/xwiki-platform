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
  DisplayerHtml is a custom displayer that displays the entry value as html
  It also fetches its Edit widget from the server, returned as an html string.
  The Edit widget url is given inside the corresponding property descriptor
  of the displayer
-->
<template>
  <!--
    Uses the BaseDisplayer as root element, as it handles for us
    all the displayer default behavior
  -->
  <BaseDisplayer
    class="displayer-html"
    :property-id="propertyId"
    :entry="entry"
  >

    <!-- Provide the Html Viewer widget to the `viewer` slot -->
    <template #viewer>
        <div
            class="html-wrapper"
            v-html="value"
        ></div>
    </template>

    <!-- Provide the Html Editor widget to the `editor` slot -->
    <!-- TODO: implemtent the edit widget fetch from the server -->
    <template #editor></template>

  </BaseDisplayer>
</template>


<script>

import displayerMixin from "./displayerMixin.js";
import BaseDisplayer from "./BaseDisplayer.vue";

export default {

  name: "displayer-html",

  components: {
    BaseDisplayer,
  },

  // Add the displayerMixin to get access to all the displayers methods and computed properties inside this component
  mixins: [displayerMixin],

};
</script>


<style>

</style>
