<!--
 * See the LICENSE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 *
 * This file is part of the Cristal Wiki software prototype
 * @copyright  Copyright (c) 2023 XWiki SAS
 * @license    http://opensource.org/licenses/AGPL-3.0 AGPL-3.0
 *
 **/
-->
<script setup lang="ts">
import { DsfrModal } from "@gouvminint/vue-dsfr";
import { ref } from "vue";
defineProps<{
  title: string;
  type: number;
  width: number;
}>();
const isModalOpen = ref(false);
</script>
<template>
  <span ref="modalOrigin" @click="isModalOpen = true">
    <slot name="activator" />
  </span>
  <!-- TODO: make the title configurable -->
  <DsfrModal
    v-if="isModalOpen"
    :title="title"
    :opened="isModalOpen"
    @close="isModalOpen = false"
  >
    <slot name="default" />
  </DsfrModal>
</template>
