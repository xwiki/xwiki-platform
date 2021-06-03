<template>
  <div>
    <div ref="body">
      <slot></slot>
    </div>
    <div v-show="false" ref="content">
      <div class="popover-btn btn-group">
        <slot name="popover"></slot>
      </div>
    </div>
  </div>
</template>

<script>

import $ from 'jquery';

export default {
  name: 'Popover',
  props: {
    popoverRoot: {
      required: true
    },
    html: {
      'default': true,
      type: Boolean
    },
    placement: {
      'default': 'bottom',
      type: String
    },
    trigger: {
      'default': 'focus',
      type: String
    },
    delay: {
      default() {
        return {show: 150, hide: 0};
      }
    }
  },
  mounted() {
    // Needs to be saved because popover sets his own this when calling content.
    // TODO: checks if this is really the best way to do it + check if nextTick is needed or not.
    var that = this;
    // this.$nextTick(() => {
    $(this.$refs.body).popover({
      'html': this.html,
      'content': () => that.$refs.content.innerHTML,
      'placement': this.placement,
      // 'trigger': this.trigger,
      'delay': this.delay
    })
    // })
  },
  beforeDestroy() {
    $(this.popoverRoot).popover('destroy')
  }
}
</script>

<style scoped>
.popover-btn.btn-group .btn {
  border: 0;
  background: inherit;
}
</style>