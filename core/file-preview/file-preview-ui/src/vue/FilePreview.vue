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
import NoPreview from "./NoPreview.vue";
import ImageFilePreview from "./preview/ImageFilePreview.vue";
import VideoFilePreview from "./preview/VideoFilePreview.vue";
import { Attachment } from "@xwiki/cristal-attachments-api";
import type { Component } from "vue";

defineProps<{ attachment: Attachment }>();
type AttachmentsMap = {
  [key: string]: { component: Component; regex: RegExp };
};
// TODO: to be moved to a component base approach.
const attachmentsMap: AttachmentsMap = {
  image: { component: ImageFilePreview, regex: /image\/.*/ },
  // TODO: pdf preview is currently disabled as it does not work on Chrome and Electron.
  // application: { component: ApplicationFilePreview, regex: /application\/pdf/ },
  video: { component: VideoFilePreview, regex: /video\/.*/ },
};

function resolve(mimetype: string): Component {
  for (const key in attachmentsMap) {
    const { component, regex } = attachmentsMap[key];
    if (mimetype.match(regex)) {
      return component;
    }
  }
  return NoPreview;
}
</script>

<template>
  <component
    :is="resolve(attachment.mimetype)"
    v-bind="{ attachment }"
  ></component>
</template>

<style scoped></style>
