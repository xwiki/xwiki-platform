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
 * Navigation Tree implemented using Shoelace's Tree component.
 * In order to use the initial component as a proper Navigation Tree for
 * Cristal, a few changes were made:
 *   - We manually create Tree Items to keep references on the hierarchy.
 *     This lets us access nodes to expand and/or select them to match the page
 *     currently opened.
 *   - The only node selected matches the current page, or the clicked link if
 *     the component has a custom clickAction. We want to use actual links so
 *     that the user can click them normally (to e.g., open them in a new tab).
 *     So the default behavior of selecting a node by clicking anywhere on the
 *     item was disabled. Default hover effects, such as changing the cursor
 *     on items, were also disabled.
 */
import XNavigationTreeItem from "./x-navigation-tree-item.vue";
import { inject, onBeforeMount, ref, useTemplateRef, watch } from "vue";
import "@shoelace-style/shoelace/dist/components/tree/tree";
import type SlTreeItem from "@shoelace-style/shoelace/dist/components/tree-item/tree-item";
import type { CristalApp, PageData } from "@xwiki/cristal-api";
import type { DocumentService } from "@xwiki/cristal-document-api";
import type {
  NavigationTreeNode,
  NavigationTreeSource,
  NavigationTreeSourceProvider,
} from "@xwiki/cristal-navigation-tree-api";
import type { Ref } from "vue";

type OnClickAction = (node: NavigationTreeNode) => void;

const cristal: CristalApp = inject<CristalApp>("cristal")!;
const documentService: DocumentService = cristal
  .getContainer()
  .get<DocumentService>("DocumentService");
const treeSource: NavigationTreeSource = cristal
  .getContainer()
  .get<NavigationTreeSourceProvider>("NavigationTreeSourceProvider")
  .get();

const rootNodes: Ref<Array<NavigationTreeNode>> = ref([]);
const items = useTemplateRef<Array<typeof XNavigationTreeItem>>("items");

var selectedTreeItem: SlTreeItem | undefined;

const props = defineProps<{
  clickAction?: OnClickAction;
  currentPage?: PageData;
}>();

onBeforeMount(async () => {
  rootNodes.value.push(...(await treeSource.getChildNodes("")));

  documentService.registerDocumentChangeListener("delete", onDocumentDelete);
  documentService.registerDocumentChangeListener("update", onDocumentUpdate);
});

watch(() => props.currentPage, expandTree);
watch(items, expandTree);

async function expandTree() {
  if (props.currentPage) {
    const nodesToExpand = treeSource.getParentNodesId(props.currentPage);
    await Promise.all(
      items.value!.map(async (it) => it.expandTree(nodesToExpand)),
    );
  }
}

function onSlSelectionChange(event: unknown) {
  // We don't want users to manually select a node, so we undo any change.
  (
    event as { detail: { selection: SlTreeItem[] } }
  ).detail.selection[0].selected = false;
  if (selectedTreeItem) {
    selectedTreeItem!.selected = true;
  }
}

function onSelectionChange(selection: SlTreeItem) {
  if (selectedTreeItem) {
    selectedTreeItem!.selected = false;
  }
  selection.selected = true;
  selectedTreeItem = selection;
}

async function onDocumentDelete(page: PageData) {
  const parents = treeSource.getParentNodesId(page);
  for (const i of rootNodes.value.keys()) {
    if (rootNodes.value[i].id == parents[0]) {
      if (parents.length == 1) {
        rootNodes.value.splice(i, 1);
      } else {
        parents.shift();
        items.value![i].onDocumentDelete(parents);
      }
      return;
    }
  }
}

async function onDocumentUpdate(page: PageData) {
  const parents = treeSource.getParentNodesId(page);

  for (const i of rootNodes.value.keys()) {
    if (rootNodes.value[i].id == parents[0]) {
      if (parents.length == 1) {
        // Page update
        const newItems = await treeSource.getChildNodes("");
        for (const newItem of newItems) {
          if (newItem.id == rootNodes.value[i].id) {
            rootNodes.value[i].label = newItem.label;
            return;
          }
        }
      } else {
        parents.shift();
        if (items.value && items.value.length > 0) {
          await items.value[i].onDocumentUpdate(parents);
        }
        break;
      }
    }
  }

  // New page
  const newItems = await treeSource.getChildNodes("");
  newItemsLoop: for (const newItem of newItems) {
    for (const i of rootNodes.value.keys()) {
      if (newItem.id == rootNodes.value[i].id) {
        continue newItemsLoop;
      }
    }
    rootNodes.value.push(newItem);
  }
}
</script>

<template>
  <sl-tree @sl-selection-change="onSlSelectionChange">
    <x-navigation-tree-item
      v-for="item in rootNodes"
      :key="item.id"
      ref="items"
      :node="item"
      :click-action="clickAction"
      @selection-change="onSelectionChange"
    >
    </x-navigation-tree-item>
  </sl-tree>
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
