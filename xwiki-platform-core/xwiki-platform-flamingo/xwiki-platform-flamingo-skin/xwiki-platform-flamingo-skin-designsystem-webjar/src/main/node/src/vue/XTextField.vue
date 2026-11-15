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
<script setup lang="ts">
import { useRequiredMark } from "./requiredMark";
import { useId } from "vue";
import type { TextFieldProps } from "@xwiki/platform-dsapi";

// The props that are input attributes are not part of $attrs, since the text field API declares them: bind them to the
// input field explicitly.
const { label, help, name, required, autofocus, readonly, type } =
  defineProps<TextFieldProps>();

const model = defineModel<string>();

const requiredMark = useRequiredMark(() => required);

const textId = useId();

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
      <label :for="textId"
        >{{ label }}
        <span class="xRequired" v-if="required && requiredMark">{{
          requiredMark
        }}</span></label
      >
      <span class="xHint" v-if="help">{{ help }}</span>
    </dt>
    <dd>
      <!-- Optional default slot to display some content before the input field -->
      <slot></slot>
      <input
        :id="textId"
        :type="type ?? 'text'"
        :name
        :required
        :autofocus
        :readonly
        v-model="model"
        v-bind="$attrs"
      />
    </dd>
  </dl>
</template>
