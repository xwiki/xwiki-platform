<!--
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
-->

<template>
  <span class="livedata-base-action btn"
        @click="trigger"
        @keypress.self.enter="trigger"
        :title="$t(titleTranslationKey)"
        tabindex="0"
  >
    <XWikiIcon :icon-descriptor="iconDescriptor"/>
    <template v-if="labelTranslationKey"> {{ $t(labelTranslationKey) }}</template>
  </span>
</template>

<script>
import XWikiIcon from "../../utilities/XWikiIcon";

export default {

  name: "BaseAction",

  components: {
    XWikiIcon,
  },

  props: {
    titleTranslationKey: {
      type: String,
      required: true
    },
    closePopover: {
      required: true
    },
    labelTranslationKey: {
      type: String,
      default: undefined
    },
    iconDescriptor: {
      type: Object,
      default: () => {},
    },
    handler: {
      type: Function,
      default: () => () => {},
    }
  },

  methods: {
    trigger(event) {
      this.handler(event);
      this.closePopover()
    }
  }

}
</script>

<style>
.livedata-base-action {
  cursor: pointer;
  padding: 0.2em;
}

</style>