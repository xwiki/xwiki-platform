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
  <div id="search">
    <x-text-field
      label="Search"
      prepend-inner-icon="mdi:mdi-magnify"
      variant="solo"
    />
  </div>
</template>
<script lang="ts">
import { ref } from "vue";

const displayname: string = "Jean-Pierre Dupont";
const progressValue = ref(10);

export default {
  setup() {
    let initials: string = "";
    let names: Array<string> = displayname.split(/[\s-]/);
    if (names.length > 0) {
      initials += names[0][0].toUpperCase();
      if (names.length > 1) {
        initials += names[1][0].toUpperCase();
      }
      if (names.length > 2) {
        initials += names[2][0].toUpperCase();
      }
    }
    return { initials: initials, displayname: displayname, progressValue: 0 };
  },
  onMounted() {
    const timer = setTimeout(() => (progressValue.value = 66), 500);
    return () => clearTimeout(timer);
  },
};
</script>
