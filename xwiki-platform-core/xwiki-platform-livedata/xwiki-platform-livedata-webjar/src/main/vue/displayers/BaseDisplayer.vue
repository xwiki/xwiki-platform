<!--
  See the NOTICE file distributed with this work for additional
  information regarding copyright ownership.
  This is free software; you can redistribute it and/or modify it
  under the terms of the GNU Lesser General Public License as
  published by the Free Software Foundation; either version 2.1 of
  the License, or (at your option) any later version.
  This software is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
  Lesser General Public License for more details.
  You should have received a copy of the GNU Lesser General Public
  License along with this software; if not, write to the Free
  Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
  02110-1301 USA, or see the FSF site: http://www.fsf.org.
-->

<template>
  <div
    :class="[
      'livedata-displayer',
      isView ? 'view' : 'edit',
    ]"
    tabindex="0"
    @dblclick="edit"
    @keypress.self.enter="edit"
  >

    <slot
      name="viewer"
      v-if="isView"
    >
      <div>{{ value }}</div>
    </slot>

    <slot
      name="editor"
      v-if="!isView"
    >
      <input
        class="default-input"
        type="text"
        size="1"
        v-autofocus
        :value="value"
        @focusout="applyEdit($event.target.value)"
        @keypress.enter="applyEdit($event.target.value)"
        @keydown.esc="cancelEdit"
      />
    </slot>

  </div>
</template>


<script>
import displayerMixin from "./displayerMixin.js";

export default {

  name: "BaseDisplayer",

  mixins: [displayerMixin],

  data () {
    return {
      isView: true,
    };
  },


  methods: {

    view () {
      if (this.isView) { return; }
      this.isView = true;
      this.$el.focus();
    },

    edit () {
      if (!this.isView) { return; }
      this.isView = false;
    },

  },

};

</script>


<style>

.livedata-displayer {
  display: inline-block;
  width: 100%;
  height: 100%;
  min-height: 1em;
  word-break: break-word;
}

.livedata-displayer > * {
  width: 100%;
  height: 100%;
}

.livedata-displayer:focus {
  outline: 2px lightblue solid;
  position: relative;
}

.livedata-displayer .default-input {
  width: 100%;
}

</style>
