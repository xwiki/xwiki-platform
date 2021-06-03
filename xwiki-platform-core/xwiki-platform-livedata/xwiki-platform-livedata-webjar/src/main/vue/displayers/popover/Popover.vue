<template>
  <div>
    <div ref="body">
      <slot></slot>
    </div>
    <div v-show="false" ref="content">
      <div class="popover btn-group">
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
    // TODO: needs to be saved because popover sets his own this when calling content
    var that = this;
    this.$nextTick(() => {
      $(this.$refs.body).popover({
        'html': this.html,
        'content': () => that.$refs.content.innerHTML,
        'placement': this.placement,
        // 'trigger': this.trigger,
        'delay': this.delay
      })
    })
  },
  beforeDestroy() {
    $(this.popoverRoot).popover('destroy')
  }
}
</script>

<style scoped>
.popover.btn-group .btn {
  border: 0;
  background: inherit;
}
</style>