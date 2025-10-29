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
import XTreeItem from "./x-tree-item.vue";
import "@shoelace-style/shoelace/dist/components/tree/tree";
import type { TreeProps } from "@xwiki/cristal-dsapi";

defineProps<TreeProps>();
const opened = defineModel<string[]>("opened", { default: [] });
const activated = defineModel<string | undefined>("activated");
</script>

<template>
  <sl-tree>
    <x-tree-item
      v-for="item in showRootNode ? [rootNode] : rootNode.children"
      :key="item.id"
      :node="item"
      v-model:activated="activated"
      v-model:opened="opened"
    >
    </x-tree-item>
  </sl-tree>
</template>
