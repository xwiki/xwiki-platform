<script lang="ts" setup>
import { defineProps } from "vue";
import {
  DsfrNavigation,
  DsfrNavigationMenu,
  DsfrNavigationItem,
} from "@gouvminint/vue-dsfr";

defineProps<{
  title: string;
}>();

function toggle() {
  document.getElementById("menuId")?.classList.toggle("fr-collapse");
  return;
}
</script>

<template>
  <DsfrNavigation>
    <DsfrNavigationItem>
      <DsfrNavigationMenu
        id="menuId"
        :title="title"
        expanded-id="expandedMenuId"
        @toggle-id="toggle"
      >
        <slot name="default" />
      </DsfrNavigationMenu>
    </DsfrNavigationItem>
  </DsfrNavigation>
</template>
