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
<!-- 
v-show is used here instead of v-if to prevent the loader to be re-rendered each time the show data field changes.
This change can happen two times in a short time span, once when the component is first rendered, then when the timout 
is switched to true (by default 200ms after the component is mounted). 
-->
  <div
    class="xwiki-loader"
    v-show="show"
  ></div>

</template>


<script>
export default {

  name: "XWikiLoader",

  props: {
    // delay before showing the loader (in ms)
    delay: {
      type: Number,
      default: 200,
    },
  },

  data () {
    return {
      show: false,
    };
  },

  mounted () {
    setTimeout(() => { this.show = true; }, this.delay);
  },

};
</script>


<style>

@keyframes waiting {
  from { background-position-x: 100%; }
  to { background-position-x: -100%; }
}

.xwiki-loader {
  width: 100%;
  height: 100%;
  min-height: 1rem;
  animation: waiting 2s linear infinite;
  background: linear-gradient(135deg, transparent 25%, @panel-default-heading-bg 50%, transparent 75%);
  background-repeat: repeat;
  background-size: 200% 100%;
}

</style>
