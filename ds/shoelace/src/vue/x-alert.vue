<template>
  <sl-alert open :title="title" :variant="variant" :description="description">
    {{ title }}
  </sl-alert>  
</template>
<script lang="ts">
import '@shoelace-style/shoelace/dist/components/alert/alert';

export default {
    props: ["title", "type", "description"],
    data(props) {
        let variant = props.type;
        switch (props.type) {
            case "" : 
              variant = "primary";
              break;
            case "success" :
              variant = "success";
              break;
            case "warning" :
              variant = "warning";
              break;
            case "error" :
              variant = "danger";
              break;
            case "info" :
              variant = "primary";
              break;
            default:
              variant = "primary";
              break;
        }
        return { variant };
    }
}
</script>
