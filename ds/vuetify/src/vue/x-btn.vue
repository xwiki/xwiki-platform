<template>
    <v-btn>
      <slot /> 
    </v-btn>
  </template>
  <script lang="ts">
  export default {
    props: [ "class"],
    data(props) {
      return {}
    }
  }
  </script>
  