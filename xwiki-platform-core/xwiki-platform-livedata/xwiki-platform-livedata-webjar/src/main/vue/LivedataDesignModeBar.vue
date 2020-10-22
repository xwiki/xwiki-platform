<!--
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 -->


<!--
  LivedataDesignModeBar is a bar component that shows up when the user
  goes to design mode.
  It purpose is to notify the user of the design mode that is on,
  and to provide buttons to leave the mode, or apply the changes made
  globally.
-->
<template>
  <div
    class="livedata-design-mode-bar alert alert-info"
    v-show="logic.designMode.activated"
  >

    <!-- The design bar -->
    <span>
      <!-- utf-8 wrench icon -->
      &#128295; You are in
      <!-- Display a popover on hover explaining what is Design mode -->
      <!-- eslint-disable max-len -->
      <span
        class="design-mode-info-span"
        data-toggle="popover"
        data-placement="bottom"
        title="Design mode"
        data-content="In Design mode, you can modify the whole configuration of the Livedata. Changes are shared with other people in design mode. To apply changes to everyone, click the 'Apply Changes' button."
      >
        Design mode
      </span>
      <!-- eslint-enable max-len -->
    </span>
    <div class="buttons">
      <button
        class="btn btn-secondary"
        @click="confirmLeave"
      >Leave
      </button>
      <button
        class="btn btn-primary"
        @click="confirmSave"
      >Apply changes
      </button>
    </div>

  </div>
</template>


<script>
import $ from "jquery";
import { confirm } from "./utilities/XWikiDialogConfirm";

export default {

  name: "LivedataDesignModeBar",

  inject: ["logic"],

  computed: {
    config () { return this.logic.config; },
  },


  methods: {

    async confirmSave () {
      // Show confirmation message
      const success = await confirm({
        title: "Apply changes?",
        text: "The current Livedata configuration will be pushed to every Livedata instances, continue?",
        confirmText: "Apply changes",
      });
      // Continue only on success
      if (!success) { return; }
      // TODO: brodcast new config to everyone
      this.logic.temporaryConfig.save({ configName: "initial" });
      this.logic.temporaryConfig.save({ configName: "edit" });
      // Show snackbar to inform user
      new XWiki.widgets.Notification("Changes were applied globally", "done");
    },

    async confirmLeave () {
      // if there is only one user in the realtime session and changes are not saved
      if (/* (TODO) USER IS THE ONLY ONE IN THE REALTIME SESSION */ true
        && !this.logic.temporaryConfig.equals({ configName: "edit" })) {
        const response = await confirm({
          title: "Leave Design mode?",
          text: "Some changes made in design mode were not saved, and will be lost. Do you really want to leave design mode?",
          confirmText: "Leave anyway",
        });
        if (!response) { return; }
      // else if changes were not saved, tell user it will be reverted
      } else if (!this.logic.temporaryConfig.equals({ configName: "edit" })) {
        const response = await confirm({
          title: "Leave Design mode?",
          text: "The Livedata configuration will be reverted to the last saved state",
          confirmText: "Leave",
        });
        if (!response) { return; }
      }
      // Continue only on success
      this.logic.designMode.toggle({ on: false });
    },

  },


  mounted () {
    $(".design-mode-info-span").popover({
      trigger: "hover",
    });
  },

};
</script>


<style>

.livedata-design-mode-bar {
  display: flex;
  flex-direction: row;
  flex-wrap: wrap;
  justify-content: space-between;
  align-items: center;
  box-shadow: unset;
}

.livedata-design-mode-bar .design-mode-info-span {
  display: inline-block;
  font-style: italic;
  text-decoration: underline;
  text-decoration-style: dotted;
  cursor: default;
}

</style>
