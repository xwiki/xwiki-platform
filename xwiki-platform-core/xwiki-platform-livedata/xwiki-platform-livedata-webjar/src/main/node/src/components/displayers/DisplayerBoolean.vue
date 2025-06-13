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
    v-model:is-view="isView"
    @saveEdit="genericSave">

    <template #viewer>
      <div>{{ $t(value ? trueLabelKey : falseLabelKey) }}</div>
    </template>

    <template #editor>
      <input type="checkbox" v-model="baseValue" v-autofocus />
    </template>
  </BaseDisplayer>
</template>

<script>
import displayerMixin from "./displayerMixin.js";
import displayerStatesMixin from "./displayerStatesMixin.js";
import BaseDisplayer from "./BaseDisplayer.vue";

export default {
  name: "displayer-boolean",
  components: { BaseDisplayer },

  // Add the displayerMixin to get access to all the displayers methods and computed properties inside this component.
  mixins: [displayerMixin, displayerStatesMixin],

  props: {
    trueLabelKey: {
      type: String,
      default: "livedata.displayer.boolean.true",
    },
    falseLabelKey: {
      type: String,
      default: "livedata.displayer.boolean.false",
    },
  },
};
</script>

<style scoped>
input {
  /* Set a fixed checkbox size to prevent it to be as large as the cell it is in. */
  width: 16px;
  height: 16px;
}

.displayer-boolean {
  text-align: center;
}
</style>
