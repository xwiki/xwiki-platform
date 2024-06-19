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
import CTemplate from "./c-template.vue";
import logo from "../images/xwiki-logo-color.svg";
import { CIcon } from "@xwiki/cristal-icons";
</script>

<template>
  <x-dialog width="auto" :logo="logo" title="Configuration">
    <template #activator="{ props }">
      <c-icon name="gear" v-bind="props"></c-icon>
    </template>
    <template #default>
      <CTemplate name="config" />
    </template>
  </x-dialog>
</template>

<style scoped></style>
