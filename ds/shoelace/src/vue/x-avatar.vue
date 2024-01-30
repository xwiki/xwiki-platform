<template>
  <sl-avatar :image="image"></sl-avatar>
</template>
<script lang="ts">
import "@shoelace-style/shoelace/dist/components/avatar/avatar";

export default {
  props: { image: { type: String, required: true } },
};
</script>
