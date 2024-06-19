<!--
See the LICENSE file distributed with this work for additional
information regarding copyright ownership.

This is free software; you can redistribute it and/or modify it
under the terms of the GNU Lesser General Public License as
published by the Free Software Foundation; either version 2.1 of
the License, or (at your option) any later version.

This software is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public
License along with this software; if not, write to the Free
Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
02110-1301 USA, or see the FSF site: http://www.fsf.org.
-->
<template>
  <span> Mounting time: {{ diff }} </span>
  <div v-for="index in 1000" :key="index">
    <v-alert type="error" :title="'This is an alert ' + index" />
  </div>
</template>
<script lang="ts">
import { ref } from "vue";

let time: number = 0;
let diff: number = 0;
const diffRef = ref<number>(diff);

export default {
  setup() {
    time = new Date().getTime();
    console.log("Performance measurement vuetify Setup");
    return { diff: diffRef };
  },
  mounted() {
    let time2 = new Date().getTime();
    diffRef.value = time2 - time;
    console.log("Performance measurement vuetify Mounted: ", diff);
  },
  updated() {
    let time2 = new Date().getTime();
    diffRef.value = time2 - time;
    console.log("Performance measurement vuetify Updated: ", diff);
  },
};
</script>
