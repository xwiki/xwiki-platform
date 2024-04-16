<script lang="ts">
import "@shoelace-style/shoelace/dist/components/card/card";
export default {
  props: { title: { type: String, required: true } },
};
</script>
<template>
  <sl-card :title="title" variant="outlined">
    <slot />
  </sl-card>
</template>
