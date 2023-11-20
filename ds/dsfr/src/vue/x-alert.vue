<template>
  <DsfrAlert :title="title" :type="type" :description="description">
    <slot />
  </DsfrAlert>
</template>
<script lang="ts">
import { DsfrAlert } from "@gouvminint/vue-dsfr";
import type { PropType } from "vue";
import type { DsfrAlertType } from "@gouvminint/vue-dsfr/types/components/DsfrAlert/DsfrAlert.vue";

export default {
  components: { DsfrAlert },
  props: {
    title: { type: String, required: true },
    type: { type: Object as PropType<DsfrAlertType>, required: true },
    description: { type: String, required: true },
  },
};
</script>
