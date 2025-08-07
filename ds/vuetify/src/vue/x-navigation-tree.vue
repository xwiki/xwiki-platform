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
import { navigationTreePropsDefaults } from "@xwiki/cristal-dsapi";
import { SpaceReference } from "@xwiki/cristal-model-api";
import { inject, onBeforeMount, ref, watch } from "vue";
import { VTreeview } from "vuetify/components/VTreeview";
import type { CristalApp } from "@xwiki/cristal-api";
import type { DocumentService } from "@xwiki/cristal-document-api";
import type { NavigationTreeProps } from "@xwiki/cristal-dsapi";
import type { DocumentReference } from "@xwiki/cristal-model-api";
import type {
  NavigationTreeSource,
  NavigationTreeSourceProvider,
} from "@xwiki/cristal-navigation-tree-api";
import type { Ref } from "vue";

type TreeItem = {
  id: string;
  title: string;
  href: string;
  children?: Array<TreeItem>;
  _location: SpaceReference | DocumentReference;
  _is_terminal: boolean;
};

const cristal: CristalApp = inject<CristalApp>("cristal")!;
const documentService: DocumentService = cristal
  .getContainer()
  .get<DocumentService>("DocumentService");
const treeSource: NavigationTreeSource = cristal
  .getContainer()
  .get<NavigationTreeSourceProvider>("NavigationTreeSourceProvider")
  .get();

const rootNodes: Ref<Array<TreeItem>> = ref([]);

const activatedNodes: Ref<Array<string>> = ref(new Array<string>());
const expandedNodes: Ref<unknown> = ref([]);
var isExpanding: boolean = false;

const props = withDefaults(
  defineProps<NavigationTreeProps>(),
  navigationTreePropsDefaults,
);

onBeforeMount(async () => {
  const newRootNodes: Array<TreeItem> = [];
  for (const node of await getChildNodes("")) {
    newRootNodes.push({
      id: node.id,
      title: node.label,
      href: node.url,
      children: node.has_children ? [] : undefined,
      _location: node.location,
      _is_terminal: node.is_terminal,
    });
  }
  if (props.showRootNode) {
    rootNodes.value.push({
      id: "",
      title: "Root",
      href: ".",
      children: newRootNodes,
      _location: new SpaceReference(),
      _is_terminal: false,
    });
  } else {
    rootNodes.value.push(...newRootNodes);
  }
  await expandTree();

  documentService.registerDocumentChangeListener("delete", onDocumentDelete);
  documentService.registerDocumentChangeListener("update", onDocumentUpdate);
});

watch(() => props.currentPageReference, expandTree);

// TODO: reduce the number of statements in the following method and reactivate the disabled eslint rule.
// eslint-disable-next-line max-statements
async function expandTree() {
  if (props.currentPageReference && !isExpanding) {
    isExpanding = true;
    const newExpandedNodes = treeSource.getParentNodesId(
      props.currentPageReference!,
      props.includeTerminals,
      props.showRootNode,
    );
    let i;
    let currentNodes = rootNodes.value;
    for (i = 0; i < newExpandedNodes.length - 1; i++) {
      if (currentNodes) {
        for (const node of currentNodes) {
          if (node.id == newExpandedNodes[i]) {
            if (node.children?.length == 0) {
              await lazyLoadChildren(node);
            }
            if (!(expandedNodes.value as unknown[]).includes(node.id)) {
              (expandedNodes.value as unknown[]).push(node.id);
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
              is_terminal: node._is_terminal,
            });
          }
        }
      }
    }
    isExpanding = false;
  }
}

async function lazyLoadChildren(item: unknown) {
  const treeItem = item as TreeItem;
  const childNodes = await getChildNodes(treeItem.id);
  for (const child of childNodes) {
    treeItem.children?.push({
      id: child.id,
      title: child.label,
      href: child.url,
      children: child.has_children ? [] : undefined,
      _location: child.location,
      _is_terminal: child.is_terminal,
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

// TODO: reduce the number of statements in the following method and reactivate the disabled eslint rule.
// eslint-disable-next-line max-statements
async function onDocumentDelete(page: DocumentReference) {
  const parents = treeSource.getParentNodesId(
    page,
    props.includeTerminals,
    props.showRootNode,
  );
  let currentItem: TreeItem | undefined = undefined;
  let currentItemChildren: TreeItem[] | undefined = rootNodes.value;
  let notFound = false;

  currentItemsLoop: while (currentItemChildren && !notFound) {
    for (const i of currentItemChildren.keys()) {
      if (currentItemChildren[i].id == parents[0]) {
        if (parents.length == 1) {
          currentItemChildren.splice(i, 1);
          if (currentItem && currentItemChildren.length == 0) {
            currentItem!.children = undefined;
          }
          return;
        } else {
          currentItem = currentItemChildren[i];
          currentItemChildren = currentItem.children;
          parents.shift();
          continue currentItemsLoop;
        }
      }
    }
    notFound = true;
  }
}

// TODO: reduce the number of statements in the following method and reactivate the disabled eslint rule.
// eslint-disable-next-line max-statements
async function onDocumentUpdate(page: DocumentReference) {
  const parents = treeSource.getParentNodesId(
    page,
    props.includeTerminals,
    props.showRootNode,
  );
  let currentParent: string | undefined = undefined;
  let currentItems: TreeItem[] | undefined = rootNodes.value;
  let notFound = false;
  let isRoot = true;

  currentItemsLoop: while (currentItems && !notFound) {
    for (const i of currentItems.keys()) {
      if (currentItems[i].id == parents[0]) {
        if (parents.length == 1) {
          // Page update
          const newItems = await getChildNodes(
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
          if (!currentItems[i].children) {
            // This node had no children, we reset this so new ones can be
            // loaded lazily.
            currentItems[i].children = [];
          }
          currentItems = currentItems[i].children;
          parents.shift();
          isRoot = false;
          continue currentItemsLoop;
        }
      }
    }
    notFound = true;
  }

  // New page
  if (currentItems.length == 0 && !isRoot) {
    // We don't do anything because this node will be loaded lazily anyway.
    return;
  }
  const newItems = await getChildNodes(currentParent ? currentParent : "");
  const currentPageParents = props.currentPageReference
    ? treeSource.getParentNodesId(
        props.currentPageReference!,
        props.includeTerminals,
      )
    : [];
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
      _is_terminal: newItem.is_terminal,
    });
    if (
      currentPageParents.length > 0 &&
      currentPageParents[currentPageParents.length - 1] === newItem.id
    ) {
      // If we add a node, and it's the current page, it should be activated.
      activatedNodes.value = [newItem.id];
    }
  }
}

async function getChildNodes(id: string) {
  return (await treeSource.getChildNodes(id)).filter(
    (c) => props.includeTerminals || !c.is_terminal,
  );
}
</script>

<template>
  <v-treeview
    v-model:opened="expandedNodes"
    density="compact"
    :activated="activatedNodes"
    :items="rootNodes"
    :load-children="lazyLoadChildren"
    activatable
    active-strategy="independent"
    item-value="id"
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
            is_terminal: item._is_terminal,
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
