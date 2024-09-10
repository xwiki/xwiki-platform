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
import "@shoelace-style/shoelace/dist/components/tree/tree";
import "@shoelace-style/shoelace/dist/components/tree-item/tree-item";
import type {
  NavigationTreeNode,
  NavigationTreeSource,
} from "@xwiki/cristal-navigation-tree-api";

const rootNodes: Ref<Array<NavigationTreeNode>> = ref([]);
const props = defineProps<{
  treeResolver: NavigationTreeSource;
}>();

onBeforeMount(async () => {
  rootNodes.value.push(...(await props.treeResolver.getChildNodes("")));
});

function lazyLoadChildren(id: string) {
  return async (event: Event) => {
    const lazyItem = event.target! as Element;
    const childNodes = await props.treeResolver.getChildNodes(id);
    for (const child of childNodes) {
      const treeItem = document.createElement("sl-tree-item");
      treeItem.innerHTML = `<a href="${child.url}">${child.label
        .replace(/&/g, "&amp;")
        .replace(/>/g, "&gt;")
        .replace(/</g, "&lt;")}</a>`;
      if (child.has_children) {
        treeItem.setAttribute("lazy", "true");
        treeItem.addEventListener("sl-lazy-load", lazyLoadChildren(child.id), {
          once: true,
        });
      }
      lazyItem.append(treeItem);
    }

    // If the node doesn't have any children, we still need to add one item
    // temporarily to disable the loading state.
    if (childNodes.length == 0) {
      const treeItem = document.createElement("sl-tree-item");
      lazyItem.append(treeItem);
      treeItem.remove();
    }

    // Disable lazy mode once the content has been loaded
    lazyItem.removeAttribute("lazy");
  };
}
</script>

<template>
  <sl-tree>
    <sl-tree-item
      v-for="item in rootNodes"
      :key="item.id"
      :lazy="item.has_children"
      @sl-lazy-load.once="lazyLoadChildren(item.id)($event)"
    >
      <a :href="item.url">{{ item.label }}</a>
    </sl-tree-item>
  </sl-tree>
</template>

<style scoped></style>
