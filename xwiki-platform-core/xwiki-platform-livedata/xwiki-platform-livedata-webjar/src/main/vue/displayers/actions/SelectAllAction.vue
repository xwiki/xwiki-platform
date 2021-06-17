<template>
  <!-- TODO: the click event is not triggered in practice because the html is copy-pasted in a new place, outside of
  Vue. Consequently the event listener is not copied along... -->
  <a :title="title" role="button" class="btn btn-default btn-xs" rel="nofollow" @click="selectAndCopy">
    <XWikiIcon :icon-descriptor="iconDescriptor"/>
  </a>
</template>

<script>
import XWikiIcon from "../../../utilities/XWikiIcon";

export default {
  // name: "SelectAllAction",
  // components: {XWikiIcon},
  // inject: ["logic"],
  // props: ['target', 'title'],
  // data: () => ({
  //   iconDescriptor: {}
  // }),
  // mounted() {
  //   const iconName = 'copy';
  //   this.logic.getIconByName(iconName)
  //     .then(icon => this.iconDescriptor = icon)
  //     .catch(() => {
  //       // TODO: translate.
  //       const errorMessage = `Failed to retrieve icon 'copy'.`;
  //       new XWiki.widgets.Notification(errorMessage, 'error');
  //     })
  // },
  // methods: {
  //   selectAndCopy() {
  //     var selection = window.getSelection();
  //     var range = document.createRange();
  //     range.selectNodeContents(this.target);
  //     selection.removeAllRanges();
  //     selection.addRange(range);
  //     // Add to clipboard.
  //     document.execCommand('copy');
  //     // TODO: translate.
  //     new XWiki.widgets.Notification(`The text of the property has been copied into the clipboard.`, 'done');
  //   }
  // }
}
</script>

<style scoped>

</style>