<template>
  <sl-avatar :image=src>
  </sl-avatar>
</template>
<script lang="ts">
import '@shoelace-style/shoelace/dist/components/avatar/avatar';

export default {
  props: [ "src" ],
  data(props) {
    return {}
  }
}
</script>
