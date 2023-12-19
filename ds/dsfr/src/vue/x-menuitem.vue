<script lang="ts">
import { defineProps } from "vue";
import {
  DsfrNavigationMenuItem,
  DsfrNavigationMenuLink,
} from "@gouvminint/vue-dsfr";

export default {
  components: { DsfrNavigationMenuItem, DsfrNavigationMenuLink },
  props: {
    title: { type: String, required: true },
    link: { type: String, required: true },
  },
};

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
