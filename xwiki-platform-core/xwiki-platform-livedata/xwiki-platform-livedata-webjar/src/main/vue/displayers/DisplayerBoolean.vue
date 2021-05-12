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

<!--
  DisplayerBoolean is a custom displayer that displays the entry value as booleans.
-->
<template>
  <!-- Uses the BaseDisplayer as root element, as it handles for us all the displayer default behavior. -->
  <BaseDisplayer
      ref="baseDisplayer"
      class="displayer-boolean"
      :property-id="propertyId"
      :entry="entry"
      :is-view.sync="isView"
      @saveEdit="genericSave">

    <template #viewer>
      <div>{{ $t(value ? trueLabelKey : falseLabelKey) }}</div>
    </template>

    <template #editor>
      <input type="checkbox" v-model="currentBoolValue"/>
    </template>
  </BaseDisplayer>
</template>

<script>
import displayerMixin from "./displayerMixin.js";
import displayerStatesMixin from "./displayerStatesMixin.js"
import BaseDisplayer from "./BaseDisplayer.vue";

export default {
  name: "displayer-boolean",
  components: {BaseDisplayer,},

  // Add the displayerMixin to get access to all the displayers methods and computed properties inside this component.
  mixins: [displayerMixin, displayerStatesMixin],

  props: {
    trueLabelKey: {
      type: String,
      default: 'livedata.displayer.boolean.true'
    },
    falseLabelKey: {
      type: String,
      default: 'livedata.displayer.boolean.false'
    }
  },

  data() {
    return {
      editedValue: undefined
    }
  },

  computed: {
    // Uses `this.value` as the default value to initialize the input field.
    // Once the field value has been changed at least once, `this.editedValue` is used instead.
    // This way, a props is used as a default value and the component can still have a internal state. 
    currentBoolValue: {
      get() {
        return this.editedValue || this.value;
      }, set(value) {
        this.editedValue = value;
      }
    }
  },

  watch: {
    isView: function(newIsView) {
      if (newIsView) {
        // When we switch back to view mode, the edited value is reset.
        this.editedValue = undefined;
      }
    }
  }
}
</script>

<style scoped>
input {
  /* Set a fixed checkbox size to prevent it to be as large as the cell it is in. */
  width: 16px;
  height: 16px;
}

div.displayer-boolean {
  text-align: center;
}
</style>