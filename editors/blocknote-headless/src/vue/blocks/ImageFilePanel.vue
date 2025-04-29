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
import ImageSearchSelector from "./ImageEditor.vue";
import { BlockOfType, EditorType } from "../../blocknote";
import { LinkEditionContext } from "../../components/linkEditionContext";

const { editor, currentBlock: image } = defineProps<{
  editor: EditorType;
  currentBlock: BlockOfType<"image">;
  linkEditionCtx: LinkEditionContext;
}>();

const emit = defineEmits<{
  update: [];
}>();

function update(url: string) {
  editor.updateBlock({ id: image.id }, { props: { url } });
  emit("update");
}
</script>

<template>
  <div>
    <ImageSearchSelector :link-edition-ctx @select="({ url }) => update(url)" />
  </div>
</template>

<style scoped>
div {
  padding: 5px;
  background-color: white;
}
</style>
