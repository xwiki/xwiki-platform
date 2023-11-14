<template>
  <span @click="isModalOpen = true" ref="modalOrigin">
    <slot name="activator"  />
  </span>
  <DsfrModal
    v-if="isModalOpen"
    :opened="isModalOpen"
    ref="modal"
    :origin="$refs.modalOrigin"
    @close="isModalOpen = false"
  >
    <slot name="default" /> 
  </DsfrModal>  
</template>
<script lang="ts">

export default {
  props: [ "class" ],
  data(props) {
    return { isModalOpen : false }
  }
}
</script>
