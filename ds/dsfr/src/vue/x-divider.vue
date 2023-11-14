<template>
  <v-divider
    :thickness="2"
    class="border-opacity-100"
    color="info"
  />
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
