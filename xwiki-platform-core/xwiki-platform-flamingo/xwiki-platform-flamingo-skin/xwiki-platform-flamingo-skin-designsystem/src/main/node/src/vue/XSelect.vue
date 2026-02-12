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
<script lang="ts" setup>
import { useId } from "vue";
import type { SelectProps } from "@xwiki/platform-dsapi";

const { items, label, required, help } = defineProps<SelectProps>();

const selected = defineModel<string>();

const selectId = useId();

defineOptions({
  // See https://vuejs.org/api/options-misc.html#inheritattrs
  // Unknown attrs are by default added to the root element (i.e., the dl), but we want to disable that and instead add
  // them to the input field. That way it's possible for instance to add a name to the input field without having to
  // explicitly declare it on the props.
  inheritAttrs: false,
});
</script>
<template>
  <dl>
    <dt>
      <label :for="selectId">{{ label }}</label>
      <span class="xHint" v-if="help">{{ help }}</span>
    </dt>
    <dd>
      <select
        ref="inputElement"
        :id="selectId"
        v-bind="$attrs"
        v-model="selected"
        :required
      >
        <option v-for="item in items" :key="item" :value="item">
          {{ item }}
        </option>
      </select>
    </dd>
  </dl>
</template>
