<template>
  <!-- TODO: add an alt attribute for accessibility -->
  <img :src="src" />
</template>
<script lang="ts">
export default {
  props: { src: { type: String, required: true } },
};
</script>
