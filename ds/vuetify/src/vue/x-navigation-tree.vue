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
import { Ref, onBeforeMount, ref } from "vue";
import { VTreeview } from "vuetify/labs/VTreeview";

import type { NavigationTreeSource } from "@xwiki/cristal-navigation-tree-api";

type TreeItem = {
  id: string;
  title: string;
  href: string;
  children?: Array<TreeItem>;
};

const rootNodes: Ref<Array<TreeItem>> = ref([]);
const props = defineProps<{
  treeResolver: NavigationTreeSource;
}>();

onBeforeMount(async () => {
  for (const node of await props.treeResolver.getChildNodes("")) {
    rootNodes.value.push({
      id: node.id,
      title: node.label,
      href: node.url,
      children: node.has_children ? [] : undefined,
    });
  }
});

async function lazyLoadChildren(item: unknown) {
  const treeItem = item as TreeItem;
  const childNodes = await props.treeResolver.getChildNodes(treeItem.id);
  for (const child of childNodes) {
    treeItem.children?.push({
      id: child.id,
      title: child.label,
      href: child.url,
      children: child.has_children ? [] : undefined,
    });
  }
  // If the node doesn't have any children, we update it.
  if (childNodes.length == 0) {
    treeItem.children = undefined;
  }
}
</script>

<template>
  <v-treeview
    :items="rootNodes"
    :load-children="lazyLoadChildren"
    activatable
    active-strategy="independent"
    open-strategy="multiple"
  >
    <template #title="{ item }: { item: any }">
      <a :href="item.href">{{ item.title }}</a>
    </template>
  </v-treeview>
</template>

<style scoped></style>
