<script setup lang="ts">
import { CIcon } from "@cristal/icons";
</script>
<template>
  <div class="help-button">
    <x-btn pill size="small">
      <c-icon name="question-circle"></c-icon>
      XWiki Help
    </x-btn>
  </div>
</template>

<style scoped>
.help-button {
  padding: 0 var(--cr-spacing-x-small) var(--cr-spacing-x-small);
}
</style>
