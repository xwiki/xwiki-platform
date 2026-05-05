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
  <template v-if="tour">
    <section
      :id="props.tour.value.id"
      class="guidedtour-tour"
      :class="{
        ['tour-' + props.tour.value.status]: true,
        collapsed: props.tour.value.isCollapsed,
      }"
    >
      <GuidedTourWidgetItem
        :loading="false"
        :waiting="ref(false)"
        class="guidedtour-tour-header"
        @click="
          console.log('clicked', props.tour.value);
          $emit('toggleCollapseTour', props.tour.value);
        "
      >
        <template v-slot:pre-btns>
          <!-- This is just for show, it shouldn't do anything. -->
          <i class="fa-solid fa-chevron-right chevron always-show" />
        </template>
        <template v-slot:item-title>
          <span class="tour-title">{{ props.tour.value.title }}</span>
        </template>
        <template v-slot:post-btns>
          <button
            v-if="tour.value.status == TourTaskStatus.TODO"
            class="post-btn"
            @click.stop="onSkipTour"
          >
            <i class="fa-solid fa-x" />
          </button>
          <button v-else class="post-btn" @click.stop="onResetTour">
            <i class="fa fa-rotate-right" />
          </button>
        </template>
      </GuidedTourWidgetItem>
      <div class="guidedtour-content">
        <Suspense>
          <template #default>
            <GuidedTourWidgetTask
              v-for="task in state.tasks"
              :key="task.id"
              :task="task"
              :tour-id="props.tour.value.id"
              @taskStatusChanged="onTaskStatusChanged"
            />
          </template>
          <template #fallback>
            <!-- Have some placeholders loading -->
            <GuidedTourWidgetItem />
            <GuidedTourWidgetItem />
            <GuidedTourWidgetItem />
          </template>
        </Suspense>
      </div>
    </section>
  </template>
  <template v-else>
    <!-- Show nice loading animation for placeholders -->
    <section class="guidedtour-tour loading-content" />
  </template>
</template>

<script setup lang="ts">
import GuidedTourWidgetItem from "./GuidedTourWidgetItem.vue";
import GuidedTourWidgetTask from "./GuidedTourWidgetTask.vue";
import { TourTaskStatus } from "@xwiki/platform-guidedtour-api";
import { inject, onMounted, reactive, ref } from "vue";
import type {
  GuidedTourManagerApi,
  TourTask,
  TourTour,
} from "@xwiki/platform-guidedtour-api";
import type { Ref } from "vue";
const props = defineProps<{ tour: Ref<TourTour> }>();
function onTaskStatusChanged(task: TourTask) {
  console.warn("Caught event for", task);
}
defineEmits(["toggleCollapseTour"]);
const guidedTourManager: GuidedTourManagerApi = inject("GuidedTourManager")!;
const state = reactive({
  tasks: [] as TourTask[],
});

function onSkipTour() {
  for (let task of state.tasks) {
    guidedTourManager.setTaskStatus(task, TourTaskStatus.SKIPPED);
  }
}

function onResetTour() {
  for (let task of state.tasks) {
    guidedTourManager.setTaskStatus(task, TourTaskStatus.TODO);
  }
}

onMounted(async () => {
  const tasks = await guidedTourManager.getTasks(props.tour.value.id);
  state.tasks = tasks ?? ([] as TourTask[]);
  if (!tasks) {
    console.error("No tasks");
  }
});
</script>

<style>
:root {
  --guidedtour-text-color: #b0b0b0;
  --guidedtour-background-color-secondary: #f2f2f2;
}

.guidedtour-tour.loading-content {
  /* width: 100%;
  height: 8px;
  border-radius: 4px; */
  content: "";
  background: linear-gradient(
    90deg,
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
  animation: loading-shimmer 1.5s ease-in-out infinite;
}

.guidedtour-tour.tour-DONE .guidedtour-tour-header .tour-title {
  text-decoration: line-through;
  color: var(
    --guidedtour-background-color
  ); /* This is not WCAG-compliant, but idk how to do faded out text with good contrast. */
}

.guidedtour-tour.tour-SKIPPED .guidedtour-tour-header .tour-title {
  color: var(--guidedtour-text-color);
}

/* FIXME: guidedtour-content should be renamed to -collapsible or something. */
.guidedtour-widget .guidedtour-tour.value.collapsed .guidedtour-content {
  max-height: 0;
}

.guidedtour-tour {
  min-height: 2em;
  margin: 0.2em;
}

.guidedtour-tour .chevron {
  /* Disable strikethrough for the chevron icon. */
  text-decoration: none;
  cursor: pointer;
  display: inline-block;
  height: 16px;
  width: 16px;
  rotate: 0deg;
}

.guidedtour-tour:not(.collapsed) .chevron {
  rotate: 90deg;
}

.guidedtour-tour-header:hover {
  background: var(--guidedtour-background-color-secondary) 100%;
}

.guidedtour-tour-header {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  font-weight: bold;
  border-radius: 0.65em;
  transition: background-color 0.1s ease;
  padding: 0.5em;
  overflow: hidden;
  overflow-wrap: break-word;
}

.guidedtour-tour-header .chevron {
  width: 20px;
  /* fixed column */
  text-align: center;
  flex-shrink: 0;
  transition: transform 0.2s ease;
}
</style>
