<script lang="ts" setup></script>
<template>
  <div class="right-sidebar">
    <div style="height: 9999px">
      <p>
        If this element (.right-sidebar) is removed from the page, the layout
        should adapt automatically.
      </p>
      <p>
        The size here is exagerated to test the overflow mechanics (scrolling)
      </p>
    </div>
  </div>
</template>
<style scoped>
.right-sidebar {
  padding: var(--cr-spacing-x-small);
  overflow: auto;
  border-left: 1px solid var(--cr-color-neutral-200);
}
</style>
