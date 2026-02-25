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
import { computed } from "vue";
import type { AvatarProps } from "@xwiki/platform-dsapi";
const { name, image, size } = defineProps<AvatarProps>();

const classes = computed(() => {
  const defaultClasses = ["avatar"];
  if (size) {
    return [...defaultClasses];
  } else {
    // Only add the avatar_50 class if no size is defined.
    return [...defaultClasses, "avatar_50"];
  }
});

const src = computed(() => {
  if (image) {
    return image;
  } else {
    // XWiki is untype and glovally defined
    // eslint-disable-next-line no-undef
    return `${XWiki.contextPath}/bin/skin/resources/icons/xwiki/noavatar.png`;
  }
});
</script>

<template>
  <img :class="classes" :alt="name" :title="name" :src :width="size" />
</template>

<style scoped>
img:not(.avatar_50) {
  /** Define a default border radius for when a size is defined (and therefore, the avatar_50 class is not applied)  */
  border-radius: var(--border-radius-base);
}
</style>
