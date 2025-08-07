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
  <div :class="{ parent: true, 'no-image': !link.imageURL }">
    <div v-if="link.imageURL" class="image">
      <img :src="link.imageURL" alt="" />
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

/* The layout is different if the suggestion does not have an image. */
.parent.no-image {
  grid-template-columns: 1fr;
  grid-template-rows: repeat(2, 1fr);
}

.no-image .text {
  grid-area: 1 / 1 / 2 / 2;
}
.no-image .breadcrumb {
  grid-area: 2 / 1 / 3 / 2;
}
</style>
