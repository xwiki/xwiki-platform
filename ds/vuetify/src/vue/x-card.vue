<template>
    <v-card :title="title" variant="outlined">
      <slot />
    </v-card>
  </template>
  <script lang="ts">
  export default {
    props: [ "title" ],
    data(props) {
      return {}
    }
  }
  </script>
  