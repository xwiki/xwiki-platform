<template>
  <div>
    <div v-for="footnote in list"
         :key="`footnote-${footnote.prefix}-${footnote.translationKey}`"
         class="box infomessage">
      (<small>{{ footnote.prefix }}</small>) {{ $t(footnote.translationKey) }}
    </div>
  </div>
</template>

<script>
export default {
  name: "LivedataFootnotes",
  inject: ["logic"],

  computed: {
    list() {
      return this.logic.footnotes.list();
    }
  }
}
</script>

<style scoped>

</style>