<template>
  <DsfrButton size="large">
    <slot /> 
  </DsfrButton>
</template>
<script lang="ts">
export default {
  props: [ "class" ],
  data(props) {
    return {}
  }
}
</script>
