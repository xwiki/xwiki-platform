<template>
  <DsfrPicture v-if="image" :src="image" class="avatar"></DsfrPicture>
  <DsfrPicture v-else size="small" class="avatar" src=""></DsfrPicture>
</template>
<script lang="ts">
import { DsfrPicture } from "@gouvminint/vue-dsfr";

export default {
  components: { DsfrPicture },
  props: { image: { type: String, required: true } },
};
</script>
<style scoped>
.avatar {
  display: inline-block;
  width: 40px;
  height: 40px;
}

/deep/ img {
  width: 40px;
  height: 40px;
  border-radius: 50%;
}
</style>
