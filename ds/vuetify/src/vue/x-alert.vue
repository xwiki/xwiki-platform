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
import type { AlertProps } from "@xwiki/cristal-dsapi";

defineProps<AlertProps>();
</script>
<template>
  <v-alert :closable="closable" :title="title" :type="type" :text="description">
    <x-btn
      v-for="action of actions"
      :key="action.name"
      size="small"
      variant="text"
      @click="action.callback"
      >{{ action.name }}</x-btn
    >
    <br v-if="details" />
    <small v-if="details">{{ details }}</small>
    <slot />
  </v-alert>
</template>
