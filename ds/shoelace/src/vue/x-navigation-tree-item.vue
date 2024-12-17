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
/**
 * Navigation Tree Item implemented using Shoelace's TreeItem component.
 * It exposes methods that can be used to control the state of nodes from the
 * root level to any leaf by recursion.
 */
import "@shoelace-style/shoelace/dist/components/tree-item/tree-item";
import { inject, ref, useTemplateRef } from "vue";
import type SlTreeItem from "@shoelace-style/shoelace/dist/components/tree-item/tree-item";
import type { CristalApp } from "@xwiki/cristal-api";
import type {
  NavigationTreeNode,
  NavigationTreeSource,
  NavigationTreeSourceProvider,
} from "@xwiki/cristal-navigation-tree-api";
import type { Ref } from "vue";

type OnClickAction = (node: NavigationTreeNode) => void;

const cristal: CristalApp = inject<CristalApp>("cristal")!;
const treeSource: NavigationTreeSource = cristal
  .getContainer()
  .get<NavigationTreeSourceProvider>("NavigationTreeSourceProvider")
  .get();

const nodes: Ref<Array<NavigationTreeNode>> = ref([]);
const current = useTemplateRef<SlTreeItem>("current");
// eslint-disable-next-line @typescript-eslint/no-explicit-any
const items = useTemplateRef<any[]>("items");

const props = defineProps<{
  node: NavigationTreeNode;
  clickAction?: OnClickAction;
}>();

const emit = defineEmits<{
  (e: "selectionChange", selection: SlTreeItem): void;
}>();

defineExpose({
  expandTree,
  onDocumentDelete,
  onDocumentUpdate,
});

function onClick(node: NavigationTreeNode) {
  emit("selectionChange", current.value!);
  props.clickAction!(node);
}

async function lazyLoadChildren() {
  // On initial load, Shoelace might not be notified that some lazy nodes are
  // already loaded.
  if (!current.value?.lazy) {
    return;
  }
  nodes.value.push(...(await treeSource.getChildNodes(props.node.id)));

  // If the node doesn't have any children, we still need to add one item
  // temporarily to disable the loading state.
  if (nodes.value.length == 0) {
    const treeItem = document.createElement("sl-tree-item");
    current.value!.append(treeItem);
    treeItem.remove();
  }

  current.value?.removeAttribute("lazy");
}

// TODO: reduce the number of statements in the following method and reactivate the disabled eslint rule.
// eslint-disable-next-line max-statements
async function expandTree(nodesToExpand: string[]) {
  if (nodesToExpand[0] == props.node.id) {
    if (nodesToExpand.length > 1) {
      if (current.value!.lazy) {
        await lazyLoadChildren();
      }
      current.value!.expanded = true;
      nodesToExpand.shift();
      if (items.value) {
        await Promise.all(
          items.value!.map(async (it) => it.expandTree(nodesToExpand)),
        );
      }
    } else {
      emit("selectionChange", current.value!);
      // If we have a custom click action, we want to use it on dynamic
      // selection.
      if (props.clickAction) {
        props.clickAction!(props.node);
      }
    }
  }
}

function onSelectionChange(selection: SlTreeItem) {
  emit("selectionChange", selection);
}

function onDocumentDelete(parents: string[]) {
  for (const i of nodes.value.keys()) {
    if (nodes.value[i].id == parents[0]) {
      if (parents.length == 1) {
        nodes.value.splice(i, 1);
      } else {
        parents.shift();
        items.value![i].onDocumentDelete(parents);
      }
      return;
    }
  }
}

// TODO: reduce the number of statements in the following method and reactivate the disabled eslint rule.
// eslint-disable-next-line max-statements
async function onDocumentUpdate(parents: string[]) {
  for (const i of nodes.value.keys()) {
    if (nodes.value[i].id == parents[0]) {
      if (parents.length == 1) {
        // Page update
        const newItems = await treeSource.getChildNodes(props.node.id);
        for (const newItem of newItems) {
          if (newItem.id == nodes.value[i].id) {
            nodes.value[i].label = newItem.label;
            return;
          }
        }
      } else {
        parents.shift();
        await items.value![i].onDocumentUpdate(parents);
        break;
      }
    }
  }

  // New page
  const newItems = await treeSource.getChildNodes(props.node.id);
  newItemsLoop: for (const newItem of newItems) {
    for (const i of nodes.value.keys()) {
      if (newItem.id == nodes.value[i].id) {
        continue newItemsLoop;
      }
    }
    nodes.value.push(newItem);
  }
}
</script>

<template>
  <sl-tree-item
    ref="current"
    :lazy="node.has_children"
    :data-id="node.id"
    @sl-lazy-load.once="lazyLoadChildren"
  >
    <a
      v-if="props.clickAction"
      :href="node.url"
      @click.prevent="onClick(node)"
      >{{ node.label }}</a
    >
    <a v-else :href="node.url">{{ node.label }}</a>
    <x-navigation-tree-item
      v-for="item in nodes"
      :key="item.id"
      ref="items"
      slot="children"
      :node="item"
      :click-action="clickAction"
      @selection-change="onSelectionChange"
    >
    </x-navigation-tree-item>
  </sl-tree-item>
</template>

<style scoped>
:deep(a) {
  text-decoration: none;
  color: var(--cr-base-text-color);
}
/* Disable hand cursor on items, since we disable the default click action. */
:deep(sl-tree-item)::part(base) {
  cursor: default;
}
</style>
