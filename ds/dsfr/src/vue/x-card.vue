<template>
  <DsfrCard :title="title" description="">
    <slot />
  </DsfrCard>
</template>
<script lang="ts">
import { DsfrCard } from '@gouvminint/vue-dsfr';

export default {
    props: ["title"],
    data(props) {
        return { };
    },
    components: { DsfrCard }
}
</script>
