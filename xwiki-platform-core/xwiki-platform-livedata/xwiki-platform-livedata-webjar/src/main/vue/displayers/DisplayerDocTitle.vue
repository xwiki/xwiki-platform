<template>
  <DisplayerLink v-bind="{...$props, ...$attrs, html}"/>
</template>

<script>
import DisplayerLink from "./DisplayerLink.vue";
import displayerMixin from "./displayerMixin.js";
import displayerLinkMixin from "./displayerLinkMixin.js";

export default {
  name: "displayer-docTitle",
  components: {DisplayerLink},
  mixins: [displayerMixin, displayerLinkMixin],
  computed: {
    html() {
        if (this.entry['doc.title_raw'] !== undefined) {
        this.logic.footnotes.put('1', 'livedata.footnotes.computedTitle');
        return `${this.htmlValue} <sup>1</sup>`;
      } else {
        return this.htmlValue;
      }
    }
  }
}
</script>

<style scoped>

</style>