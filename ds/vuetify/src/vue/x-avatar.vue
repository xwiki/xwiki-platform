<template>
  <v-avatar :image="image" :size="size"></v-avatar>
</template>
<script lang="ts">
export default {
  props: {
    image: { type: String, required: true },
    size: { type: String, required: true },
  },
};
</script>
