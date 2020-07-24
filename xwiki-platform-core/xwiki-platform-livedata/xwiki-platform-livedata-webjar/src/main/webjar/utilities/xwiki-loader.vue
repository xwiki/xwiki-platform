<template>

  <div
    class="xwiki-loader"
    v-if="show"
  ></div>

</template>


<script>
/*
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
*/
define([
  "Vue",
], function (
  Vue
) {

  Vue.component("xwiki-loader", {

    name: "xwiki-loader",

    template: template,

    props: {
      // delay before showing the loader (in ms)
      delay: {
        type: Number,
        default: 1000,
      },
    },

    data: function () {
      return {
        show: false,
      };
    },

    mounted: function () {
      var self = this;
      setTimeout(function () {
        self.show = true;
      }, this.delay);
    }

  });
});
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
  --c1: transparent;
  --c2: #ccc4;
  background: linear-gradient(135deg, var(--c1) 25%, var(--c2) 50%, var(--c1) 75%);
  background-repeat: repeat;
  background-size: 200% 100%;
}

</style>
