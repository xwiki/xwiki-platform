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
 * Navigation Tree implemented using Vuetify's VTreeView component.
 * In order to use the initial component as a proper Navigation Tree for
 * Cristal, a few changes were made:
 *   - The only node activated matches the current page, or the clicked link if
 *     the component has a custom clickAction. We want to use actual links so
 *     that the user can click them normally (to e.g., open them in a new tab).
 *     So the default behavior of activating a node by clicking anywhere on the
 *     item was disabled. Default hover effects, such as darkening or changing
 *     the cursor on items, were also disabled.
 */
import { inject, onBeforeMount, ref, watch } from "vue";
import { VTreeview } from "vuetify/labs/VTreeview";
import type { CristalApp, PageData } from "@xwiki/cristal-api";
import type { DocumentService } from "@xwiki/cristal-document-api";
import type {
  NavigationTreeNode,
  NavigationTreeSource,
  NavigationTreeSourceProvider,
} from "@xwiki/cristal-navigation-tree-api";
import type { Ref } from "vue";

type TreeItem = {
  id: string;
  title: string;
  href: string;
  children?: Array<TreeItem>;
  _location: string;
};

type OnClickAction = (node: NavigationTreeNode) => void;

const cristal: CristalApp = inject<CristalApp>("cristal")!;
const documentService: DocumentService = cristal
  .getContainer()
  .get<DocumentService>("DocumentService");
const treeSource: NavigationTreeSource = cristal
  .getContainer()
  .get<NavigationTreeSourceProvider>("NavigationTreeSourceProvider")
  .get();

const rootNodes: Ref<Array<TreeItem>> = ref([]);
const tree: Ref<VTreeview | undefined> = ref(undefined);

const activatedNodes: Ref<Array<string>> = ref(new Array<string>());
const expandedNodes: Ref<Array<string>> = ref(new Array<string>());

const props = defineProps<{
  clickAction?: OnClickAction;
  currentPage?: PageData;
}>();

onBeforeMount(async () => {
  for (const node of await treeSource.getChildNodes("")) {
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

  documentService.registerDocumentChangeListener("delete", onDocumentDelete);
  documentService.registerDocumentChangeListener("update", onDocumentUpdate);
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
  const newExpandedNodes = treeSource.getParentNodesId(props.currentPage);
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
  const childNodes = await treeSource.getChildNodes(treeItem.id);
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

function clearSelection() {
  // Clicking on a node would activate it and this can't be disabled easily.
  // With this listener, we ensure that only the first activated node stays
  // active.
  if (activatedNodes.value.length > 1) {
    activatedNodes.value.pop();
  }
}

async function onDocumentDelete(page: PageData) {
  const parents = treeSource.getParentNodesId(page);
  let currentItems: TreeItem[] | undefined = rootNodes.value;
  while (currentItems) {
    for (const i of currentItems.keys()) {
      if (currentItems[i].id == parents[0]) {
        if (parents.length == 1) {
          currentItems.splice(i, 1);
          return;
        } else {
          currentItems = currentItems[i].children;
          parents.shift();
          break;
        }
      }
    }
  }
}

async function onDocumentUpdate(page: PageData) {
  const parents = treeSource.getParentNodesId(page);
  let currentParent: string | undefined = undefined;
  let currentItems: TreeItem[] | undefined = rootNodes.value;

  while (currentItems) {
    for (const i of currentItems.keys()) {
      if (currentItems[i].id == parents[0]) {
        if (parents.length == 1) {
          // Page update
          const newItems = await treeSource.getChildNodes(
            currentParent ? currentParent : "",
          );
          for (const newItem of newItems) {
            if (newItem.id == currentItems[i].id) {
              currentItems[i].title = newItem.label;
              return;
            }
          }
        } else {
          currentParent = currentItems[i].id;
          currentItems = currentItems[i].children;
          parents.shift();
          break;
        }
      }
    }

    // New page
    const newItems = await treeSource.getChildNodes(
      currentParent ? currentParent : "",
    );
    newItemsLoop: for (const newItem of newItems) {
      for (const i of currentItems!.keys()) {
        if (newItem.id == currentItems![i].id) {
          continue newItemsLoop;
        }
      }
      currentItems!.push({
        id: newItem.id,
        title: newItem.label,
        href: newItem.url,
        children: newItem.has_children ? [] : undefined,
        _location: newItem.location,
      });
    }
  }
}
</script>

<template>
  <v-treeview
    ref="tree"
    v-model:opened="expandedNodes"
    density="compact"
    :activated="activatedNodes"
    :items="rootNodes"
    :load-children="lazyLoadChildren"
    activatable
    active-strategy="independent"
    item-value="id"
    open-strategy="multiple"
    @update:activated="clearSelection"
  >
    <template #title="{ item }: { item: any }">
      <a
        v-if="props.clickAction"
        :href="item.href"
        @click.prevent="
          activatedNodes = [item.id];
          clickAction!({
            id: item.id,
            label: item.title,
            location: item._location,
            url: item.href,
            has_children: item.children !== undefined,
          });
        "
        >{{ item.title }}</a
      >
      <a v-else :href="item.href">{{ item.title }}</a>
    </template>
  </v-treeview>
</template>

<style scoped>
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
