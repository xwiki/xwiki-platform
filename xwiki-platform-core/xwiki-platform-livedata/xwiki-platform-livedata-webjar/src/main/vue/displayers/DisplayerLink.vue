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

<template>
  <BaseDisplayer
    :property-id="propertyId"
    :entry="entry"
  >

    <template #viewer>
        <a
          :href="href"
        >
          {{ value }}
          <span
            class="explicit-empty-value"
            v-if="!value"
          >(no value)</span>
        </a>
    </template>

    <template #editor></template>

  </BaseDisplayer>
</template>


<script>
import displayerMixin from "./displayerMixin.js";
import BaseDisplayer from "./BaseDisplayer.vue";

export default {

  name: "displayer-link",

  components: {
    BaseDisplayer,
  },

  mixins: [displayerMixin],

  computed: {
    href () {
      return this.config.propertyHref && this.entry[this.config.propertyHref] || "#";
    },
  },

};
</script>


<style>

.livedata-displayer .explicit-empty-value {
  font-style: italic;
  color: grey;
}

</style>
