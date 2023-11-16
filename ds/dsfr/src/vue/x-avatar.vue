<template>
  <DsfrPicture :src="src" size="small"></DsfrPicture>
</template>
<script lang="ts">
import { DsfrPicture } from "@gouvminint/vue-dsfr";

export default {
  components: { DsfrPicture },
  props: { src: { type: String, required: true } },
};
</script>
