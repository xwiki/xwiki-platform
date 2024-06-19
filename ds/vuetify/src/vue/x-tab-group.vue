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
<script setup lang="ts">
import { ref } from "vue";
const tab = ref(null);

const emits = defineEmits(["change"]);

function change(tabId: string) {
  emits("change", tabId);
}
</script>

<template>
  <v-card class="mx-auto">
    <v-tabs v-model="tab" @update:model-value="change">
      <slot name="tabs"></slot>
    </v-tabs>
    <v-card-text>
      <v-tabs-window v-model="tab">
        <slot name="panels"></slot>
      </v-tabs-window>
    </v-card-text>
  </v-card>
</template>

<style scoped></style>
