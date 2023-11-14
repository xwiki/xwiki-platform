<template>
  <v-alert :title="title" :type="type">
    <slot />
  </v-alert>
</template>
<script lang="ts">

export default {
    props: ["title", "type"],
    data(props) {
        return { };
    },
    components: { }
}
</script>
