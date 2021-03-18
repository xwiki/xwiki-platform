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
  DisplayerHtml is a custom displayer that displays the entry value as static html.
  It only support displaying the entries in view mode.
-->
<template>
  <!-- Uses the BaseDisplayer as root element, as it handles for us all the displayer default behavior. -->
  <BaseDisplayer
    class="displayer-html"
    :property-id="propertyId"
    :entry="entry"
    is-view
    view-only>
    <!-- Provide the Html Viewer widget to the `viewer` slot -->
    <template #viewer>
      <div class="html-wrapper" v-html="value"></div>
    </template>
  </BaseDisplayer>
</template>

<script>
import displayerMixin from "./displayerMixin.js";
import BaseDisplayer from "./BaseDisplayer.vue";

export default {
  name: "displayer-html",
  components: {BaseDisplayer,},
  mixins: [displayerMixin]
};
</script>

<style>
</style>
