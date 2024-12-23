<script setup lang="ts">
import { CIcon, Size } from "@xwiki/cristal-icons";
import { LinkType } from "@xwiki/cristal-link-suggest-api";
defineProps<{
  link: {
    type: LinkType;
    title: string;
    segments: string[];
    imageURL?: string;
  };
}>();
</script>

<template>
  <c-icon
    class="icon-type"
    :size="Size.Small"
    :name="link.type == LinkType.PAGE ? 'file-earmark' : 'paperclip'"
  />
  <img v-if="link.imageURL" :src="link.imageURL" alt="" />
  {{ link.title }}
  <XBreadcrumb
    :items="
      link.segments.map((segment) => {
        return { label: segment };
      })
    "
  ></XBreadcrumb>
</template>

<style scoped>
img {
  float: left;
  max-width: 4em;
  max-height: 4em;
}
</style>
