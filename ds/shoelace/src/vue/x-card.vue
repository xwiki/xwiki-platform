<template>
  <sl-card :title="title" variant="outlined">
    <slot />
  </sl-card>
</template>
<script lang="ts">
export default {
  props: [ "title" ],
  data(props) {
    return {}
  }
}
</script>
