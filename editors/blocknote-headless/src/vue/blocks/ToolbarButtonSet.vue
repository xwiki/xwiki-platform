<script setup lang="ts">
import { CIcon } from "@xwiki/cristal-icons";

defineProps<{
  buttons: Array<{ icon: string; title: string; onClick: () => void }>;
}>();
</script>

<template>
  <div class="container">
    <button
      v-for="button in buttons"
      :key="button.title"
      @click="button.onClick()"
    >
      <c-icon class="icon" :name="button.icon" />
    </button>
  </div>
</template>

<style scoped>
.container {
  display: flex;
  flex-direction: row;
  background-color: white;
  border-radius: 5px;
}

button {
  display: block;
  border-radius: 4px;
  height: 30px;
  max-height: 30px;
  width: 30px;
}

button:hover {
  background-color: #efefef;
}

.icon {
  font-size: 1.1rem;
  color: #3f3f3f;
  height: 30px;
}
</style>
