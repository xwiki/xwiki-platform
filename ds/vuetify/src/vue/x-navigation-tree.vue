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
import { Ref, onBeforeMount, ref, watch } from "vue";
import { VTreeview } from "vuetify/labs/VTreeview";
import { type PageData } from "@xwiki/cristal-api";
import type {
  NavigationTreeSource,
  NavigationTreeNode,
} from "@xwiki/cristal-navigation-tree-api";

type TreeItem = {
  id: string;
  title: string;
  href: string;
  children?: Array<TreeItem>;
  _location: string;
};

type OnClickAction = (node: NavigationTreeNode) => void;

const rootNodes: Ref<Array<TreeItem>> = ref([]);
const tree: Ref<VTreeview | undefined> = ref(undefined);

const activatedNodes: Ref<Array<string>> = ref(new Array<string>());
const expandedNodes: Ref<Array<string>> = ref(new Array<string>());

const props = defineProps<{
  treeSource: NavigationTreeSource;
  clickAction?: OnClickAction;
  currentPage?: PageData;
}>();

onBeforeMount(async () => {
  for (const node of await props.treeSource.getChildNodes("")) {
    rootNodes.value.push({
      id: node.id,
      title: node.label,
      href: node.url,
      children: node.has_children ? [] : undefined,
      _location: node.location,
    });
  }
  if (props.currentPage !== undefined) {
    await expandTree();
  }
});

watch(
  () => props.currentPage,
  async () => {
    if (props.currentPage) {
      await expandTree();
    }
  },
);

async function expandTree() {
  const newExpandedNodes = props.treeSource.getParentNodesId(props.currentPage);
  let i;
  let currentNodes = rootNodes.value;
  for (i = 0; i < newExpandedNodes.length - 1; i++) {
    if (currentNodes) {
      for (const node of currentNodes) {
        if (node.id == newExpandedNodes[i]) {
          if (node.children?.length == 0) {
            await lazyLoadChildren(node);
          }
          if (!expandedNodes.value.includes(node.id)) {
            expandedNodes.value.push(node.id);
          }
          currentNodes = node.children!;
        }
      }
    }
  }
  if (currentNodes) {
    for (const node of currentNodes) {
      if (node.id == newExpandedNodes[i]) {
        activatedNodes.value = [node.id];
        // If we have a custom click action, we want to use it on dynamic
        // selection.
        if (props.clickAction) {
          props.clickAction({
            id: node.id,
            label: node.title,
            location: node._location,
            url: node.href,
            has_children: node.children !== undefined,
          });
        }
      }
    }
  }
}

async function lazyLoadChildren(item: unknown) {
  const treeItem = item as TreeItem;
  const childNodes = await props.treeSource.getChildNodes(treeItem.id);
  for (const child of childNodes) {
    treeItem.children?.push({
      id: child.id,
      title: child.label,
      href: child.url,
      children: child.has_children ? [] : undefined,
      _location: child.location,
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
    ref="tree"
    v-model:activated="activatedNodes"
    v-model:opened="expandedNodes"
    :items="rootNodes"
    :load-children="lazyLoadChildren"
    activatable
    active-strategy="single-independent"
    item-value="id"
    open-strategy="multiple"
  >
    <template #title="{ item }: { item: any }">
      <a
        v-if="props.clickAction"
        :href="item.href"
        @click.prevent="
          clickAction!({
            id: item.id,
            label: item.title,
            location: item._location,
            url: item.href,
            has_children: item.children !== undefined,
          })
        "
        >{{ item.title }}</a
      >
      <a v-else :href="item.href">{{ item.title }}</a>
    </template>
  </v-treeview>
</template>

<style scoped></style>
