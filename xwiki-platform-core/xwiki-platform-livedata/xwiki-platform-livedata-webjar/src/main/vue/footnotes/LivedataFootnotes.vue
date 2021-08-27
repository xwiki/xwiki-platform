<template>
  <div class="footnotes">
    <div v-for="footnote in footnotes.list()"
         :key="`footnote-${footnote.prefix}-${footnote.translationKey}`"
         class="box infomessage footnote">
      (<small>{{ footnote.symbol }}</small>) {{ $t(footnote.translationKey) }}
    </div>
  </div>
</template>

<script>
export default {
  name: "LivedataFootnotes",
  inject: ["logic"],
  data() {
    return {
      footnotes: this.logic.footnotes
    }
  }
}
</script>

<style scoped>
.footnotes .footnote {
  padding: 0.1em 1em;
}
</style>