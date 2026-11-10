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
import { usePopover } from "./bootstrap";
import { useEnvironment } from "./context";
import { useTemplateRef } from "vue";
import type { ToolbarWarning } from "./types";

defineProps<{
  warning: ToolbarWarning;
}>();

const environment = useEnvironment();

usePopover(useTemplateRef("toggle"));
</script>

<template>
  <button
    ref="toggle"
    type="button"
    class="btn btn-link realtime-warning"
    data-toggle="popover"
    data-placement="top"
    :data-warning="warning.id"
    :title="warning.title"
    :data-content="warning.message"
  >
    <span v-html="environment.icons.warning"></span>
    <span class="sr-only">{{ warning.title }}</span>
  </button>
</template>

<style>
.realtime-edit-toolbar .realtime-warning {
  padding: 6px;
  border: 0;
  margin: 0 0 0 0.5em;
}
</style>
