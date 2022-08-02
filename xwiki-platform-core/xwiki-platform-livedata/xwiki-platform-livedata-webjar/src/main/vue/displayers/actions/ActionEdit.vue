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
  <BaseAction
    title-translation-key="livedata.displayer.actions.edit"
    :icon-descriptor="{ name: 'edit' }"
    :handler="handler"
    :close-popover="closePopover"
  />
</template>

<script>
import BaseAction from "./BaseAction.vue";

export default {

  name: "ActionEdit",

  components: {
    BaseAction,
  },

  props: {
    displayer: {
      type: Object,
      required: true
    },
    closePopover: {
      required: true
    }
  },

  methods: {
    handler(event) {
      event.stopPropagation();
      this.displayer.setEdit();
    },
  },
}
</script>

<style>

</style>