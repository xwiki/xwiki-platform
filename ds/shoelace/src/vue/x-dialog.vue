<template>
  <span @click="click">
    <slot name="activator" />
  </span>
  <sl-dialog :label="title" class="dialog-overview">
    <slot name="default" />
  </sl-dialog>
</template>
<script lang="ts">
import "@shoelace-style/shoelace/dist/components/dialog/dialog";

export default {
  props: {
    class: { type: String, required: true },
    title: { type: String, required: true },
  },
  setup() {
    console.log("in setup");

    function click() {
      console.log("opening modal");
      this.isModalOpen = true;
      const dialog = document.querySelector(
        ".dialog-overview",
      ) as HTMLDivElement;
      console.log("in update");
      if (this.isModalOpen) {
        // @ts-expect-error TODO might be an actual bug?
        dialog?.show();
      } else {
        // @ts-expect-error TODO might be an actual bug?
        dialog?.hide();
      }
    }

    return { isModalOpen: false, click: click };
  },
};
</script>
