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
import type { FormProps } from "@xwiki/platform-dsapi";

defineProps<FormProps>();
// The browser submits the form only once its fields are valid. The template has no comment, which would be a second
// root node in development mode, preventing the attributes (e.g. the id targeted by a submit button outside the form)
// from falling through to the form.
defineEmits(["formSubmit"]);
</script>

<template>
  <form @submit.prevent="$emit('formSubmit')">
    <slot />
  </form>
</template>
