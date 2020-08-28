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
    v-show="logic.designMode"
  >

    <!-- The design bar -->
    <span>
      <!-- utf-8 wrench icon -->
      &#128295; You are in
      <!-- Display a popover on hover explaining what is Design mode -->
      <span
        class="design-mode-info-span"
        data-toggle="popover"
        data-placement="bottom"
        title="Design mode"
        data-content="In Design mode, you can modify the whole configuration of the Livedata. Changes are shared with other people in design mode. To apply changes to everyone, click the 'Apply Changes' button."
      >
        Design mode
      </span>
    </span>
    <div class="buttons">
      <button
        class="btn btn-secondary"
        @click="logic.toggleDesignMode(false)"
      >Leave
      </button>
      <button
        class="btn btn-primary"
        type="button"
        data-toggle="modal"
        :data-target="'#' + designModeApplyModalId"
      >Apply changes
      </button>
    </div>

    <!-- Apply changes confirmation modal -->
    <div
      class="modal fade"
      :id="designModeApplyModalId"
      tabindex="-1"
      role="dialog"
    >
      <div
        class="modal-dialog"
        role="document"
      >
        <div class="modal-content">
          <div class="modal-header">
            <button
              type="button"
              class="close"
              data-dismiss="modal"
              aria-label="Close"
            >
              <span aria-hidden="true">&times;</span>
            </button>
            <h4 class="modal-title">Modal title</h4>
          </div>
          <div class="modal-body">
            <p>Applying chages will blah blah blah ...</p>
          </div>
          <div class="modal-footer">
            <button
              type="button"
              class="btn btn-default"
              aria-label="Close"
              data-dismiss="modal"
            >Close</button>
            <button
              type="button"
              class="btn btn-primary"
              aria-label="Apply"
              data-dismiss="modal"
            >Aplly changes</button>
          </div>
        </div>
      </div>
    </div>


  </div>
</template>


<script>
import $ from "jquery";

export default {

  name: "LivedataDesignModeBar",

  inject: ["logic"],

  computed: {
    data () { return this.logic.data; },

    // return a dedicated id for this Livedata only
    // in case there are several Livedata on the page
    designModeApplyModalId () {
      return "design-mode-modal-confirm-apply-" + this.data.id;
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
