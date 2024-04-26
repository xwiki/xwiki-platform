<script setup lang="ts">
import CTemplate from "./c-template.vue";
import logo from "../images/xwiki-logo-color.svg";
import { CIcon } from "@cristal/icons";
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
