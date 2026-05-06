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
  <div
    class="guidedtour-widget"
    :class="{
      collapsed: state.isWidgetCollapsed,
    }"
  >
    <DriverCss />
    <GuidedTourWidgetHeader
      @closeGuidedTourWidget="onCloseGuidedTourWidget"
      :progress="progress.val"
    />
    <div class="guidedtour-widget-content">
      <div class="guidedtour-container">
        <!-- FIXME: There should be a better grouping style here, groups shouldn't be sections. -->
        <template
          v-if="
            /* Waiting for 2 async fetches to finish (in any order). FIXME: `<` used for development, but it's some weird behavior if the async loads more times than expected. */
            state.waitingLoadAsync < 2
          "
        >
          <GuidedTourWidgetItem />
          <GuidedTourWidgetItem />
          <GuidedTourWidgetItem />
        </template>
        <template v-else-if="state.tours.length > 0">
          <GuidedTourWidgetTour
            v-for="tour in state.tours"
            :key="tour.id"
            :tour="ref(tour)"
            @toggleCollapseTour="
              (tour: TourTour) => {
                console.debug('toggleCollapseTour closeset for ', tour);
                tour.isCollapsed = !tour.isCollapsed;
              }
            "
          />
        </template>
        <div v-else>
          <template v-if="state.tours.length == 0">
            <GuidedTourWidgetItem />
          </template>
          <template v-else-if="state.toursLoadError.length > 0">{{
            state.toursLoadError
          }}</template>
          <template v-else>
            Something went terribly wrong. Check the console.</template
          >
        </div>
      </div>
      <div
        class="guidedtour-useful-links"
        v-bind:class="{ empty: state.usefulLinks.length == 0 }"
      >
        <template v-if="state.usefulLinks.length">
          <GuidedTourWidgetUsefulLink
            v-for="(link, index) in state.usefulLinks"
            :key="index"
            :link="link"
          />
        </template>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
//import type { I18n } from "vue-i18n";
// All the logic should live here (TODO: Maybe move most of it to a .ts file)
// FIXME: This should be injected from somewhere else, but I have no idea from where.
import DriverCss from "./DriverCss.vue";
import GuidedTourWidgetHeader from "./GuidedTourWidgetHeader.vue";
import GuidedTourWidgetItem from "./GuidedTourWidgetItem.vue";
import GuidedTourWidgetTour from "./GuidedTourWidgetTour.vue";
import GuidedTourWidgetUsefulLink from "./GuidedTourWidgetUsefulLink.vue";
import { TourTaskStatus } from "@xwiki/platform-guidedtour-api";
import { computed, onMounted, provide, reactive, ref } from "vue";
import type {
  GuidedTourManagerApi,
  TourTask,
  TourTour,
} from "@xwiki/platform-guidedtour-api";

console.info("In widget setup. 233123213");
const { guidedTourManager } = defineProps<{
  guidedTourManager: GuidedTourManagerApi;
}>();

provide<GuidedTourManagerApi>("GuidedTourManager", guidedTourManager!);

const state = reactive({
  guidedTourManager: guidedTourManager,
  isWidgetCollapsed: false,
  tours: [] as TourTour[],
  usefulLinks: [] as string[],
  isWidgetShown: true,
  toursLoadError: "",
  waitingLoadAsync: 0,
});
provide("GuidedTourWidgetState", state!);
function onCloseGuidedTourWidget(buttonClicked: boolean) {
  console.info("toggle colapse in parent", buttonClicked);
  if (state.isWidgetCollapsed && buttonClicked) {
    state.isWidgetShown = false;
  } else {
    state.isWidgetCollapsed = !state.isWidgetCollapsed;
  }
}
onMounted(() => {
  // TODO: Split these into two Async components, so they can load independently (The links can show up earlier than the tasks, etc...)
  state.waitingLoadAsync = 0;
  guidedTourManager
    .getTours()
    .then((tours) => {
      // In order for the progress to be reactive, we need to preserve the original tours array. Thus, the elements need to be pushed into the old array.
      state.tours.push(...tours);
      state.waitingLoadAsync++;
      return tours;
    })
    .catch((e) => {
      console.error(e);
      state.waitingLoadAsync++;
      state.toursLoadError = e;
      state.waitingLoadAsync++;
    });
  guidedTourManager
    .getUsefulLinks()
    .then((usefulLinks) => {
      state.usefulLinks = usefulLinks;
      state.waitingLoadAsync++;
      return usefulLinks;
    })
    .catch((e) => {
      console.error(e);
      state.waitingLoadAsync++;
    });
  guidedTourManager.initExistingTask();
  // TODO: This should come from the localStorage
  // state.isWidgetShown = await guidedTourManager.isWidgetShown();
});
// FIXME: The .val property is a workaround, so vue doesn't auto-unwrap the progress, thus making it non-reactive.
const progress = {
  val: computed(() => {
    console.debug("Computing progress...", state.tours);
    const allTasks: TourTask[] = state.tours.flatMap((t) => t.tasksList!);
    return (
      allTasks.filter(
        (task: TourTask) =>
          task.status != undefined && task.status != TourTaskStatus.TODO,
      ).length / allTasks.length
    );
  }),
};
</script>

<style>
.guidedtour-useful-links {
  max-height: 300px;
  .empty {
    max-height: 0px;
  }
}
.guidedtour-widget.dragging * {
  pointer-events: none;
}

.guidedtour-widget.collapsed .guidedtour-widget-content {
  width: 0;
  max-height: 0;
  max-width: 0;
}

.guidedtour-widget {
  z-index: 999;
  position: fixed;
  bottom: 0px;
  right: 0px;
  box-shadow: 0px 0px 12px 0px #00000033;
  background-color: white;
  width: fit-content;
  overflow: hidden;
  display: inline-block;
  border-start-start-radius: 8px;
  user-select: none;

  .guidedtour-widget-content {
    overflow: hidden;
    max-height: 500px; /* For nice animations, these can't be % values. */
    max-width: 500px;
  }

  .guidedtour-content {
    display: flex;
    max-height: 400px; /* Placeholder so the height animates nicely. */
    flex-direction: column;
    overflow: hidden;
  }

  .collapsed .guidedtour-content {
    max-height: 0px;
  }

  .guidedtour-container {
    overflow-x: scroll;
    padding: 0px 16px 14px 16px;
    max-height: 300px;
  }

  * {
    transition: max-height 0.45s cubic-bezier(0.25, 1, 0.25, 1);
    /*, width 0.45s ease-in-out 0s*/
  }
  transition:
    max-height 0.45s cubic-bezier(0.25, 1, 0.25, 1),
    max-width 0.45s ease-in-out;
}
</style>
