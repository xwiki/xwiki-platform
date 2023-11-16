<template>
  <DsfrCard :title="title" description="">
    <slot />
  </DsfrCard>
</template>
<script lang="ts">
import { DsfrCard } from "@gouvminint/vue-dsfr";

export default {
  components: { DsfrCard },
  props: { title: { type: String, required: true } },
};
</script>
