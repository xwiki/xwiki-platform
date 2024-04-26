<template>
  <sl-button variant="default">
    <!-- TODO: We should try to use each the default icon slots to avoid weird alignment issues
      https://shoelace.style/components/button#slots
      -->
    <span class="align">
      <slot />
    </span>
  </sl-button>
</template>
<script lang="ts">
import "@shoelace-style/shoelace/dist/components/button/button";
export default {};
</script>
<style scoped>
.align {
  display: flex;
  flex-flow: row;
  flex-wrap: nowrap;
  gap: var(--cr-spacing-2x-small);
  align-items: center;
}
</style>
