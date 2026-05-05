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

<!--Would be nice to have a generic XWiki progressbar component maybe? idk-->
<!--If we want a dependency for the progress bar, maybe use this: https://v3.primevue.org/progressbar/ ?-->
<template>
  <div class="completeness">
    <div
      class="completeness-progress"
      :style="{ width: width ? width + 'px' : progress * 100 + '%' }"
    />
  </div>
</template>

<script setup lang="ts">
import type { ComputedRef } from "vue";

const props = defineProps<{
  progress: ComputedRef<number>;
  width?: number;
}>();
const progress = props.progress; // reactive read-only ref
</script>

<style>
.guidedtour-widget .completeness {
  margin: 16px 0 16px 0;
  height: 9px;
  position: relative;
  width: 100%;
  background: #f2f2f2ff;
  border-start-start-radius: 18px;
  border-start-end-radius: 18px;
  border-end-start-radius: 18px;
  border-end-end-radius: 18px;
  overflow: hidden;
  flex-shrink: 0;
}

.guidedtour-widget.collapsed .completeness {
  margin: 0;
  height: 4px;
  border-start-start-radius: 0;
  border-start-end-radius: 0;
  border-end-start-radius: 0;
  border-end-end-radius: 0;
}

.guidedtour-widget.collapsed .completeness-progress {
  height: 4px;
}

.guidedtour-widget .completeness-progress {
  position: absolute;
  left: 0px;
  top: 0px;
  height: 9px;
  background: #7698ffff;
  transition: width 0.15s ease-out;
}
</style>
