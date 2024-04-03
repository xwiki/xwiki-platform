<script lang="ts" setup>
import { defineProps } from "vue";
import {
  DsfrNavigationMenuItem,
  DsfrNavigationMenuLink,
} from "@gouvminint/vue-dsfr";

defineProps<{
  title: string;
  link: string;
}>();
</script>

<template>
  <DsfrNavigationMenuItem>
    <DsfrNavigationMenuLink :to="link" :text="title" />
  </DsfrNavigationMenuItem>
</template>
