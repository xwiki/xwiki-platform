<script lang="ts" setup>
import "@shoelace-style/shoelace";

import "@shoelace-style/shoelace/dist/themes/light.css";

import { defineProps } from "vue";
defineProps<{
  title: string;
}>();
</script>
<template>
  <sl-dropdown>
    <sl-button slot="trigger" caret> Edit </sl-button>
    <sl-menu>
      <slot name="default" />
    </sl-menu>
  </sl-dropdown>

  <sl-dropdown>
    <sl-button>
      <slot name="activator" />
    </sl-button>
    <sl-menu>
      <slot name="default" />
    </sl-menu>
  </sl-dropdown>
</template>
