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
import { useId } from "vue";
import type { TextFieldProps } from "@xwiki/platform-dsapi";

const { label, help } = defineProps<TextFieldProps>();

const model = defineModel<boolean>();

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
      <label :for="textId">{{ label }}</label>
      <span class="xHint" v-if="help">{{ help }}</span>
    </dt>
    <dd>
      <input :id="textId" type="text" v-model="model" v-bind="$attrs" />
    </dd>
  </dl>
</template>

<style scoped></style>
