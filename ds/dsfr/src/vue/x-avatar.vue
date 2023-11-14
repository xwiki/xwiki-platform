<template>
  <DsfrPicture :src=src size="small">
  </DsfrPicture>
</template>
<script lang="ts">
import { DsfrPicture } from '@gouvminint/vue-dsfr';

export default {
  props: [ "src" ],
  components : { DsfrPicture },
  data(props) {
    return {}
  }
}
</script>
