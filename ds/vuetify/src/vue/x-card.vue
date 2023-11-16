<template>
  <v-card :title="title" variant="outlined">
    <slot />
  </v-card>
</template>
<script lang="ts">
export default {
  props: { title: { type: String, required: true } },
};
</script>
