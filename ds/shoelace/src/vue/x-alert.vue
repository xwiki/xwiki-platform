<template>
  <sl-alert open :title="title" :variant="variant">
    {{ description }}
  </sl-alert>
</template>
<script lang="ts">
import "@shoelace-style/shoelace/dist/components/alert/alert";
import type { PropType } from "vue";

type Types = "" | "success" | "warning" | "error" | "info";
export default {
  props: {
    title: { type: String, required: true },
    type: {
      type: String as PropType<Types>,
      required: true,
    },
    description: { type: String, required: true },
  },
  data(props: { type: Types }) {
    let variant: string;
    switch (props.type) {
      case "":
        variant = "primary";
        break;
      case "success":
        variant = "success";
        break;
      case "warning":
        variant = "warning";
        break;
      case "error":
        variant = "danger";
        break;
      case "info":
        variant = "primary";
        break;
      default:
        variant = "primary";
        break;
    }
    return { variant };
  },
};
</script>
