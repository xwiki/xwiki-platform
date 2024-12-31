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
  <div class="parent">
    <div class="image">
      <img v-if="link.imageURL" :src="link.imageURL" alt="" />
    </div>
    <div class="text">
      <c-icon
        class="icon-type"
        :size="Size.Small"
        :name="link.type == LinkType.PAGE ? 'file-earmark' : 'paperclip'"
      />
      {{ link.title }}
    </div>
    <div class="breadcrumb">
      <XBreadcrumb
        :items="
          link.segments.map((segment) => {
            return { label: segment };
          })
        "
      ></XBreadcrumb>
    </div>
  </div>
</template>

<style scoped>
.parent {
  display: grid;
  grid-template-columns: repeat(2, auto);
  grid-template-rows: repeat(2, auto);
  grid-column-gap: 0;
  grid-row-gap: 0;
}

.image {
  grid-area: 1 / 1 / 3 / 2;
}
.text {
  grid-area: 1 / 2 / 2 / 3;
}
.breadcrumb {
  grid-area: 2 / 2 / 3 / 3;
}
img {
  float: left;
  max-width: 4em;
  max-height: 4em;
  margin-right: var(--cr-spacing-small);
}
</style>
