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
import { ref, watch } from "vue";
import { VTreeview } from "vuetify/components/VTreeview";
import type { DisplayableTreeNode, TreeProps } from "@xwiki/cristal-dsapi";
import type { Ref } from "vue";

defineProps<TreeProps>();
const opened = defineModel<string[]>("opened", { default: [] });
const activated = defineModel<string | undefined>("activated");

const activatedNodes: Ref<Array<string>> = ref([]);

watch(activated, resetActivated, { immediate: true });

function resetActivated() {
  // Clicking on a node would activate it and this can't be disabled easily.
  // With this listener, we ensure that only the node we want stays active.
  activatedNodes.value = activated.value ? [activated.value] : [];
}

function updateActivated(newActivatedNode: DisplayableTreeNode) {
  if (newActivatedNode.activatable) {
    activated.value = newActivatedNode.id;
  }
}
</script>

<template>
  <v-treeview
    density="compact"
    :activated="activatedNodes"
    :items="showRootNode ? [rootNode] : rootNode.children"
    activatable
    active-strategy="independent"
    item-value="id"
    v-model:opened="opened"
    @update:activated="resetActivated"
  >
    <template #title="{ item }: { item: DisplayableTreeNode }">
      <a :href="item.url" @click="updateActivated(item)">{{ item.label }}</a>
    </template>
  </v-treeview>
</template>

<style scoped>
/* Remove the background color from the root div element. */
.v-list {
  background: none;
}
:deep(a) {
  text-decoration: none;
  color: var(--cr-base-text-color);
}
/* Disable hover on items. */
:deep(.v-list-item__overlay) {
  --v-hover-opacity: 0;
}
/* Disable hand cursor on items, since we disable the default click action. */
:deep(.v-list-item--link) {
  cursor: default;
}
</style>
