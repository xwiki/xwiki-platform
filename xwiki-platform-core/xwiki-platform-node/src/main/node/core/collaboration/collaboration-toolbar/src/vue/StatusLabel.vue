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
import LoadingSpinner from "./LoadingSpinner.vue";
import { computed } from "vue";
import type { ToolbarStatus } from "./types";

const props = defineProps<{
  /**
   * Undefined when the status is not known yet.
   */
  status?: ToolbarStatus;
  /**
   * The labels to display for each status, indexed by status. No label is displayed for a missing value.
   */
  labels: [string?, string?, string?];
}>();

// The status is exposed through a data attribute, which is used for styling and by the functional tests.
const value = computed(() =>
  props.status === undefined
    ? undefined
    : ["dirty", "cleaning", "clean"][props.status],
);
</script>

<template>
  <span class="realtime-status" :data-status="value">
    <LoadingSpinner v-if="status === 1" />
    {{ status === undefined ? "" : (labels[status] ?? "") }}
  </span>
</template>

<style>
.realtime-status {
  color: var(--text-muted);
  margin: 0 0.5em;
}
</style>
