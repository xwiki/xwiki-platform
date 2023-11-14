<template>
    <span @click="click">
      <slot name="activator" />
    </span>
    <sl-dialog :label=title class="dialog-overview">
      <slot name="default" />
    </sl-dialog>
</template>
<script lang="ts">
import '@shoelace-style/shoelace/dist/components/dialog/dialog';


export default {
  props: [ "class", "title" ],
  setup(props) {
    console.log("in setup")

    function click() {
      console.log('opening modal');
      this.isModalOpen = true; 
      const dialog = document.querySelector('.dialog-overview') as HTMLDivElement;
      console.log("in update");
      if (this.isModalOpen)  {
        // @ts-ignore
        dialog?.show();
      } else {  
        // @ts-ignore   
        dialog?.hide(); 
      }
    }

    return { isModalOpen : false, click: click }
  }
}
</script>