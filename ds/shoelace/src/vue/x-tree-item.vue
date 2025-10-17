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
import "@shoelace-style/shoelace/dist/components/tree-item/tree-item";
import { useTemplateRef } from "vue";
import type SlTreeItem from "@shoelace-style/shoelace/dist/components/tree-item/tree-item.d.ts";
import type { DisplayableTreeNode } from "@xwiki/cristal-dsapi";

const current = useTemplateRef<SlTreeItem>("current");

const props = defineProps<{
  node: DisplayableTreeNode;
}>();
const opened = defineModel<string[]>("opened", { default: [] });
const activated = defineModel<string | undefined>("activated");

function updateActivated() {
  if (props.node.activatable) {
    activated.value = props.node.id;
  }
}

function onExpand() {
  if (!opened.value?.includes(props.node.id)) {
    opened.value = [props.node.id, ...opened.value];
  }
}

function onCollapse() {
  if (opened.value?.includes(props.node.id)) {
    opened.value = [
      ...opened.value.filter((nodeId) => nodeId !== props.node.id),
    ];
  }
}
</script>

<template>
  <sl-tree-item
    ref="current"
    :data-id="node.id"
    :selected="node.id === activated"
    :expanded="opened?.includes(node.id)"
    @sl-expand="onExpand"
    @sl-collapse="onCollapse"
  >
    <a class="undecorated" :href="node.url" @click="updateActivated">{{
      node.label
    }}</a>
    <!-- @vue-expect-error the slot attribute is shoelace specific and is not know by the typechecker.
    Disabling it for now as I did not find an elegant solution to declare this property. -->
    <!-- eslint-disable vue/no-deprecated-slot-attribute -->
    <x-tree-item
      slot="children"
      v-for="item in node.children"
      :key="item.id"
      :node="item"
      v-model:activated="activated"
      v-model:opened="opened"
    >
    </x-tree-item>
    <!-- eslint-enable vue/no-deprecated-slot-attribute -->
  </sl-tree-item>
</template>

<style scoped>
sl-tree-item > a.undecorated {
  text-decoration: none;
  color: var(--cr-base-text-color);
}
/* Disable hand cursor on items, since we disable the default click action. */
sl-tree-item::part(base) {
  cursor: default;
}
</style>
