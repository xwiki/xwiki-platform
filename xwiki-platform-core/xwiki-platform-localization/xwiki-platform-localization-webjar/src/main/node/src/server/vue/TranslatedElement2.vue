<!--
  See the NOTICE file distributed with this work for additional
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
import { useRemoteI18n } from "../../consumer-vue";
import { inject } from "vue";
import type { Resolver } from "../../api";

const resolver = inject<Resolver>("resolver");

const { t, isLoading } = useRemoteI18n(resolver!, ["key3", "key4"]);
</script>

<template>
  <ul v-if="!isLoading">
    <li>Translated value {{ t("key3") }}</li>
    <li>Translated value {{ t("key4") }}</li>
  </ul>
</template>

<style scoped></style>
