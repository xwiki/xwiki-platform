<template>
  <!-- TODO: Add an alt property to the image, as a prop. -->
  <img :src="src" />
</template>
<script lang="ts">
export default {
  props: { src: { type: String, required: true } },
};
</script>
