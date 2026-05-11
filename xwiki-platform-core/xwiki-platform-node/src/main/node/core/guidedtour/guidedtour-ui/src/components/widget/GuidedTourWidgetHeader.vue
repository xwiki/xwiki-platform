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

<!--
  The GuidedTourWidget

  It contains a single default slot.
-->

<template>
  <div class="header">
    <div class="top-bar" @click="onCloseButtonClicked(false)">
      <span class="icon fa fa-compass" />
      <div class="title">Guided Tours</div>
      <div class="right-group">
        <button
          id="widget-options"
          class="btn"
          @click.stop="console.info('Options menu')"
        >
          <span class="fa fa-cog" />
        </button>
        <button
          id="widget-close"
          class="btn"
          @click.stop="onCloseButtonClicked(true)"
        >
          <i class="fa-solid fa-x" />
        </button>
      </div>
    </div>
    <GuidedTourWidgetProgressBar :progress="computed(() => progress)" />
  </div>
</template>

<script setup lang="ts">
import GuidedTourWidgetProgressBar from "./GuidedTourWidgetProgressBar.vue";
import { computed } from "vue";
import type { ComputedRef } from "vue";
const emit = defineEmits(["closeGuidedTourWidget"]);

function onCloseButtonClicked(buttonClicked: boolean) {
  console.info("Send widget close event...", buttonClicked);
  emit("closeGuidedTourWidget", buttonClicked);
}

const props = defineProps<{ progress: ComputedRef<number> }>();
const progress = props.progress; // reactive read-only ref
</script>

<style>
.top-bar {
  display: flex;
  cursor: pointer;
}

.right-group button {
  font-size: inherit;
  padding: 0.3em 0.5em 0.3em 0.5em;
}

.guidedtour-widget.collapsed .top-bar {
  padding: 8px;
}

.icon {
  align-self: flex-start;
  margin-right: 0.5em;
  overflow: hidden;
  width: 30px;
}

.guidedtour-widget.collapsed .icon {
  width: 0;
}

.right-group {
  align-self: flex-end;
  white-space: nowrap;
  margin-left: 0.5em;
}

.title {
  align-self: flex-start;
  width: 100%;
  font-weight: bold;
}

.guidedtour-widget.collapsed:not(.dragging) .header {
  cursor: pointer;
}

.guidedtour-widget.collapsed .right-group button#widget-options {
  display: none;
}

.guidedtour-widget.collapsed .header {
  padding: 0;
}

.guidedtour-widget .header {
  display: inline-block;
  user-select: none;
  padding: 14px 16px 14px 16px;
  width: 100%;
  overflow-wrap: break-word;
}
</style>
