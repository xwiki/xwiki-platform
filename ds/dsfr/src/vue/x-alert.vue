<template>
    <DsfrAlert :title="title" :type="type" :description="description">
      <slot />
    </DsfrAlert>
</template>
<script lang="ts">
import { DsfrAlert } from '@gouvminint/vue-dsfr';

export default {
    props: ["title", "type", "description"],
    data(props) {
        return {};
    },
    components: { DsfrAlert }
}
</script>
