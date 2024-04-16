<script lang="ts" setup>
import "@shoelace-style/shoelace";

import { defineProps } from "vue";
defineProps<{
  title: string;
}>();
</script>
<template>
  <sl-dropdown>
    <!-- The slot attribute is from shoelace, so not invalid. -->
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
