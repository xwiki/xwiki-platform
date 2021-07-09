<template>
  <BaseAction
    title-translation-key="livedata.displayer.actions.selectAll"
    :icon-descriptor="{name: 'copy'}"
    :handler="selectAndCopy"
    :close-popover="closePopover"
  />
</template>

<script>
import BaseAction from "./BaseAction";

export default {
  name: "ActionSelectAll",

  components: {BaseAction},

  inject: ["logic"],
  props: {
    target: {
      required: true
    },
    closePopover: {
      required: true
    }
  },

  methods: {
    selectAndCopy() {
      var selection = window.getSelection();
      var range = document.createRange();
      range.selectNodeContents(this.target);
      selection.removeAllRanges();
      selection.addRange(range);
      // Add to clipboard.
      document.execCommand('copy');
      new XWiki.widgets.Notification(this.$t('livedata.displayer.actions.selectAll.success'), 'done');
    }
  }
}
</script>

<style scoped>

</style>