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
  <template
    v-if="
      props.loading ||
      props.waiting === undefined ||
      props.loading === undefined
    "
  >
    <div class="guidedtour-widget-item loading" />
  </template>
  <template v-else>
    <div class="guidedtour-widget-item" v-bind="$attrs">
      <span class="pre-btns">
        <slot name="pre-btns" />
      </span>
      <span class="guidedtour-widget-item-title"
        ><slot name="item-title"
      /></span>
      <slot name="loading-icon">
        <i
          class="fa-solid fa-circle-notch fa-spin"
          style="--fa-animation-timing: ease-in-out"
          v-show="props.waiting!.value"
        />
      </slot>
      <span class="post-btns">
        <slot name="post-btns" />
      </span>
    </div>
  </template>
</template>

<script setup lang="ts">
import type { Ref } from "vue";

defineOptions({
  inheritAttrs: false,
});

const props = defineProps<{
  loading?: boolean;
  waiting?: Ref<boolean>;
}>();

console.debug("Widget item:", props, props.waiting, props.loading);
</script>

<style>
.guidedtour-widget-item.loading {
  /* width: 100%;
  height: 8px;
  border-radius: 4px; */
  background: linear-gradient(
    to left,
    var(--guidedtour-text-color) 0%,
    var(--guidedtour-text-color) 25%,
    var(--guidedtour-background-color-secondary) 30%,
    var(--guidedtour-background-color-secondary) 35%,
    var(--guidedtour-text-color) 40%,
    var(--guidedtour-text-color) 75%,
    var(--guidedtour-background-color-secondary) 80%,
    var(--guidedtour-background-color-secondary) 85%,
    var(--guidedtour-text-color) 90%
  );
  background-size: 200% 100%;
  animation: loading-shimmer 4s linear infinite;
}

@keyframes loading-shimmer {
  from {
    background-position: 200% 0;
  }
  to {
    background-position: -200% 0;
  }
}

.guidedtour-widget-item {
  min-height: 2em;
  margin: 0.2em;
  display: flex;
  align-items: center;
  gap: 10px;
  border-radius: 0.65em;
  transition: background-color 0.1s ease;
  padding: 0.5em;
  cursor: pointer;
}

.guidedtour-widget-item:hover {
  background: var(--guidedtour-background-color-secondary) 100%;
}

.guidedtour-widget-item:hover .pre-btns *:not(.always-show),
.guidedtour-widget-item:hover .post-btns *:not(.always-show) {
  opacity: 1;
}

.pre-btns *:not(.always-show),
.post-btns *:not(.always-show) {
  text-decoration: none;
  color: var(--guidedtour-text-color);
  flex-shrink: 0;
  background: none;
  border: none;
  opacity: 0;
  transition: opacity 0.15s ease;
}

.pre-btns button,
.post-btns button {
  cursor: pointer;
  width: 20px;
  flex-shrink: 0;
  background: none;
  border: none;
}

.post-btns,
.post-btns:hover {
  margin-left: auto;
}
</style>
